package com.sshakusora.shadowsandpetals.block.decoration.irori;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IroriGrillVoxelShapesTest {
    private static final double PIXEL = 1.0D / 16.0D;

    @Test
    void upperSurfaceIsOnePixelHighAndFollowsEachModelFootprint() {
        Map<IroriGrillPart, AABB> expected = Map.of(
                IroriGrillPart.SINGLE, box(1, 4, 0, 15, 5, 16),
                IroriGrillPart.STRIP_NORTH, box(1, 4, 1, 15, 5, 16),
                IroriGrillPart.STRIP_SOUTH, box(1, 4, 0, 15, 5, 15),
                IroriGrillPart.STRIP_WEST, box(1, 4, 1, 16, 5, 15),
                IroriGrillPart.STRIP_EAST, box(0, 4, 1, 15, 5, 15),
                IroriGrillPart.QUAD_NORTH_WEST, box(2, 4, 1, 16, 5, 16),
                IroriGrillPart.QUAD_NORTH_EAST, box(0, 4, 1, 14, 5, 16),
                IroriGrillPart.QUAD_SOUTH_WEST, box(2, 4, 0, 16, 5, 15),
                IroriGrillPart.QUAD_SOUTH_EAST, box(0, 4, 0, 14, 5, 15)
        );

        for (Map.Entry<IroriGrillPart, AABB> entry : expected.entrySet()) {
            VoxelShape surface = IroriGrillVoxelShapes.upperSurface(entry.getKey());
            List<AABB> boxes = surface.toAabbs();
            assertEquals(entry.getValue(), surface.bounds(), entry.getKey().toString());
            assertTrue(boxes.size() > 1, entry.getKey().toString());
            assertTrue(boxes.stream().allMatch(box ->
                    box.minY == 4 * PIXEL && box.maxY == 5 * PIXEL
            ), entry.getKey().toString());
        }

        assertTrue(IroriGrillVoxelShapes.upperSurface(IroriGrillPart.SINGLE).toAabbs().contains(
                box(1.5, 4, 3, 14.5, 5, 13)
        ));
        assertTrue(IroriGrillVoxelShapes.upperSurface(IroriGrillPart.STRIP_NORTH).toAabbs().contains(
                box(1.5, 4, 4, 14.5, 5, 16)
        ));
    }

    @Test
    void everyGrillPartKeepsItsDetailedOutlineAndLowerShape() {
        for (IroriGrillPart part : IroriGrillPart.values()) {
            assertFalse(IroriGrillVoxelShapes.upperOutline(part).isEmpty());
            assertFalse(IroriGrillVoxelShapes.lower(part).isEmpty());
        }
    }

    private static AABB box(double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ) {
        return new AABB(
                minX * PIXEL, minY * PIXEL, minZ * PIXEL,
                maxX * PIXEL, maxY * PIXEL, maxZ * PIXEL
        );
    }
}
