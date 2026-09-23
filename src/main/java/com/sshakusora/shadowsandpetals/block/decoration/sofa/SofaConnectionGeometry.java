package com.sshakusora.shadowsandpetals.block.decoration.sofa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.*;

/** Converts model-local sofa ports into world-space ports and matches them. */
final class SofaConnectionGeometry {
    private static final Map<Direction, Map<SofaBlock.SofaShape, Set<SofaPort>>> WORLD_PORTS =
            createWorldPorts();
    private static final Map<Direction, Map<Set<SofaPort>, SofaBlock.SofaShape>> SHAPES_BY_PORTS =
            createShapesByPorts();

    private SofaConnectionGeometry() {
    }

    static Set<SofaPort> worldPorts(
            Direction facing,
            SofaBlock.SofaShape shape
    ) {
        Map<SofaBlock.SofaShape, Set<SofaPort>> portsForFacing = WORLD_PORTS.get(facing);
        if (portsForFacing == null) {
            throw new IllegalArgumentException("Sofa facing must be horizontal: " + facing);
        }
        return portsForFacing.get(shape);
    }

    static boolean matches(
            BlockPos firstPos,
            SofaPort firstPort,
            BlockPos secondPos,
            SofaPort secondPort
    ) {
        return secondPos.equals(firstPos.relative(firstPort.face()))
                && secondPort.face() == firstPort.face().getOpposite()
                && secondPort.back() == firstPort.back();
    }

    /** Maps a complete world-space port set back to a rendered shape. */
    static SofaBlock.@Nullable SofaShape shapeForPorts(
            Direction facing,
            Set<SofaPort> ports
    ) {
        Map<Set<SofaPort>, SofaBlock.SofaShape> shapesForFacing = SHAPES_BY_PORTS.get(facing);
        if (shapesForFacing == null) {
            throw new IllegalArgumentException("Sofa facing must be horizontal: " + facing);
        }
        return shapesForFacing.get(ports);
    }

    private static Map<Direction, Map<SofaBlock.SofaShape, Set<SofaPort>>> createWorldPorts() {
        EnumMap<Direction, Map<SofaBlock.SofaShape, Set<SofaPort>>> result =
                new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            EnumMap<SofaBlock.SofaShape, Set<SofaPort>> portsForFacing =
                    new EnumMap<>(SofaBlock.SofaShape.class);
            for (SofaBlock.SofaShape shape : SofaBlock.SofaShape.values()) {
                Set<SofaPort> worldPorts = new HashSet<>();
                int quarterTurns = directionQuarterTurns(facing)
                        + shape.modelRotationDegrees() / 90;
                for (SofaPort localPort : shape.localPorts()) {
                    worldPorts.add(localPort.rotateClockwise(quarterTurns));
                }
                portsForFacing.put(shape, Set.copyOf(worldPorts));
            }
            result.put(facing, Collections.unmodifiableMap(portsForFacing));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<Direction, Map<Set<SofaPort>, SofaBlock.SofaShape>> createShapesByPorts() {
        EnumMap<Direction, Map<Set<SofaPort>, SofaBlock.SofaShape>> result =
                new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Map<Set<SofaPort>, SofaBlock.SofaShape> shapesForFacing = new HashMap<>();
            for (SofaBlock.SofaShape shape : SofaBlock.SofaShape.values()) {
                shapesForFacing.put(worldPorts(facing, shape), shape);
            }
            result.put(facing, Collections.unmodifiableMap(shapesForFacing));
        }
        return Collections.unmodifiableMap(result);
    }

    private static int directionQuarterTurns(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalArgumentException(
                    "Sofa facing must be horizontal: " + direction);
        };
    }
}
