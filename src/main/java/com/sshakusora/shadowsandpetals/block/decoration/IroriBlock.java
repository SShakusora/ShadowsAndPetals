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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

public class IroriBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<IroriBlock> CODEC = Block.simpleCodec(IroriBlock::new);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty SHIFT_PLACED = BooleanProperty.create("shift_placed");
    private static final int MAX_CONNECTED_SIZE = 5;
    private static final int MAX_CONNECTED_BLOCKS = MAX_CONNECTED_SIZE * MAX_CONNECTED_SIZE;

    private static final VoxelShape BASE_SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);
    private static final VoxelShape STANDALONE_SHAPE = Shapes.or(
            BASE_SHAPE,
            box(0.0D, 10.0D, 0.0D, 16.0D, 15.0D, 1.0D),
            box(15.0D, 10.0D, 1.0D, 16.0D, 15.0D, 15.0D),
            box(0.0D, 10.0D, 15.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 10.0D, 1.0D, 1.0D, 15.0D, 15.0D),
            box(0.0D, 15.0D, 0.0D, 13.0D, 16.0D, 3.0D),
            box(13.0D, 15.0D, 0.0D, 16.0D, 16.0D, 13.0D),
            box(3.0D, 15.0D, 13.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 15.0D, 3.0D, 3.0D, 16.0D, 16.0D)
    ).optimize();
    private static final VoxelShape SINGLE_EDGE_NORTH_SHAPE = Shapes.or(
            BASE_SHAPE,
            box(0.0D, 10.0D, 0.0D, 16.0D, 15.0D, 1.0D),
            box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 4.0D)
    );
    private static final VoxelShape DOUBLE_EDGE_NORTH_SOUTH_SHAPE = Shapes.or(
            SINGLE_EDGE_NORTH_SHAPE,
            box(0.0D, 10.0D, 15.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 15.0D, 12.0D, 16.0D, 16.0D, 16.0D)
    );
    private static final VoxelShape CORNER_NORTH_EAST_SHAPE = Shapes.or(
            BASE_SHAPE,
            box(0.0D, 10.0D, 0.0D, 15.0D, 15.0D, 1.0D),
            box(15.0D, 10.0D, 0.0D, 16.0D, 15.0D, 1.0D),
            box(15.0D, 10.0D, 1.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 15.0D, 0.0D, 12.0D, 16.0D, 4.0D),
            box(12.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    );
    private static final VoxelShape END_NORTH_EAST_WEST_SHAPE = Shapes.or(
            BASE_SHAPE,
            box(0.0D, 10.0D, 0.0D, 16.0D, 15.0D, 2.0D),
            box(15.0D, 10.0D, 1.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 10.0D, 1.0D, 1.0D, 15.0D, 16.0D),
            box(0.0D, 15.0D, 0.0D, 12.0D, 16.0D, 4.0D),
            box(12.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 15.0D, 4.0D, 4.0D, 16.0D, 16.0D)
    );
    private static final Map<Direction, VoxelShape> SINGLE_EDGE_SHAPES = VoxelShapeUtils.rotateHorizontal(SINGLE_EDGE_NORTH_SHAPE);
    private static final Map<Direction, VoxelShape> DOUBLE_EDGE_SHAPES = VoxelShapeUtils.rotateHorizontal(DOUBLE_EDGE_NORTH_SOUTH_SHAPE);
    private static final Map<Direction, VoxelShape> CORNER_SHAPES = VoxelShapeUtils.rotateHorizontal(CORNER_NORTH_EAST_SHAPE);
    private static final Map<Direction, VoxelShape> END_SHAPES = createEndShapes();
    private static final VoxelShape[] SHAPES_BY_CONNECTIONS = createShapes();

    public IroriBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(WATERLOGGED, false)
                .setValue(SHIFT_PLACED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER)
                .setValue(SHIFT_PLACED, context.isSecondaryUseActive());
        return updateConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED, SHIFT_PLACED);
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
            return updateConnections(state.setValue(getConnectionProperty(direction), false), level, pos);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES_BY_CONNECTIONS[getConnectionMask(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected MapCodec<IroriBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        int candidateMask = 0;
        boolean shiftPlaced = state.getValue(SHIFT_PLACED);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isConnectableIrori(level.getBlockState(pos.relative(direction)), shiftPlaced)) {
                candidateMask |= directionMask(direction);
            }
        }

        int bestMask = 0;
        int bestSize = 1;
        int bestConnectionCount = 0;
        for (int connectionMask = candidateMask; connectionMask >= 0; connectionMask = (connectionMask - 1) & candidateMask) {
            ConnectedComponent component = collectConnectedComponent(level, pos, state, connectionMask);
            int connectionCount = Integer.bitCount(connectionMask);
            if (component.isValid()
                    && (component.size() > bestSize || component.size() == bestSize && connectionCount > bestConnectionCount)) {
                bestMask = connectionMask;
                bestSize = component.size();
                bestConnectionCount = connectionCount;
            }
            if (connectionMask == 0) {
                break;
            }
        }

        return state
                .setValue(NORTH, (bestMask & directionMask(Direction.NORTH)) != 0)
                .setValue(EAST, (bestMask & directionMask(Direction.EAST)) != 0)
                .setValue(SOUTH, (bestMask & directionMask(Direction.SOUTH)) != 0)
                .setValue(WEST, (bestMask & directionMask(Direction.WEST)) != 0);
    }

    private ConnectedComponent collectConnectedComponent(
            BlockGetter level,
            BlockPos origin,
            BlockState originState,
            int originConnectionMask
    ) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos originImmutable = origin.immutable();
        queue.add(originImmutable);
        visited.add(originImmutable);
        ComponentBounds bounds = new ComponentBounds(originImmutable);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.removeFirst();
            BlockState currentState = stateAt(level, origin, originState, currentPos);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos nextPos = currentPos.relative(direction).immutable();
                if (visited.contains(nextPos) || !isConnectableIrori(stateAt(level, origin, originState, nextPos), originState.getValue(SHIFT_PLACED))) {
                    continue;
                }
                if (hasConnection(origin, currentPos, currentState, direction, originConnectionMask)) {
                    visited.add(nextPos);
                    if (visited.size() > MAX_CONNECTED_BLOCKS || !bounds.include(nextPos)) {
                        return ConnectedComponent.invalid(visited.size());
                    }
                    queue.add(nextPos);
                }
            }
        }

        return new ConnectedComponent(visited.size(), bounds.isFilledBy(visited.size()));
    }

    private BlockState stateAt(BlockGetter level, BlockPos origin, BlockState originState, BlockPos pos) {
        return pos.equals(origin) ? originState : level.getBlockState(pos);
    }

    private boolean isConnectableIrori(BlockState state, boolean shiftPlaced) {
        return state.is(this) && state.getValue(SHIFT_PLACED) == shiftPlaced;
    }

    private static boolean hasConnection(
            BlockPos origin,
            BlockPos pos,
            BlockState state,
            Direction direction,
            int originConnectionMask
    ) {
        if (pos.equals(origin)) {
            return (originConnectionMask & directionMask(direction)) != 0;
        }
        if (pos.relative(direction).equals(origin)) {
            return (originConnectionMask & directionMask(direction.getOpposite())) != 0;
        }
        return state.getValue(getConnectionProperty(direction));
    }

    public static BooleanProperty getConnectionProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Unsupported direction: " + direction);
        };
    }

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < shapes.length; mask++) {
            if (mask == 0) {
                shapes[mask] = STANDALONE_SHAPE;
                continue;
            }

            boolean edgeNorth = (mask & 1) == 0;
            boolean edgeEast = (mask & 2) == 0;
            boolean edgeSouth = (mask & 4) == 0;
            boolean edgeWest = (mask & 8) == 0;
            shapes[mask] = getShapeForEdges(edgeNorth, edgeEast, edgeSouth, edgeWest).optimize();
        }
        return shapes;
    }

    private static VoxelShape getShapeForEdges(boolean north, boolean east, boolean south, boolean west) {
        int edgeCount = (north ? 1 : 0) + (east ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0);
        return switch (edgeCount) {
            case 0 -> BASE_SHAPE;
            case 1 -> SINGLE_EDGE_SHAPES.get(north ? Direction.NORTH : east ? Direction.EAST : south ? Direction.SOUTH : Direction.WEST);
            case 2 -> getDoubleEdgeShape(north, east, south, west);
            case 3 -> END_SHAPES.get(!north ? Direction.NORTH : !east ? Direction.EAST : !south ? Direction.SOUTH : Direction.WEST);
            case 4 -> STANDALONE_SHAPE;
            default -> throw new IllegalStateException("Unexpected edge count: " + edgeCount);
        };
    }

    private static VoxelShape getDoubleEdgeShape(boolean north, boolean east, boolean south, boolean west) {
        if (north && south) {
            return DOUBLE_EDGE_SHAPES.get(Direction.NORTH);
        }
        if (east && west) {
            return DOUBLE_EDGE_SHAPES.get(Direction.EAST);
        }
        if (north && east) {
            return CORNER_SHAPES.get(Direction.NORTH);
        }
        if (east && south) {
            return CORNER_SHAPES.get(Direction.EAST);
        }
        if (south && west) {
            return CORNER_SHAPES.get(Direction.SOUTH);
        }
        return CORNER_SHAPES.get(Direction.WEST);
    }

    private static Map<Direction, VoxelShape> createEndShapes() {
        Map<Direction, VoxelShape> rotated = VoxelShapeUtils.rotateHorizontal(END_NORTH_EAST_WEST_SHAPE);
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.SOUTH, rotated.get(Direction.NORTH));
        shapes.put(Direction.WEST, rotated.get(Direction.EAST));
        shapes.put(Direction.NORTH, rotated.get(Direction.SOUTH));
        shapes.put(Direction.EAST, rotated.get(Direction.WEST));
        return shapes;
    }

    private static int getConnectionMask(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0)
                | (state.getValue(EAST) ? 2 : 0)
                | (state.getValue(SOUTH) ? 4 : 0)
                | (state.getValue(WEST) ? 8 : 0);
    }

    private static int directionMask(Direction direction) {
        return switch (direction) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 4;
            case WEST -> 8;
            default -> throw new IllegalArgumentException("Unsupported direction: " + direction);
        };
    }

    private static final class ComponentBounds {
        private int minX;
        private int maxX;
        private int minZ;
        private int maxZ;

        private ComponentBounds(BlockPos initialPos) {
            this.minX = initialPos.getX();
            this.maxX = initialPos.getX();
            this.minZ = initialPos.getZ();
            this.maxZ = initialPos.getZ();
        }

        private boolean include(BlockPos pos) {
            this.minX = Math.min(this.minX, pos.getX());
            this.maxX = Math.max(this.maxX, pos.getX());
            this.minZ = Math.min(this.minZ, pos.getZ());
            this.maxZ = Math.max(this.maxZ, pos.getZ());
            return width() <= MAX_CONNECTED_SIZE && depth() <= MAX_CONNECTED_SIZE;
        }

        private boolean isFilledBy(int blockCount) {
            return blockCount == width() * depth();
        }

        private int width() {
            return this.maxX - this.minX + 1;
        }

        private int depth() {
            return this.maxZ - this.minZ + 1;
        }
    }

    private record ConnectedComponent(int size, boolean isValid) {
        private static ConnectedComponent invalid(int size) {
            return new ConnectedComponent(size, false);
        }
    }
}
