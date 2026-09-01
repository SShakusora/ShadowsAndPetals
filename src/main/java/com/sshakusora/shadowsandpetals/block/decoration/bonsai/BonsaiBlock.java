package com.sshakusora.shadowsandpetals.block.decoration.bonsai;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineProvider;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.outline.BonsaiOutlineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Bonsai pot block. Players craft an empty pot, then right-click with a
 * sapling to plant it. The pot becomes a block entity that dynamically
 * renders the tree's trunk and leaves textures on a bonsai-shaped model.
 */
public final class BonsaiBlock extends BaseEntityBlock implements BlockOutlineProvider {
    public static final MapCodec<BonsaiBlock> CODEC = simpleCodec(BonsaiBlock::new);

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 2.0, 13.0, 7.0, 14.0);
    private static final VoxelShape SHAPE_ROT90 = Block.box(2.0, 0.0, 3.0, 14.0, 7.0, 13.0);

    public BonsaiBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BonsaiBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation()));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }

    @Override
    public OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        return BonsaiOutlineGeometry.forRotation(state.getValue(ROTATION));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Net yaw = 90° - 22.5° * segment (renderer compensation included); the
        // pot's long axis lands on X for segments 0-3, Z for 4-7, alternating
        // every quarter turn. Bucket to the nearest 90° orientation.
        int segment = state.getValue(ROTATION);
        boolean longOnX = ((segment >> 2) & 1) == 0;
        return longOnX ? SHAPE_ROT90 : SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BonsaiBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            boolean wouldSucceed = false;
            if (stack.is(Items.SHEARS)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof BonsaiBlockEntity bonsai && bonsai.isPlanted()) {
                    wouldSucceed = true;
                }
            } else {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof BonsaiBlockEntity bonsai && !bonsai.isPlanted()) {
                    if (stack.is(Items.DEAD_BUSH)) {
                        wouldSucceed = true;
                    } else {
                        Block block = Block.byItem(stack.getItem());
                        if (block instanceof SaplingBlock) {
                            wouldSucceed = true;
                        }
                    }
                }
            }
            return wouldSucceed ? InteractionResult.SUCCESS
                    : (stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BonsaiBlockEntity bonsai)) {
            return InteractionResult.PASS;
        }

        // Shears on planted bonsai
        if (stack.is(Items.SHEARS)) {
            if (bonsai.isPlanted()) {
                if (bonsai.isDead()) {
                    ItemStack recovered = player.isCreative()
                            ? ItemStack.EMPTY
                            : bonsai.getPlantedItemStack();
                    bonsai.clear();
                    if (!recovered.isEmpty() && !player.getInventory().add(recovered)) {
                        Block.popResource(level, pos, recovered);
                    }
                } else {
                    bonsai.makeDead();
                }
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                level.playSound(null, pos, SoundEvents.SHEARS_SNIP, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS;
        }

        // Empty hand on planted bonsai → shape cycling via useWithoutItem
        if (bonsai.isPlanted()) {
            return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        }

        // Dead Bush → dead tree mode (oak log trunk, no leaves)
        if (stack.is(Items.DEAD_BUSH)) {
            bonsai.plant(Blocks.OAK_LOG, Blocks.OAK_LEAVES, stack, true);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS_SERVER;
        }

        // Sapling → plant with resolved trunk/leaves
        Item item = stack.getItem();
        Block block = Block.byItem(item);
        if (block instanceof SaplingBlock) {
            BonsaiTreeResolver.Result resolved = level instanceof ServerLevel serverLevel
                    ? BonsaiTreeResolver.resolve(serverLevel, pos, block)
                    : BonsaiTreeResolver.resolve(block);
            if (resolved != null) {
                bonsai.plant(resolved.trunkBlock(), resolved.leavesBlock(), stack, false);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        // Nothing matched; let an empty hand fall through to useWithoutItem (shape cycling)
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            // Match the server: sneak always rotates; otherwise shape cycling
            // only when planted. Client-side SUCCESS enables arm swing.
            if (player.isSecondaryUseActive()) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof BonsaiBlockEntity bonsai && bonsai.isPlanted()
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BonsaiBlockEntity bonsai)) {
            return InteractionResult.PASS;
        }

        if (player.isSecondaryUseActive()) {
            // Sneak right-click → rotate one 22.5° segment clockwise (wraps)
            int rotation = (state.getValue(ROTATION) + 1) & 15;
            level.setBlock(pos, state.setValue(ROTATION, rotation), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
            return InteractionResult.SUCCESS_SERVER;
        }

        if (bonsai.isPlanted()) {
            bonsai.cycleShape();
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.5F, 1.2F);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.PASS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));

        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof BonsaiBlockEntity bonsai && bonsai.isPlanted()) {
            ItemStack plantedItem = bonsai.getPlantedItemStack();
            if (!plantedItem.isEmpty()) {
                drops.add(plantedItem);
            }
        }

        return drops;
    }
}
