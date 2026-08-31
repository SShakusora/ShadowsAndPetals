package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.CopperTeapotBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TeapotOutlineCacheTest {
    private static final double EPSILON = 1.0E-6D;
    private static final String MODEL_RESOURCE =
            "assets/shadowsandpetals/models/block/teapot/copper/main.json";

    @Test
    void mainModelProducesNonDegenerateGeometryIncludingRotatedSpout() throws IOException {
        JsonObject model = loadModel();

        assertEquals(16, model.getAsJsonArray("elements").size());
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(model);
        assertNotNull(geometry);
        assertFalse(geometry.lines().isEmpty());
        assertTrue(geometry.lines().stream().noneMatch(line ->
                line.from().distanceToSqr(line.to()) <= EPSILON * EPSILON
        ));
        assertTrue(geometry.lines().stream().anyMatch(line -> {
            Vec3 delta = line.to().subtract(line.from());
            return Math.abs(delta.y) > EPSILON && Math.abs(delta.z) > EPSILON;
        }), "the rotated spout should contribute a non-axis-aligned outline edge");
    }

    @Test
    void onIroriOutlinesFollowTheTranslatedBlockStateModel() throws IOException {
        OutlineGeometry base = RockeryOutlineGeometry.fromModel(loadModel());
        assertNotNull(base);
        Map<Boolean, Map<Direction, OutlineGeometry>> directions = TeapotOutlineCache.buildDirections(base);

        assertSame(base, directions.get(false).get(Direction.NORTH));
        assertGeometryClose(
                RockeryOutlineGeometry.translate(
                        base,
                        0.0D,
                        CopperTeapotBlock.IRORI_RENDER_OFFSET * 16.0D,
                        0.0D
                ),
                directions.get(true).get(Direction.NORTH)
        );
        assertGeometryClose(
                RockeryOutlineGeometry.translate(
                        directions.get(false).get(Direction.EAST),
                        0.0D,
                        CopperTeapotBlock.IRORI_RENDER_OFFSET * 16.0D,
                        0.0D
                ),
                directions.get(true).get(Direction.EAST)
        );
    }

    @Test
    void horizontalFacingUsesFourQuarterTurns() throws IOException {
        OutlineGeometry base = RockeryOutlineGeometry.fromModel(loadModel());
        assertNotNull(base);
        Map<Boolean, Map<Direction, OutlineGeometry>> directions = TeapotOutlineCache.buildDirections(base);

        assertEquals(
                RockeryOutlineGeometry.rotateClockwise(base).lines(),
                directions.get(false).get(Direction.EAST).lines()
        );
        assertEquals(
                RockeryOutlineGeometry.rotateClockwise(directions.get(false).get(Direction.EAST)).lines(),
                directions.get(false).get(Direction.SOUTH).lines()
        );

        OutlineGeometry rotated = base;
        for (int turn = 0; turn < 4; turn++) {
            rotated = RockeryOutlineGeometry.rotateClockwise(rotated);
        }
        assertGeometryClose(base, rotated);
    }

    private static JsonObject loadModel() throws IOException {
        try (InputStream stream = TeapotOutlineCacheTest.class.getClassLoader()
                .getResourceAsStream(MODEL_RESOURCE)) {
            assertNotNull(stream, MODEL_RESOURCE);
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
