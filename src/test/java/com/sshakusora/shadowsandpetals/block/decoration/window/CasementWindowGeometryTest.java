package com.sshakusora.shadowsandpetals.block.decoration.window;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CasementWindowGeometryTest {
    @Test
    void partnerPositionsAreMutualForEachFacingAndSide() {
        BlockPos origin = new BlockPos(12, 64, -7);

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (CasementWindowSide side : CasementWindowSide.values()) {
                BlockPos partner = CasementWindowGeometry.partnerPosition(origin, facing, side);
                assertEquals(
                        origin,
                        CasementWindowGeometry.partnerPosition(partner, facing, side.mirror())
                );
            }
        }
    }

    @Test
    void openShapesExistForEachFacing() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            assertFalse(CasementWindowGeometry.shape(facing, CasementWindowSide.LEFT, false).isEmpty());
            assertFalse(CasementWindowGeometry.shape(facing, CasementWindowSide.RIGHT, false).isEmpty());
            assertFalse(CasementWindowGeometry.shape(facing, CasementWindowSide.LEFT, true).isEmpty());
            assertFalse(CasementWindowGeometry.shape(facing, CasementWindowSide.RIGHT, true).isEmpty());
        }

        assertEquals(90.0F, CasementWindowGeometry.targetAngle(CasementWindowSide.LEFT));
        assertEquals(-90.0F, CasementWindowGeometry.targetAngle(CasementWindowSide.RIGHT));
    }

    @Test
    void partnerDirectionsFollowFacing() {
        assertEquals(Direction.WEST,
                CasementWindowGeometry.partnerDirection(Direction.NORTH, CasementWindowSide.LEFT));
        assertEquals(Direction.EAST,
                CasementWindowGeometry.partnerDirection(Direction.NORTH, CasementWindowSide.RIGHT));
        assertEquals(Direction.NORTH,
                CasementWindowGeometry.partnerDirection(Direction.EAST, CasementWindowSide.LEFT));
        assertEquals(Direction.SOUTH,
                CasementWindowGeometry.partnerDirection(Direction.EAST, CasementWindowSide.RIGHT));
        assertEquals(Direction.EAST,
                CasementWindowGeometry.partnerDirection(Direction.SOUTH, CasementWindowSide.LEFT));
        assertEquals(Direction.WEST,
                CasementWindowGeometry.partnerDirection(Direction.SOUTH, CasementWindowSide.RIGHT));
        assertEquals(Direction.SOUTH,
                CasementWindowGeometry.partnerDirection(Direction.WEST, CasementWindowSide.LEFT));
        assertEquals(Direction.NORTH,
                CasementWindowGeometry.partnerDirection(Direction.WEST, CasementWindowSide.RIGHT));
    }
}
