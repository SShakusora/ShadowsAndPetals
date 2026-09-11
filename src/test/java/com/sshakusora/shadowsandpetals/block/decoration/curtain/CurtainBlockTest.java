package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurtainBlockTest {
    private static final double PIXEL = 1.0D / 16.0D;

    @Test
    void collisionBoundsFollowEachHalfAndOpenPoseEnvelope() {
        List<ShapeCase> cases = List.of(
                new ShapeCase(CurtainGeometry.closed(false), box(0, 3, 13, 16, 16, 16)),
                new ShapeCase(CurtainGeometry.closed(true), box(0, 0, 13, 16, 16, 16)),
                new ShapeCase(CurtainGeometry.open(false, false), box(0, 3, 12, 4, 16, 17)),
                new ShapeCase(CurtainGeometry.open(false, true), box(12, 3, 12, 16, 16, 17)),
                new ShapeCase(CurtainGeometry.open(true, false), box(0, 0, 12, 4, 16, 17)),
                new ShapeCase(CurtainGeometry.open(true, true), box(12, 0, 12, 16, 16, 17))
        );

        for (ShapeCase testCase : cases) {
            CurtainGeometry.CollisionBox actual = testCase.actual();
            assertEquals(testCase.expectedBounds(), box(
                    actual.minX(), actual.minY(), actual.minZ(),
                    actual.maxX(), actual.maxY(), actual.maxZ()
            ), testCase.toString());
        }
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

    private record ShapeCase(CurtainGeometry.CollisionBox actual, AABB expectedBounds) {
    }
}
