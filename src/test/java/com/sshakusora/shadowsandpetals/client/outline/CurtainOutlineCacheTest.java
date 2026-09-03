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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CurtainOutlineCacheTest {
    private static final double EPSILON = 1.0E-6D;
    private static final List<String> MODEL_NAMES = List.of(
            "curtain_upper_right",
            "curtain_upper_right_open",
            "curtain_upper_left",
            "curtain_upper_left_open",
            "curtain_lower_right",
            "curtain_lower_right_open",
            "curtain_lower_left",
            "curtain_lower_left_open"
    );

    @Test
    void everyStaticCurtainMasterProducesVisibleNonDegenerateGeometry() throws IOException {
        for (String modelName : MODEL_NAMES) {
            String resourceName = "assets/shadowsandpetals/models/block/curtain/" + modelName + ".json";
            JsonObject model = loadModel(resourceName);
            assertFalse(model.getAsJsonArray("elements").isEmpty(), resourceName);

            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(model);
            assertNotNull(geometry, resourceName);
            assertFalse(geometry.lines().isEmpty(), resourceName);
            assertTrue(geometry.lines().stream().noneMatch(line ->
                    line.from().distanceToSqr(line.to()) <= EPSILON * EPSILON
            ), resourceName);
        }
    }

    @Test
    void eachStaticCurtainMasterSupportsAllHorizontalFacings() throws IOException {
        for (String modelName : MODEL_NAMES) {
            OutlineGeometry base = RockeryOutlineGeometry.fromModel(loadModel(
                    "assets/shadowsandpetals/models/block/curtain/" + modelName + ".json"
            ));
            assertNotNull(base, modelName);

            Map<Direction, OutlineGeometry> directions = CurtainOutlineCache.buildDirections(base);
            assertSame(base, directions.get(Direction.NORTH), modelName);

            OutlineGeometry rotated = base;
            for (int turn = 0; turn < 4; turn++) {
                rotated = RockeryOutlineGeometry.rotateClockwise(rotated);
            }
            assertGeometryClose(base, rotated);
            assertEquals(4, directions.size(), modelName);
        }
    }

    private static JsonObject loadModel(String resourceName) throws IOException {
        try (InputStream stream = CurtainOutlineCacheTest.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            assertNotNull(stream, resourceName);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
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
