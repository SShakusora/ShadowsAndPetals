package com.sshakusora.shadowsandpetals.blockentity.irori;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class IroriComponentTopology {
    private static final int MAX_CONNECTED_SIZE = 4;
    private static final int MAX_CONNECTED_BLOCKS = MAX_CONNECTED_SIZE * MAX_CONNECTED_SIZE;

    private IroriComponentTopology() {
    }

    public static ConnectionSelection selectConnections(
            BlockGetter level,
            BlockPos origin,
            BlockState originState
    ) {
        Block iroriBlock = originState.getBlock();
        int candidateMask = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(origin.relative(direction)).is(iroriBlock)) {
                candidateMask |= directionMask(direction);
            }
        }

        ConnectionSelection best = new ConnectionSelection(0, Set.of(origin.immutable()));
        int bestSize = 1;
        int bestConnectionCount = 0;

        for (int connectionMask = candidateMask; connectionMask >= 0; connectionMask = (connectionMask - 1) & candidateMask) {
            ComponentTraversal traversal = traverseCandidate(
                    level,
                    origin,
                    originState,
                    iroriBlock,
                    connectionMask
            );
            int connectionCount = Integer.bitCount(connectionMask);
            int componentSize = traversal.positions().size();
            if (traversal.valid()
                    && (componentSize > bestSize || componentSize == bestSize && connectionCount > bestConnectionCount)) {
                best = new ConnectionSelection(connectionMask, traversal.positions());
                bestSize = componentSize;
                bestConnectionCount = connectionCount;
            }
            if (connectionMask == 0) {
                break;
            }
        }

        return best;
    }

    public static Set<BlockPos> collectConnectedComponent(BlockGetter level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos originImmutable = origin.immutable();
        Block componentBlock = level.getBlockState(origin).getBlock();
        queue.add(originImmutable);
        visited.add(originImmutable);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.removeFirst();
            BlockState currentState = level.getBlockState(currentPos);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (!hasConnection(currentState, direction)) {
                    continue;
                }

                BlockPos nextPos = currentPos.relative(direction).immutable();
                if (visited.contains(nextPos)) {
                    continue;
                }

                BlockState nextState = level.getBlockState(nextPos);
                if (!nextState.is(componentBlock) || !hasConnection(nextState, direction.getOpposite())) {
                    continue;
                }

                visited.add(nextPos);
                queue.add(nextPos);
            }
        }

        return visited;
    }

    public static BlockPos electMaster(Set<BlockPos> positions) {
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("Empty positions");
        }
        if (positions.size() == 1) {
            return positions.iterator().next();
        }

        Bounds bounds = bounds(positions);
        int y = positions.iterator().next().getY();
        return new BlockPos(
                Math.floorDiv(bounds.minX() + bounds.maxX(), 2),
                y,
                Math.floorDiv(bounds.minZ() + bounds.maxZ(), 2)
        );
    }

    public static Bounds bounds(BlockGetter level, BlockPos origin) {
        return bounds(collectConnectedComponent(level, origin));
    }

    public static Bounds bounds(Set<BlockPos> component) {
        if (component.isEmpty()) {
            throw new IllegalArgumentException("Empty component");
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : component) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new Bounds(minX, maxX, minZ, maxZ);
    }

    public static Set<BlockPos> centerPositions(Set<BlockPos> component, BlockPos fallback) {
        if (component.isEmpty()) {
            return Set.of();
        }

        Bounds bounds = bounds(component);
        int y = component.iterator().next().getY();
        Set<BlockPos> centerPositions = new HashSet<>();
        for (int x = bounds.centerMinX(); x <= bounds.centerMaxX(); x++) {
            for (int z = bounds.centerMinZ(); z <= bounds.centerMaxZ(); z++) {
                BlockPos centerPos = new BlockPos(x, y, z);
                if (component.contains(centerPos)) {
                    centerPositions.add(centerPos);
                }
            }
        }
        if (centerPositions.isEmpty() && component.contains(fallback)) {
            centerPositions.add(fallback.immutable());
        }
        return Set.copyOf(centerPositions);
    }

    public static Layout layout(int width, int depth) {
        if (width <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Component dimensions must be positive");
        }

        boolean widthEven = width % 2 == 0;
        boolean depthEven = depth % 2 == 0;
        return new Layout(
                width,
                depth,
                widthEven ? 0.5D : 0.0D,
                depthEven ? 0.5D : 0.0D,
                widthEven && !depthEven,
                widthEven ? 2 : 1,
                depthEven ? 2 : 1
        );
    }

    private static ComponentTraversal traverseCandidate(
            BlockGetter level,
            BlockPos origin,
            BlockState originState,
            Block componentBlock,
            int originConnectionMask
    ) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos originImmutable = origin.immutable();
        queue.add(originImmutable);
        visited.add(originImmutable);
        MutableBounds bounds = new MutableBounds(originImmutable);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.removeFirst();
            BlockState currentState = stateAt(level, origin, originState, currentPos);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos nextPos = currentPos.relative(direction).immutable();
                if (visited.contains(nextPos)
                        || !stateAt(level, origin, originState, nextPos).is(componentBlock)) {
                    continue;
                }
                if (hasCandidateConnection(origin, currentPos, currentState, direction, originConnectionMask)) {
                    visited.add(nextPos);
                    if (visited.size() > MAX_CONNECTED_BLOCKS || !bounds.include(nextPos)) {
                        return new ComponentTraversal(visited, false);
                    }
                    queue.add(nextPos);
                }
            }
        }

        return new ComponentTraversal(visited, bounds.isFilledBy(visited.size()));
    }

    private static BlockState stateAt(BlockGetter level, BlockPos origin, BlockState originState, BlockPos pos) {
        return pos.equals(origin) ? originState : level.getBlockState(pos);
    }

    private static boolean hasCandidateConnection(
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
        return hasConnection(state, direction);
    }

    private static boolean hasConnection(BlockState state, Direction direction) {
        BooleanProperty property = connectionProperty(direction);
        return state.hasProperty(property) && state.getValue(property);
    }

    private static BooleanProperty connectionProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockStateProperties.NORTH;
            case EAST -> BlockStateProperties.EAST;
            case SOUTH -> BlockStateProperties.SOUTH;
            case WEST -> BlockStateProperties.WEST;
            default -> throw new IllegalArgumentException("Unsupported direction: " + direction);
        };
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

    public record ConnectionSelection(int connectionMask, Set<BlockPos> positions) {
        public ConnectionSelection {
            positions = Set.copyOf(positions);
        }

        public BlockState applyTo(BlockState state) {
            return state
                    .setValue(BlockStateProperties.NORTH, (connectionMask & directionMask(Direction.NORTH)) != 0)
                    .setValue(BlockStateProperties.EAST, (connectionMask & directionMask(Direction.EAST)) != 0)
                    .setValue(BlockStateProperties.SOUTH, (connectionMask & directionMask(Direction.SOUTH)) != 0)
                    .setValue(BlockStateProperties.WEST, (connectionMask & directionMask(Direction.WEST)) != 0);
        }
    }

    public record Bounds(int minX, int maxX, int minZ, int maxZ) {
        public int width() {
            return maxX - minX + 1;
        }

        public int depth() {
            return maxZ - minZ + 1;
        }

        public int centerMinX() {
            return minX + (width() - 1) / 2;
        }

        public int centerMaxX() {
            return minX + width() / 2;
        }

        public int centerMinZ() {
            return minZ + (depth() - 1) / 2;
        }

        public int centerMaxZ() {
            return minZ + depth() / 2;
        }

        public boolean containsCenter(BlockPos pos) {
            return pos.getX() >= centerMinX()
                    && pos.getX() <= centerMaxX()
                    && pos.getZ() >= centerMinZ()
                    && pos.getZ() <= centerMaxZ();
        }
    }

    public record Layout(
            int width,
            int depth,
            double offsetX,
            double offsetZ,
            boolean rotated,
            int centerWidth,
            int centerDepth
    ) {
    }

    private static final class MutableBounds {
        private int minX;
        private int maxX;
        private int minZ;
        private int maxZ;

        private MutableBounds(BlockPos initialPos) {
            this.minX = initialPos.getX();
            this.maxX = initialPos.getX();
            this.minZ = initialPos.getZ();
            this.maxZ = initialPos.getZ();
        }

        private boolean include(BlockPos pos) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
            return width() <= MAX_CONNECTED_SIZE && depth() <= MAX_CONNECTED_SIZE;
        }

        private boolean isFilledBy(int blockCount) {
            return blockCount == width() * depth();
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int depth() {
            return maxZ - minZ + 1;
        }
    }

    private record ComponentTraversal(Set<BlockPos> positions, boolean valid) {
    }
}
