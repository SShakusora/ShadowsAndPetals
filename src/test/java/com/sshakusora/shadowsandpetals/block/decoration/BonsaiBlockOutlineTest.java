package com.sshakusora.shadowsandpetals.block.decoration;

import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiModelTransform;
import com.sshakusora.shadowsandpetals.client.outline.BonsaiOutlineGeometry;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BonsaiBlockOutlineTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void everyRotationUsesAllPotModelEdges() {
        for (int rotation = 0; rotation < 16; rotation++) {
            OutlineGeometry geometry = BonsaiOutlineGeometry.forRotation(rotation);
            assertEquals(7 * 12, geometry.lines().size(), "rotation " + rotation);
            assertTrue(geometry.lines().stream().noneMatch(line ->
                    line.from().distanceToSqr(line.to()) <= EPSILON * EPSILON
            ), "rotation " + rotation);
        }
    }

    @Test
    void cardinalRotationsMatchThePotModelBounds() {
        assertBounds(BonsaiOutlineGeometry.forRotation(0), 2.0D, 0.0D, 3.0D, 14.0D, 7.0D, 13.0D);
        assertBounds(BonsaiOutlineGeometry.forRotation(4), 3.0D, 0.0D, 2.0D, 13.0D, 7.0D, 14.0D);
        assertBounds(BonsaiOutlineGeometry.forRotation(8), 2.0D, 0.0D, 3.0D, 14.0D, 7.0D, 13.0D);
        assertBounds(BonsaiOutlineGeometry.forRotation(12), 3.0D, 0.0D, 2.0D, 13.0D, 7.0D, 14.0D);
    }

    @Test
    void diagonalRotationIsNotReducedToAnAxisAlignedQuarterTurn() {
        Bounds diagonal = Bounds.of(BonsaiOutlineGeometry.forRotation(1));
        Bounds quarterTurn = Bounds.of(BonsaiOutlineGeometry.forRotation(0));

        assertTrue(diagonal.minX() < quarterTurn.minX());
        assertTrue(diagonal.maxX() > quarterTurn.maxX());
        assertTrue(diagonal.minZ() < quarterTurn.minZ());
        assertTrue(diagonal.maxZ() > quarterTurn.maxZ());
    }

    @Test
    void rotationKeepsTheBlockCentreFixedAndCachesWrappedSegments() {
        Vec3 centre = new Vec3(8.0D, 4.0D, 8.0D);
        for (int rotation = 0; rotation < 16; rotation++) {
            assertEquals(centre, BonsaiModelTransform.transformModelPoint(centre, rotation));
        }
        assertSame(
                BonsaiOutlineGeometry.forRotation(0),
                BonsaiOutlineGeometry.forRotation(16)
        );
    }

    @Test
    void outlineTransformMatchesTheChunkModelMatrix() {
        Vec3 source = new Vec3(6.0D, 2.0D, 11.0D);
        for (int rotation = 0; rotation < 16; rotation++) {
            Vector3f expected = new Vector3f(
                    (float) (source.x / 16.0D),
                    (float) (source.y / 16.0D),
                    (float) (source.z / 16.0D)
            );
            BonsaiModelTransform.aroundBlockCenter(rotation).transformPosition(expected);

            Vec3 actual = BonsaiModelTransform.transformModelPoint(source, rotation);
            assertEquals(expected.x * 16.0F, actual.x, 1.0E-5D, "x, rotation " + rotation);
            assertEquals(expected.y * 16.0F, actual.y, 1.0E-5D, "y, rotation " + rotation);
            assertEquals(expected.z * 16.0F, actual.z, 1.0E-5D, "z, rotation " + rotation);
        }
    }

    private static void assertBounds(
            OutlineGeometry geometry,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        Bounds actual = Bounds.of(geometry);
        assertEquals(minX, actual.minX(), EPSILON);
        assertEquals(minY, actual.minY(), EPSILON);
        assertEquals(minZ, actual.minZ(), EPSILON);
        assertEquals(maxX, actual.maxX(), EPSILON);
        assertEquals(maxY, actual.maxY(), EPSILON);
        assertEquals(maxZ, actual.maxZ(), EPSILON);
    }

    private record Bounds(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        private static Bounds of(OutlineGeometry geometry) {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (OutlineGeometry.Line line : geometry.lines()) {
                for (Vec3 point : new Vec3[]{line.from(), line.to()}) {
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    minZ = Math.min(minZ, point.z);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                    maxZ = Math.max(maxZ, point.z);
                }
            }
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}