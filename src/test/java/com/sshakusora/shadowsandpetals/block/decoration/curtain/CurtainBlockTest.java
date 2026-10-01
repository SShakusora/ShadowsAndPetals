package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurtainBlockTest {
    private static final double PIXEL = 1.0D / 16.0D;

    @Test
    void longCurtainCollisionBoundsFollowEachHalfAndOpenPoseEnvelope() {
        List<ShapeCase> cases = List.of(
                new ShapeCase(LongCurtainGeometry.closed(false), box(0, 3, 13, 16, 16, 16)),
                new ShapeCase(LongCurtainGeometry.closed(true), box(0, 0, 13, 16, 16, 16)),
                new ShapeCase(LongCurtainGeometry.open(false, false), box(0, 3, 12, 4, 16, 17)),
                new ShapeCase(LongCurtainGeometry.open(false, true), box(12, 3, 12, 16, 16, 17)),
                new ShapeCase(LongCurtainGeometry.open(true, false), box(0, 0, 12, 4, 16, 17)),
                new ShapeCase(LongCurtainGeometry.open(true, true), box(12, 0, 12, 16, 16, 17))
        );

        for (ShapeCase testCase : cases) {
            LongCurtainGeometry.CollisionBox actual = testCase.actual();
            assertEquals(testCase.expectedBounds(), box(
                    actual.minX(), actual.minY(), actual.minZ(),
                    actual.maxX(), actual.maxY(), actual.maxZ()
            ), testCase.toString());
        }
    }

    @Test
    void oneCellCurtainUsesTheUpperPanelEnvelope() {
        assertEquals(box(0, 0, 13, 16, 16, 16), toAabb(CurtainGeometry.closed()));
        assertEquals(box(0, 0, 12, 4, 16, 17), toAabb(CurtainGeometry.open(false)));
        assertEquals(box(12, 0, 12, 16, 16, 17), toAabb(CurtainGeometry.open(true)));
    }

    private static AABB toAabb(CurtainGeometry.CollisionBox box) {
        return box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    private static AABB box(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        return new AABB(
                minX * PIXEL,
                minY * PIXEL,
                minZ * PIXEL,
                maxX * PIXEL,
                maxY * PIXEL,
                maxZ * PIXEL
        );
    }

    private record ShapeCase(LongCurtainGeometry.CollisionBox actual, AABB expectedBounds) {
    }
}
