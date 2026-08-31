package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LampOutlineCacheTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void everyLampOffModelProducesVisibleNonDegenerateGeometry() throws IOException {
        Map<String, Integer> expectedElementCounts = Map.of(
                "bedroom_lamp", 17,
                "wall_lamp", 19,
                "emergency_lamp", 23,
                "desk_lamp", 9
        );
        for (Map.Entry<String, Integer> entry : expectedElementCounts.entrySet()) {
            String name = entry.getKey();
            String resourceName = "assets/shadowsandpetals/models/block/" + name + "/off.json";
            JsonObject model;
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                assertNotNull(stream, resourceName);
                model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            }

            assertEquals(entry.getValue(), model.getAsJsonArray("elements").size(), resourceName);
            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(model);
            assertNotNull(geometry, resourceName);
            assertFalse(geometry.lines().isEmpty(), resourceName);
            assertTrue(geometry.lines().stream().noneMatch(line ->
                    line.from().distanceToSqr(line.to()) <= EPSILON * EPSILON
            ), resourceName);
        }
    }

    @Test
    void horizontalRotationReturnsToTheOriginalGeometryAfterFourTurns() {
        OutlineGeometry original = OutlineGeometry.box(2.0D, 3.0D, 4.0D, 11.0D, 13.0D, 15.0D);
        OutlineGeometry rotated = original;
        for (int turn = 0; turn < 4; turn++) {
            rotated = LampOutlineCache.transform(rotated, LampOutlineCache::rotateClockwise);
        }

        assertEquals(original.lines(), rotated.lines());
    }

    @Test
    void directionalRotationsPreserveTheExpectedInstallationBounds() {
        OutlineGeometry source = OutlineGeometry.box(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D);
        Map<Direction, Bounds> expected = new EnumMap<>(Direction.class);
        expected.put(Direction.UP, new Bounds(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D));
        expected.put(Direction.DOWN, new Bounds(4.0D, 4.0D, 4.0D, 12.0D, 16.0D, 12.0D));
        expected.put(Direction.NORTH, new Bounds(4.0D, 4.0D, 4.0D, 12.0D, 12.0D, 16.0D));
        expected.put(Direction.EAST, new Bounds(0.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D));
        expected.put(Direction.SOUTH, new Bounds(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 12.0D));
        expected.put(Direction.WEST, new Bounds(4.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D));

        for (Map.Entry<Direction, Bounds> entry : expected.entrySet()) {
            OutlineGeometry transformed = LampOutlineCache.transform(
                    source,
                    point -> LampOutlineCache.transformPoint(point, entry.getKey())
            );
            assertEquals(entry.getValue(), Bounds.of(transformed), entry.getKey().toString());
        }
    }

    @Test
    void fixedLampDirectionLookupReturnsTheCachedGeometryIdentity() {
        OutlineGeometry cached = OutlineGeometry.box(3.5D, 0.0D, 3.5D, 12.5D, 13.0D, 12.5D);
        Map<Direction, OutlineGeometry> fixedCache = Map.of(Direction.UP, cached);

        assertSame(cached, LampOutlineCache.selectDirection(fixedCache, Direction.UP));
        assertSame(cached, LampOutlineCache.selectDirection(fixedCache, Direction.UP));
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
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

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Bounds bounds)) {
                return false;
            }
            return close(minX, bounds.minX)
                    && close(minY, bounds.minY)
                    && close(minZ, bounds.minZ)
                    && close(maxX, bounds.maxX)
                    && close(maxY, bounds.maxY)
                    && close(maxZ, bounds.maxZ);
        }

        private static boolean close(double first, double second) {
            return Math.abs(first - second) <= EPSILON;
        }
    }
}
