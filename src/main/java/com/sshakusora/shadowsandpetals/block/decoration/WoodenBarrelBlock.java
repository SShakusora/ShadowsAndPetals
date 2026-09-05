package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.WoodenBarrelBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A wooden fluid container whose contents live in its block entity rather than in block state.
 */
public class WoodenBarrelBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<WoodenBarrelBlock> CODEC = simpleCodec(WoodenBarrelBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape BASE_SHAPE = Shapes.or(
            // Floor of the 7x7 fluid cavity.
            Block.box(4.5D, 0.0D, 4.5D, 11.5D, 1.0D, 11.5D),
            // Four low barrel walls.
            Block.box(3.5D, 0.0D, 3.5D, 11.5D, 9.0D, 4.5D),
            Block.box(4.5D, 0.0D, 11.5D, 12.5D, 9.0D, 12.5D),
            Block.box(3.5D, 0.0D, 4.5D, 4.5D, 9.0D, 12.5D),
            Block.box(11.5D, 0.0D, 3.5D, 12.5D, 9.0D, 11.5D),
            // Two vertical rails and the top horizontal band.
            Block.box(3.0D, 1.0D, 7.0D, 4.0D, 16.0D, 9.0D),
            Block.box(12.0D, 1.0D, 7.0D, 13.0D, 16.0D, 9.0D),
            Block.box(2.5D, 13.0D, 7.5D, 13.5D, 15.0D, 8.5D)
    ).optimize();
    private static final Map<Direction.Axis, VoxelShape> SHAPES = createShapes();

    public WoodenBarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(AXIS, Direction.Axis.Z)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<WoodenBarrelBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        if (!axis.isHorizontal()) {
            axis = context.getHorizontalDirection().getAxis();
        }

        return defaultBlockState()
                .setValue(AXIS, axis)
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            Direction.Axis axis = state.getValue(AXIS);
            return state.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.WOODEN_BARREL.get().create(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof WoodenBarrelBlockEntity barrel) || !barrel.hasFluid()) {
            return drops;
        }

        CompoundTag barrelData = barrel.saveCustomOnly(params.getLevel().registryAccess());
        if (barrelData.isEmpty()) {
            return drops;
        }

        drops.forEach(stack -> {
            if (stack.is(this.asItem())) {
                stack.set(
                        DataComponents.BLOCK_ENTITY_DATA,
                        TypedEntityData.of(BlockEntityRegistry.WOODEN_BARREL.get(), barrelData.copy())
                );
            }
        });
        return drops;
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
        if (level.getBlockEntity(pos) instanceof WoodenBarrelBlockEntity barrel) {
            InteractionResult bottleResult = interactWithBottle(stack, level, pos, player, hand, barrel);
            if (bottleResult.consumesAction()) {
                return bottleResult;
            }
        }

        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection())) {
            return InteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    private static InteractionResult interactWithBottle(
            ItemStack stack,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            WoodenBarrelBlockEntity barrel
    ) {
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!barrel.canExtractWater(WoodenBarrelBlockEntity.BOTTLE_AMOUNT)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide()) {
                if (!barrel.extractWaterExactly(WoodenBarrelBlockEntity.BOTTLE_AMOUNT)) {
                    return InteractionResult.PASS;
                }
                player.setItemInHand(
                        hand,
                        ItemUtils.createFilledResult(
                                stack,
                                player,
                                PotionContents.createItemStack(Items.POTION, Potions.WATER)
                        )
                );
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }

        if (!stack.is(Items.POTION)) {
            return InteractionResult.PASS;
        }

        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion == null || !potion.is(Potions.WATER)) {
            return InteractionResult.PASS;
        }
        if (!barrel.canInsertWater(WoodenBarrelBlockEntity.BOTTLE_AMOUNT)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            if (!barrel.insertWaterExactly(WoodenBarrelBlockEntity.BOTTLE_AMOUNT)) {
                return InteractionResult.PASS;
            }
            player.setItemInHand(
                    hand,
                    ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE))
            );
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (precipitation != Biome.Precipitation.RAIN || level.getRandom().nextFloat() >= 0.05F) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof WoodenBarrelBlockEntity barrel) {
            barrel.fillFromRain();
        }
    }

    private static Map<Direction.Axis, VoxelShape> createShapes() {
        Map<Direction.Axis, VoxelShape> shapes = new EnumMap<>(Direction.Axis.class);
        Map<Direction, VoxelShape> rotated = VoxelShapeUtils.rotateHorizontal(BASE_SHAPE);
        shapes.put(Direction.Axis.X, rotated.get(Direction.EAST));
        shapes.put(Direction.Axis.Z, rotated.get(Direction.NORTH));
        return Map.copyOf(shapes);
    }
}
