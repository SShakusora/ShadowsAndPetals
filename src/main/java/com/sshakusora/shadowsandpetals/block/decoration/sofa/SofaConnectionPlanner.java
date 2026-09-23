package com.sshakusora.shadowsandpetals.block.decoration.sofa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Pure connection planner for sofas.
 *
 * <p>The planner treats a connection as an edge carrying the two complete
 * port values. A shape is legal only when its complete port set is exactly the
 * set of selected edges at that sofa. Therefore a successful plan cannot
 * contain an exposed connection port.</p>
 */
final class SofaConnectionPlanner {
    private SofaConnectionPlanner() {
    }

    @FunctionalInterface
    interface SofaLayout {
        @Nullable SofaState stateAt(BlockPos pos);
    }

    record SofaState(Direction facing, SofaBlock.SofaShape shape) {
    }

    record Connection(
            BlockPos first,
            BlockPos second,
            SofaPort firstPort,
            SofaPort secondPort
    ) {
        Connection {
            if (first.equals(second)
                    || !SofaConnectionGeometry.matches(first, firstPort, second, secondPort)
                    && !SofaConnectionGeometry.matches(second, secondPort, first, firstPort)) {
                throw new IllegalArgumentException("Invalid sofa connection");
            }
        }

        static Connection between(
                BlockPos first,
                SofaPort firstPort,
                BlockPos second,
                SofaPort secondPort
        ) {
            if (first.asLong() <= second.asLong()) {
                return new Connection(
                        first.immutable(),
                        second.immutable(),
                        firstPort,
                        secondPort);
            }
            return new Connection(
                    second.immutable(),
                    first.immutable(),
                    secondPort,
                    firstPort);
        }

        boolean touches(BlockPos pos) {
            return first.equals(pos) || second.equals(pos);
        }

    }

    static Map<BlockPos, SofaBlock.SofaShape> planPlacement(
            SofaLayout layout,
            BlockPos placedPos
    ) {
        Snapshot snapshot = new Snapshot(layout);
        SofaState placed = snapshot.requireState(placedPos);
        Set<BlockPos> seeds = neighbourhoodOf(placedPos);
        seeds.add(placedPos);
        Set<Connection> existing = collectMatchedConnections(snapshot, seeds);

        Search search = new Search(
                snapshot,
                placedPos,
                true,
                existing);

        for (SofaBlock.SofaShape shape : SofaBlock.SofaShape.values()) {
            Set<Connection> initial = new HashSet<>(existing);
            boolean canExposeEveryPort = true;
            for (SofaPort port : SofaConnectionGeometry.worldPorts(placed.facing(), shape)) {
                Connection candidate = possiblePlacementConnection(snapshot, placedPos, port);
                if (candidate == null) {
                    canExposeEveryPort = false;
                    break;
                }
                initial.add(candidate);
            }
            if (canExposeEveryPort) {
                search.run(initial, activePositions(snapshot, seeds, initial));
            }
        }

        return search.bestShapesOrSingle();
    }

    static Map<BlockPos, SofaBlock.SofaShape> planRemoval(
            SofaLayout layout,
            BlockPos removedPos
    ) {
        Snapshot snapshot = new Snapshot(layout);
        Set<BlockPos> seeds = neighbourhoodOf(removedPos);
        Set<Connection> existing = collectMatchedConnections(snapshot, seeds);
        Search search = new Search(
                snapshot,
                removedPos,
                false,
                existing);
        search.run(existing, activePositions(snapshot, seeds, existing));
        return search.bestShapesOrSingle();
    }

    private static Set<Connection> collectMatchedConnections(
            Snapshot snapshot,
            Collection<BlockPos> seeds
    ) {
        Set<Connection> connections = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos seed : seeds) {
            if (snapshot.stateAt(seed) != null && visited.add(seed)) {
                queue.addLast(seed);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            SofaState state = snapshot.stateAt(pos);
            if (state == null) {
                continue;
            }
            for (SofaPort port : snapshot.worldPorts(pos)) {
                BlockPos neighbourPos = pos.relative(port.face());
                SofaState neighbour = snapshot.stateAt(neighbourPos);
                if (neighbour == null) {
                    continue;
                }
                SofaPort neighbourPort = new SofaPort(port.face().getOpposite(), port.back());
                if (snapshot.worldPorts(neighbourPos).contains(neighbourPort)) {
                    connections.add(Connection.between(pos, port, neighbourPos, neighbourPort));
                    if (visited.add(neighbourPos)) {
                        queue.addLast(neighbourPos);
                    }
                }
            }
        }
        return connections;
    }

    private static @Nullable Connection possiblePlacementConnection(
            Snapshot snapshot,
            BlockPos placedPos,
            SofaPort placedPort
    ) {
        BlockPos neighbourPos = placedPos.relative(placedPort.face());
        SofaState neighbour = snapshot.stateAt(neighbourPos);
        if (neighbour == null) {
            return null;
        }

        SofaPort expectedNeighbourPort = new SofaPort(
                placedPort.face().getOpposite(), placedPort.back());
        for (SofaBlock.SofaShape shape : SofaBlock.SofaShape.values()) {
            if (SofaConnectionGeometry.worldPorts(neighbour.facing(), shape)
                    .contains(expectedNeighbourPort)) {
                return Connection.between(
                        placedPos,
                        placedPort,
                        neighbourPos,
                        expectedNeighbourPort);
            }
        }
        return null;
    }

    private static Set<BlockPos> neighbourhoodOf(BlockPos pos) {
        Set<BlockPos> result = new HashSet<>();
        result.add(pos.north());
        result.add(pos.east());
        result.add(pos.south());
        result.add(pos.west());
        return result;
    }

    private static Set<BlockPos> activePositions(
            Snapshot snapshot,
            Collection<BlockPos> seeds,
            Collection<Connection> connections
    ) {
        Set<BlockPos> result = new HashSet<>();
        for (BlockPos seed : seeds) {
            if (snapshot.stateAt(seed) != null) {
                result.add(seed);
            }
        }
        for (Connection connection : connections) {
            result.add(connection.first());
            result.add(connection.second());
        }
        return result;
    }

    /** Caches the immutable sofa view used by one planning operation. */
    private static final class Snapshot {
        private final SofaLayout layout;
        private final Map<BlockPos, SofaState> states = new HashMap<>();
        private final Set<BlockPos> queried = new HashSet<>();

        private Snapshot(SofaLayout layout) {
            this.layout = layout;
        }

        private @Nullable SofaState stateAt(BlockPos pos) {
            BlockPos key = pos.immutable();
            if (queried.add(key)) {
                SofaState state = layout.stateAt(key);
                if (state != null) {
                    states.put(key, state);
                }
                return state;
            }
            return states.get(key);
        }

        private Set<SofaPort> worldPorts(BlockPos pos) {
            SofaState state = stateAt(pos);
            return state == null
                    ? Set.of()
                    : SofaConnectionGeometry.worldPorts(state.facing(), state.shape());
        }

        private SofaState requireState(BlockPos pos) {
            SofaState state = stateAt(pos);
            if (state == null) {
                throw new IllegalArgumentException("Expected sofa at " + pos);
            }
            return state;
        }
    }

    /** Indexes all ports and incident edges for one search branch. */
    private record ConnectionIndex(
            Map<BlockPos, Set<SofaPort>> portsByPosition,
            Map<BlockPos, List<Connection>> connectionsByPosition
    ) {
        private static ConnectionIndex create(Set<Connection> connections) {
            Map<BlockPos, Set<SofaPort>> ports = new HashMap<>();
            Map<BlockPos, List<Connection>> incident = new HashMap<>();
            for (Connection connection : connections) {
                ports.computeIfAbsent(connection.first(), ignored -> new HashSet<>())
                        .add(connection.firstPort());
                ports.computeIfAbsent(connection.second(), ignored -> new HashSet<>())
                        .add(connection.secondPort());
                incident.computeIfAbsent(connection.first(), ignored -> new ArrayList<>())
                        .add(connection);
                incident.computeIfAbsent(connection.second(), ignored -> new ArrayList<>())
                        .add(connection);
            }

            Map<BlockPos, Set<SofaPort>> immutablePorts = new HashMap<>();
            for (Map.Entry<BlockPos, Set<SofaPort>> entry : ports.entrySet()) {
                immutablePorts.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }
            Comparator<Connection> comparator = connectionComparator();
            Map<BlockPos, List<Connection>> immutableIncident = new HashMap<>();
            for (Map.Entry<BlockPos, List<Connection>> entry : incident.entrySet()) {
                entry.getValue().sort(comparator);
                immutableIncident.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return new ConnectionIndex(immutablePorts, immutableIncident);
        }

        private Set<SofaPort> portsAt(BlockPos pos) {
            return portsByPosition.getOrDefault(pos, Set.of());
        }

        private List<Connection> connectionsAt(BlockPos pos) {
            return connectionsByPosition.getOrDefault(pos, List.of());
        }
    }

    private static Comparator<Connection> connectionComparator() {
        return Comparator
                .comparingLong((Connection connection) -> connection.first().asLong())
                .thenComparingLong(connection -> connection.second().asLong())
                .thenComparingInt(connection -> connection.firstPort().face().ordinal())
                .thenComparingInt(connection -> connection.firstPort().back().ordinal());
    }

    private static final class Search {
        private final Snapshot snapshot;
        private final BlockPos mutationPos;
        private final boolean placement;
        private final Set<Connection> existing;
        private final Set<BlockPos> activePositionSet = new HashSet<>();
        private final List<BlockPos> orderedActivePositions;
        private final Set<Set<Connection>> visited = new HashSet<>();
        private @Nullable Candidate best;

        private Search(
                Snapshot snapshot,
                BlockPos mutationPos,
                boolean placement,
                Set<Connection> existing
        ) {
            this.snapshot = snapshot;
            this.mutationPos = mutationPos;
            this.placement = placement;
            this.existing = Set.copyOf(existing);
            this.orderedActivePositions = new ArrayList<>();
        }

        private void run(Set<Connection> connections, Set<BlockPos> activePositions) {
            if (activePositionSet.addAll(activePositions)) {
                orderedActivePositions.clear();
                orderedActivePositions.addAll(activePositionSet);
                orderedActivePositions.sort(Comparator.comparingLong(BlockPos::asLong));
            }
            Set<Connection> state = Set.copyOf(connections);
            if (!visited.add(state)) {
                return;
            }

            ConnectionIndex connectionIndex = ConnectionIndex.create(state);
            BlockPos invalid = findInvalidPosition(connectionIndex);
            if (invalid != null) {
                List<Connection> incident = connectionIndex.connectionsAt(invalid);
                for (Connection connection : incident) {
                    Set<Connection> next = new HashSet<>(state);
                    next.remove(connection);
                    run(next, activePositions);
                }
                return;
            }

            Map<BlockPos, SofaBlock.SofaShape> shapes = shapesFor(connectionIndex);
            if (shapes == null) {
                return;
            }
            Candidate candidate = new Candidate(
                    shapes,
                    state,
                    retainedCount(state),
                    newConnectionCount(state),
                    changedExistingCount(shapes),
                    tieKey(shapes));
            if (best == null || isBetter(candidate, best)) {
                best = candidate;
            }
        }

        private @Nullable BlockPos findInvalidPosition(
                ConnectionIndex connectionIndex
        ) {
            for (BlockPos pos : orderedActivePositions) {
                SofaState state = snapshot.stateAt(pos);
                if (state == null) {
                    continue;
                }
                if (SofaConnectionGeometry.shapeForPorts(
                        state.facing(), connectionIndex.portsAt(pos)) == null) {
                    return pos;
                }
            }
            return null;
        }

        private @Nullable Map<BlockPos, SofaBlock.SofaShape> shapesFor(
                ConnectionIndex connectionIndex
        ) {
            Map<BlockPos, SofaBlock.SofaShape> result = new HashMap<>();
            for (BlockPos pos : orderedActivePositions) {
                SofaState state = snapshot.stateAt(pos);
                if (state == null) {
                    continue;
                }
                SofaBlock.SofaShape shape = SofaConnectionGeometry.shapeForPorts(
                        state.facing(), connectionIndex.portsAt(pos));
                if (shape == null) {
                    return null;
                }
                result.put(pos, shape);
            }
            return result;
        }

        private int retainedCount(Set<Connection> connections) {
            int count = 0;
            for (Connection connection : connections) {
                if (existing.contains(connection)) {
                    count++;
                }
            }
            return count;
        }

        private int newConnectionCount(Set<Connection> connections) {
            if (!placement) {
                return 0;
            }
            int count = 0;
            for (Connection connection : connections) {
                if (!existing.contains(connection) && connection.touches(mutationPos)) {
                    count++;
                }
            }
            return count;
        }

        private int changedExistingCount(Map<BlockPos, SofaBlock.SofaShape> shapes) {
            int count = 0;
            for (Map.Entry<BlockPos, SofaBlock.SofaShape> entry : shapes.entrySet()) {
                SofaState state = snapshot.stateAt(entry.getKey());
                if (state != null
                        && !entry.getKey().equals(mutationPos)
                        && state.shape() != entry.getValue()) {
                    count++;
                }
            }
            return count;
        }

        private String tieKey(Map<BlockPos, SofaBlock.SofaShape> shapes) {
            StringBuilder result = new StringBuilder();
            shapes.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparingLong(BlockPos::asLong)))
                    .forEach(entry -> result
                            .append(entry.getKey().asLong())
                            .append(':')
                            .append(entry.getValue().ordinal())
                            .append(';'));
            return result.toString();
        }

        private Map<BlockPos, SofaBlock.SofaShape> bestShapesOrSingle() {
            if (best != null) {
                return best.shapes();
            }
            return Map.of();
        }

        private boolean isBetter(Candidate candidate, Candidate current) {
            if (candidate.retainedConnections() != current.retainedConnections()) {
                return candidate.retainedConnections() > current.retainedConnections();
            }
            if (placement && candidate.newConnections() != current.newConnections()) {
                return candidate.newConnections() > current.newConnections();
            }
            if (candidate.connections().size() != current.connections().size()) {
                return candidate.connections().size() > current.connections().size();
            }
            if (candidate.changedExisting() != current.changedExisting()) {
                return candidate.changedExisting() < current.changedExisting();
            }
            return candidate.tieKey().compareTo(current.tieKey()) < 0;
        }

        private record Candidate(
                Map<BlockPos, SofaBlock.SofaShape> shapes,
                Set<Connection> connections,
                int retainedConnections,
                int newConnections,
                int changedExisting,
                String tieKey
        ) {
        }
    }
}
