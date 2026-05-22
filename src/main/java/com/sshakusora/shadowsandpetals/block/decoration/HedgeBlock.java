package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HedgeBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<HedgeBlock> CODEC = simpleCodec(HedgeBlock::new);

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape OUTLINE_CORE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    private static final VoxelShape OUTLINE_NORTH_ARM = Block.box(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 4.0D);
    private static final VoxelShape COLLISION_CORE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 24.0D, 12.0D);
    private static final VoxelShape COLLISION_NORTH_ARM = Block.box(4.0D, 0.0D, 0.0D, 12.0D, 24.0D, 4.0D);
    private static final VoxelShape[] OUTLINE_SHAPES = VoxelShapeUtils.buildHorizontalConnectionShapes(OUTLINE_CORE, OUTLINE_NORTH_ARM);
    private static final VoxelShape[] COLLISION_SHAPES = VoxelShapeUtils.buildHorizontalConnectionShapes(COLLISION_CORE, COLLISION_NORTH_ARM);

    public HedgeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<HedgeBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(defaultBlockState(), context.getLevel(), context.getClickedPos())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
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

        if (direction.getAxis().isHorizontal()) {
            return state.setValue(propertyFor(direction), canConnectTo(neighborState));
        }

        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return pathComputationType == PathComputationType.WATER && state.getFluidState().is(Fluids.WATER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE_SHAPES[connectionMask(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPES[connectionMask(state)];
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }

    private static BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(propertyFor(direction), canConnectTo(level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    private static boolean canConnectTo(BlockState neighborState) {
        return neighborState.getBlock() instanceof HedgeBlock;
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Unsupported direction: " + direction);
        };
    }

    private static int connectionMask(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) {
            mask |= 1;
        }
        if (state.getValue(EAST)) {
            mask |= 1 << 1;
        }
        if (state.getValue(SOUTH)) {
            mask |= 1 << 2;
        }
        if (state.getValue(WEST)) {
            mask |= 1 << 3;
        }
        return mask;
    }
}
