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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

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
    public static final float BASE_DRAWER_TRAVEL_DISTANCE = 7.0F / 16.0F;
    public static final float MAX_DRAWER_TRAVEL_SCALE = 1.16F;
    public static final float MAX_DRAWER_TRAVEL_DISTANCE = BASE_DRAWER_TRAVEL_DISTANCE * MAX_DRAWER_TRAVEL_SCALE;
    private static final double DRAWER_FRONT_MIN_X = 4.0D / 16.0D;
    private static final double DRAWER_FRONT_MAX_X = 12.0D / 16.0D;
    private static final double DRAWER_FRONT_MIN_Y = 8.75D / 16.0D;
    private static final double DRAWER_FRONT_MAX_Y = 13.0D / 16.0D;
    private static final float DRAWER_FRONT_INSET = 2.25F / 16.0F;
    private static final float MIN_DRAWER_OPEN_DISTANCE = DRAWER_FRONT_INSET;

    static {
        LOWER_SHAPES.putAll(VoxelShapeUtils.rotateHorizontal(LOWER_NORTH_SHAPE));
        UPPER_SHAPES.putAll(VoxelShapeUtils.rotateHorizontal(UPPER_NORTH_SHAPE));
    }

    public VanityBlock(BlockBehaviour.Properties properties) {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new VanityBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? createTickerHelper(type, BlockEntityRegistry.VANITY.get(), VanityBlockEntity::tick)
                : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockPos abovePos = pos.above();
        Level level = context.getLevel();
        if (abovePos.getY() >= level.getMaxY() || !level.getBlockState(abovePos).canBeReplaced(context)) {
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
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return canOpenDrawer(state, level, pos, hitResult) ? openMenu(state, level, pos, player) : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return canOpenDrawer(state, level, pos, hitResult) ? openMenu(state, level, pos, player) : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    private InteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        VanityBlockEntity vanityBlockEntity = getVanityBlockEntity(level, state, pos);
        if (vanityBlockEntity != null) {
            player.openMenu(vanityBlockEntity);
            player.awardStat(Stats.OPEN_BARREL);
        }
        return InteractionResult.SUCCESS;
    }

    private boolean canOpenDrawer(BlockState state, Level level, BlockPos pos, BlockHitResult hitResult) {
        Direction facing = state.getValue(FACING);
        Direction hitFace = hitResult.getDirection();
        if (hitFace == facing.getOpposite()) {
            return false;
        }

        if (hitFace.getAxis().isVertical() && state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return false;
        }

        BlockPos basePos = getBasePos(state, pos);
        float drawerTravelLimit = computeDrawerTravelLimit(level, basePos, facing);
        updateCachedDrawerTravelLimit(level, basePos, drawerTravelLimit);
        return drawerTravelLimit > MIN_DRAWER_OPEN_DISTANCE;
    }

    public static float computeDrawerTravelLimit(Level level, BlockPos basePos, Direction facing) {
        BlockPos frontPos = basePos.relative(facing);
        VoxelShape collisionShape = level.getBlockState(frontPos).getCollisionShape(level, frontPos);
        if (collisionShape.isEmpty()) {
            return MAX_DRAWER_TRAVEL_DISTANCE;
        }

        float[] nearestObstacleDistance = {MAX_DRAWER_TRAVEL_DISTANCE};
        collisionShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            AABB obstacleBox = new AABB(
                    frontPos.getX() + minX,
                    frontPos.getY() + minY,
                    frontPos.getZ() + minZ,
                    frontPos.getX() + maxX,
                    frontPos.getY() + maxY,
                    frontPos.getZ() + maxZ
            );
            if (overlapsDrawerFrontProjection(basePos, facing, obstacleBox)) {
                float obstacleDistance = computeObstacleDistanceFromDrawerFront(basePos, facing, obstacleBox);
                nearestObstacleDistance[0] = Math.min(nearestObstacleDistance[0], obstacleDistance);
            }
        });

        return nearestObstacleDistance[0];
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

        if (state.getValue(HALF) == DoubleBlockHalf.LOWER && direction == state.getValue(FACING)) {
            ticks.scheduleTick(pos, this, 1);
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
                : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) != DoubleBlockHalf.UPPER || level.getBlockState(pos.below()).is(this);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            DoublePlantBlock.preventDropFromBottomPart(level, pos, state, player);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? List.of() : super.getDrops(state, params);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
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
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(getBaseBlockEntity(level, state, pos));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos basePos = getBasePos(state, pos);
        VanityBlockEntity vanityBlockEntity = getVanityBlockEntity(level, state, pos);
        if (vanityBlockEntity != null) {
            vanityBlockEntity.updateDrawerTravelLimit(computeDrawerTravelLimit(level, basePos, state.getValue(FACING)), true);
            vanityBlockEntity.recheckOpen();
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, getBasePos(state, pos));
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity blockEntity = getBaseBlockEntity(level, state, pos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }

    public static BlockPos getBasePos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    private static @Nullable BlockEntity getBaseBlockEntity(Level level, BlockState state, BlockPos pos) {
        return level.getBlockEntity(getBasePos(state, pos));
    }

    private static @Nullable VanityBlockEntity getVanityBlockEntity(Level level, BlockState state, BlockPos pos) {
        BlockEntity blockEntity = getBaseBlockEntity(level, state, pos);
        return blockEntity instanceof VanityBlockEntity vanityBlockEntity ? vanityBlockEntity : null;
    }

    private static void updateCachedDrawerTravelLimit(Level level, BlockPos basePos, float drawerTravelLimit) {
        if (level.getBlockEntity(basePos) instanceof VanityBlockEntity vanityBlockEntity) {
            vanityBlockEntity.updateDrawerTravelLimit(drawerTravelLimit, !level.isClientSide());
        }
    }

    private static boolean overlapsDrawerFrontProjection(BlockPos basePos, Direction facing, AABB obstacleBox) {
        double minY = basePos.getY() + DRAWER_FRONT_MIN_Y;
        double maxY = basePos.getY() + DRAWER_FRONT_MAX_Y;
        if (!overlaps(minY, maxY, obstacleBox.minY, obstacleBox.maxY)) {
            return false;
        }

        return switch (facing.getAxis()) {
            case Z -> overlaps(basePos.getX() + DRAWER_FRONT_MIN_X, basePos.getX() + DRAWER_FRONT_MAX_X, obstacleBox.minX, obstacleBox.maxX);
            case X -> overlaps(basePos.getZ() + DRAWER_FRONT_MIN_X, basePos.getZ() + DRAWER_FRONT_MAX_X, obstacleBox.minZ, obstacleBox.maxZ);
            case Y -> false;
        };
    }

    private static float computeObstacleDistanceFromDrawerFront(BlockPos basePos, Direction facing, AABB obstacleBox) {
        return switch (facing) {
            case NORTH -> clampTravelDistance((float) ((basePos.getZ() + DRAWER_FRONT_INSET) - obstacleBox.maxZ));
            case SOUTH -> clampTravelDistance((float) (obstacleBox.minZ - (basePos.getZ() + 1.0D - DRAWER_FRONT_INSET)));
            case WEST -> clampTravelDistance((float) ((basePos.getX() + DRAWER_FRONT_INSET) - obstacleBox.maxX));
            case EAST -> clampTravelDistance((float) (obstacleBox.minX - (basePos.getX() + 1.0D - DRAWER_FRONT_INSET)));
            default -> MAX_DRAWER_TRAVEL_DISTANCE;
        };
    }

    private static float clampTravelDistance(float travelDistance) {
        if (travelDistance <= 0.0F) {
            return 0.0F;
        }
        return Math.min(travelDistance, MAX_DRAWER_TRAVEL_DISTANCE);
    }

    private static boolean overlaps(double minA, double maxA, double minB, double maxB) {
        return overlapAmount(minA, maxA, minB, maxB) > 0.0D;
    }

    private static double overlapAmount(double minA, double maxA, double minB, double maxB) {
        return Math.min(maxA, maxB) - Math.max(minA, minB);
    }
}
