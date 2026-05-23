package com.sshakusora.shadowsandpetals.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class VoxelShapeUtils {
    private VoxelShapeUtils() {}

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape northShape) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        VoxelShape currentShape = northShape;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(direction, currentShape.optimize());
            currentShape = rotateClockwise(currentShape);
        }

        return shapes;
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape[] rotated = new VoxelShape[]{Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> rotated[0] = Shapes.or(
                rotated[0],
                Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX)
        ));
        return rotated[0].optimize();
    }
}
