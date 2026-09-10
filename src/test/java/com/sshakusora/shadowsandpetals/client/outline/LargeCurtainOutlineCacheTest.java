package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LargeCurtainOutlineCacheTest {
    private static final double EPSILON = 1.0E-6D;
    private static final List<String> NON_EMPTY_MODELS = List.of(
            "right/closed/white/lower_outer",
            "right/closed/white/lower_inner",
            "right/closed/white/upper_outer",
            "right/closed/white/upper_inner",
            "left/closed/white/lower_outer",
            "left/closed/white/lower_inner",
            "left/closed/white/upper_outer",
            "left/closed/white/upper_inner",
            "right/open/white/lower_inner",
            "right/open/white/upper_outer",
            "right/open/white/upper_inner",
            "left/open/white/lower_inner",
            "left/open/white/upper_outer",
            "left/open/white/upper_inner"
    );

    @Test
    void everyPoseReferencesAnExistingOutlineModel() {
        for (LargeCurtainOutlineCache.Pose pose : LargeCurtainOutlineCache.Pose.values()) {
            Identifier modelId = LargeCurtainOutlineCache.outlineModelId(pose);
            String resourceName = "assets/" + modelId.getNamespace() + "/" + modelId.getPath();
            try (InputStream stream = LargeCurtainOutlineCacheTest.class.getClassLoader()
                    .getResourceAsStream(resourceName)) {
                assertNotNull(stream, modelId.toString());
            } catch (IOException exception) {
                fail("Failed to close " + modelId, exception);
            }
        }
    }

    @Test
    void everyVisibleQuadrantProducesNonDegenerateGeometry() throws IOException {
        for (String modelName : NON_EMPTY_MODELS) {
            String resourceName = "assets/shadowsandpetals/models/block/large_curtain/static/"
                    + modelName + ".json";
            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(loadModel(resourceName));
            assertNotNull(geometry, resourceName);
            assertFalse(geometry.lines().isEmpty(), resourceName);
            assertTrue(geometry.lines().stream().noneMatch(line ->
                    line.from().distanceToSqr(line.to()) <= EPSILON * EPSILON
            ), resourceName);
        }
    }

    @Test
    void openLowerOuterQuadrantsAreIntentionallyEmpty() throws IOException {
        for (String side : List.of("right", "left")) {
            JsonObject model = loadModel(
                    "assets/shadowsandpetals/models/block/large_curtain/static/"
                            + side + "/open/white/lower_outer.json"
            );
            assertNull(RockeryOutlineGeometry.fromModel(model), side);
        }
    }

    @Test
    void eachVisibleQuadrantSupportsAllHorizontalFacings() throws IOException {
        for (String modelName : NON_EMPTY_MODELS) {
            OutlineGeometry base = RockeryOutlineGeometry.fromModel(loadModel(
                    "assets/shadowsandpetals/models/block/large_curtain/static/" + modelName + ".json"
            ));
            assertNotNull(base, modelName);

            Map<Direction, OutlineGeometry> directions = LargeCurtainOutlineCache.buildDirections(base);
            assertSame(base, directions.get(Direction.NORTH), modelName);
            assertEquals(4, directions.size(), modelName);

            OutlineGeometry rotated = base;
            for (int turn = 0; turn < 4; turn++) {
                rotated = RockeryOutlineGeometry.rotateClockwise(rotated);
            }
            assertGeometryClose(base, rotated);
        }
    }

    private static JsonObject loadModel(String resourceName) throws IOException {
        try (InputStream stream = LargeCurtainOutlineCacheTest.class.getClassLoader()
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
