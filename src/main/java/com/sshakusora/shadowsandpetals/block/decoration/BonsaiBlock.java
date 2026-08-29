package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Bonsai pot block. Players craft an empty pot, then right-click with a
 * sapling to plant it. The pot becomes a block entity that dynamically
 * renders the tree's trunk and leaves textures on a bonsai-shaped model.
 */
public final class BonsaiBlock extends BaseEntityBlock {
    public static final MapCodec<BonsaiBlock> CODEC = simpleCodec(BonsaiBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

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
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The model-space pot's long axis runs along Z (see the renderer's
        // MODEL_AUTHORING_ROTATION_DEGREES); the compensation rotation puts it
        // on X, so facings on the Z axis get the Z-long shape and vice versa.
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE : SHAPE_ROT90;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
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
                        if (block instanceof net.minecraft.world.level.block.SaplingBlock) {
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
                    bonsai.clear();
                } else {
                    bonsai.makeDead();
                }
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
            bonsai.plant(Blocks.OAK_LOG, Blocks.OAK_LEAVES, BuiltInRegistries.ITEM.getKey(Items.DEAD_BUSH), true);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS_SERVER;
        }

        // Sapling → plant with resolved trunk/leaves
        Item item = stack.getItem();
        Block block = Block.byItem(item);
        if (block instanceof net.minecraft.world.level.block.SaplingBlock) {
            BonsaiTreeResolver.Result resolved = BonsaiTreeResolver.resolve(block, level);
            if (resolved != null) {
                Identifier plantedItemId = BuiltInRegistries.ITEM.getKey(item);
                bonsai.plant(resolved.trunkBlock(), resolved.leavesBlock(), plantedItemId, false);
                if (!player.getAbilities().instabuild) {
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
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof BonsaiBlockEntity bonsai && bonsai.isPlanted()
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BonsaiBlockEntity bonsai)) {
            return InteractionResult.PASS;
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
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(BlockRegistry.BONSAI.get()));

        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof BonsaiBlockEntity bonsai && bonsai.isPlanted()) {
            Identifier plantedItemId = bonsai.getPlantedItemId();
            if (plantedItemId != null) {
                Item plantedItem = BuiltInRegistries.ITEM.getValue(plantedItemId);
                if (plantedItem != Items.AIR) {
                    drops.add(new ItemStack(plantedItem));
                }
            }
        }

        return drops;
    }
}