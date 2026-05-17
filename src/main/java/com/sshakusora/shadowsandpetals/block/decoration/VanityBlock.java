package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.VanityBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class VanityBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<VanityBlock> CODEC = simpleCodec(VanityBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape LOWER_NORTH_SHAPE = Shapes.or(
            Block.box(1.0D, 2.25D, 2.5D, 3.0D, 13.0D, 13.5D),
            Block.box(13.0D, 2.25D, 2.5D, 15.0D, 13.0D, 13.5D),
            Block.box(3.5D, 7.75D, 2.0D, 12.5D, 8.75D, 14.0D),
            Block.box(3.5D, 8.75D, 3.0D, 4.5D, 13.75D, 13.75D),
            Block.box(11.5D, 8.75D, 3.0D, 12.5D, 13.75D, 13.75D),
            Block.box(4.5D, 8.75D, 12.5D, 11.5D, 13.75D, 13.5D),
            Block.box(3.5D, 4.75D, 13.0D, 12.5D, 7.75D, 14.0D),
            Block.box(1.5D, 13.0D, 2.0D, 14.5D, 15.0D, 14.0D),
            Block.box(0.0D, 14.0D, 1.0D, 16.0D, 15.0D, 15.0D),
            Block.box(0.25D, 15.0D, 10.75D, 15.75D, 16.0D, 14.75D)
    );
    private static final VoxelShape UPPER_NORTH_SHAPE = Shapes.or(
            Block.box(0.25D, 0.0D, 10.75D, 3.25D, 1.0D, 14.75D),
            Block.box(3.25D, 0.0D, 12.0D, 12.75D, 1.0D, 14.0D),
            Block.box(12.75D, 0.0D, 10.75D, 15.75D, 1.0D, 14.75D),
            Block.box(0.5D, 1.0D, 11.5D, 2.5D, 13.5D, 14.5D),
            Block.box(13.5D, 1.0D, 11.5D, 15.5D, 13.5D, 14.5D),
            Block.box(2.5D, 1.0D, 12.5D, 13.5D, 12.0D, 13.5D),
            Block.box(0.5D, 12.5D, 11.5D, 2.5D, 13.5D, 14.5D),
            Block.box(13.5D, 12.5D, 11.5D, 15.5D, 13.5D, 14.5D),
            Block.box(3.75D, 12.0D, 12.5D, 12.25D, 13.0D, 13.5D),
            Block.box(5.0D, 13.0D, 11.5D, 11.0D, 14.82513D, 14.5D)
    );
    private static final Map<Direction, VoxelShape> LOWER_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> UPPER_SHAPES = new EnumMap<>(Direction.class);

    static {
        LOWER_SHAPES.putAll(VoxelShapeUtils.rotateHorizontal(LOWER_NORTH_SHAPE));
        UPPER_SHAPES.putAll(VoxelShapeUtils.rotateHorizontal(UPPER_NORTH_SHAPE));
    }

    public VanityBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<VanityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @org.jetbrains.annotations.Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new VanityBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? createTickerHelper(type, BlockEntityRegistry.VANITY.get(), VanityBlockEntity::tick)
                : null;
    }

    @Override
    public @org.jetbrains.annotations.Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockPos abovePos = pos.above();
        Level level = context.getLevel();
        if (abovePos.getY() >= level.getMaxBuildHeight() || !level.getBlockState(abovePos).canBeReplaced(context)) {
            return null;
        }

        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(
                pos.above(),
                state.setValue(HALF, DoubleBlockHalf.UPPER)
                        .setValue(WATERLOGGED, level.getFluidState(pos.above()).getType() == Fluids.WATER),
                Block.UPDATE_ALL
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return openMenu(state, level, pos, player);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = openMenu(state, level, pos, player);
        return result.consumesAction()
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos basePos = getBasePos(state, pos);
        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (blockEntity instanceof VanityBlockEntity vanityBlockEntity) {
            player.openMenu(vanityBlockEntity);
            player.awardStat(Stats.OPEN_BARREL);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.FAIL;
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y) {
            Direction expectedDirection = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
            if (direction == expectedDirection) {
                if (neighborState.getBlock() instanceof VanityBlock && neighborState.getValue(HALF) != half) {
                    return state
                            .setValue(FACING, neighborState.getValue(FACING))
                            .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                }
                return state.getValue(WATERLOGGED)
                        ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                        : Blocks.AIR.defaultBlockState();
            }
        }

        return half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? state.getValue(WATERLOGGED)
                    ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                    : Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) != DoubleBlockHalf.UPPER || level.getBlockState(pos.below()).is(this);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && (player.isCreative())) {
            preventCreativeDropFromBottomPart(level, pos, state);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VanityBlockEntity vanityBlockEntity) {
                Containers.dropContents(level, pos, vanityBlockEntity);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? List.of() : super.getDrops(state, params);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, WATERLOGGED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? LOWER_SHAPES.get(state.getValue(FACING))
                : UPPER_SHAPES.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockPos basePos = getBasePos(state, pos);
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(basePos));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos basePos = getBasePos(state, pos);
        if (level.getBlockEntity(basePos) instanceof VanityBlockEntity vanityBlockEntity) {
            vanityBlockEntity.recheckOpen();
        }
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockPos basePos = getBasePos(state, pos);
        BlockEntity blockEntity = level.getBlockEntity(basePos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }

    private static void preventCreativeDropFromBottomPart(Level level, BlockPos pos, BlockState state) {
        DoubleBlockHalf half = state.getValue(HALF);
        BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(otherPos);
        if (!otherState.is(state.getBlock()) || otherState.getValue(HALF) == half) {
            return;
        }

        BlockState replacement = otherState.getValue(WATERLOGGED)
                ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                : Blocks.AIR.defaultBlockState();
        level.setBlock(otherPos, replacement, Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
        level.levelEvent(null, 2001, otherPos, Block.getId(otherState));
    }

    public static BlockPos getBasePos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }
}
