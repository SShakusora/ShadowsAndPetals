package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VanityOutlineCacheTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void bothVanityHalfModelsProduceNonDegenerateModelOutlines() throws IOException {
        Map<String, Integer> expectedElementCounts = Map.of(
                "vanity_lower", 46,
                "vanity_upper", 21
        );

        for (Map.Entry<String, Integer> entry : expectedElementCounts.entrySet()) {
            String resourceName = "assets/shadowsandpetals/models/block/vanity/" + entry.getKey() + ".json";
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
    void eachHalfUsesTheMatchingModelAndRotatesWithFacing() throws IOException {
        OutlineGeometry lower = load("vanity_lower");
        OutlineGeometry upper = load("vanity_upper");
        Map<DoubleBlockHalf, Map<Direction, OutlineGeometry>> directions =
                VanityOutlineCache.buildDirections(lower, upper);

        assertSame(lower, directions.get(DoubleBlockHalf.LOWER).get(Direction.NORTH));
        assertSame(upper, directions.get(DoubleBlockHalf.UPPER).get(Direction.NORTH));
        assertEquals(
                RockeryOutlineGeometry.rotateClockwise(lower).lines(),
                directions.get(DoubleBlockHalf.LOWER).get(Direction.EAST).lines()
        );
        assertEquals(
                RockeryOutlineGeometry.rotateClockwise(upper).lines(),
                directions.get(DoubleBlockHalf.UPPER).get(Direction.EAST).lines()
        );
    }

    @Test
    void fourHorizontalTurnsReturnToTheOriginalModelGeometry() throws IOException {
        for (OutlineGeometry base : new OutlineGeometry[]{load("vanity_lower"), load("vanity_upper")}) {
            OutlineGeometry rotated = base;
            for (int turn = 0; turn < 4; turn++) {
                rotated = RockeryOutlineGeometry.rotateClockwise(rotated);
            }
            assertGeometryClose(base, rotated);
        }
    }

    private static OutlineGeometry load(String name) throws IOException {
        String resourceName = "assets/shadowsandpetals/models/block/vanity/" + name + ".json";
        try (InputStream stream = VanityOutlineCacheTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(stream, resourceName);
            return RockeryOutlineGeometry.fromModel(JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject());
        }
    }

    private static void assertGeometryClose(OutlineGeometry expected, OutlineGeometry actual) {
        assertEquals(expected.lines().size(), actual.lines().size());
        for (int index = 0; index < expected.lines().size(); index++) {
            assertPointClose(expected.lines().get(index).from(), actual.lines().get(index).from());
            assertPointClose(expected.lines().get(index).to(), actual.lines().get(index).to());
        }
    }

    private static void assertPointClose(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }
}
