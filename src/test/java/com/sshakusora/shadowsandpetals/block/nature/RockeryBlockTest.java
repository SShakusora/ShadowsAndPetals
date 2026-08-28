package com.sshakusora.shadowsandpetals.block.nature;

import com.google.gson.JsonParser;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RockeryBlockTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void outlineUsesGeneratedPartModelAndRotatesWithFacing() {
        OutlineGeometry south = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [1, 2, 3],
                      "to": [5, 6, 7],
                      "faces": {"north": {}}
                    }
                  ]
                }
                """).getAsJsonObject());
        OutlineGeometry west = RockeryOutlineGeometry.rotateClockwise(south);
        assertNotNull(south);
        assertNotNull(west);
        assertTrue(south.lines().size() > 0);
        assertEquals(south.lines().size(), west.lines().size());

        for (int index = 0; index < south.lines().size(); index++) {
            OutlineGeometry.Line source = south.lines().get(index);
            OutlineGeometry.Line rotated = west.lines().get(index);
            assertClockwiseRotation(source.from(), rotated.from());
            assertClockwiseRotation(source.to(), rotated.to());
        }
    }

    @Test
    void outlineUsesOnlyDeclaredFacesAndDeduplicatesUndirectedEdges() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [4, 4, 4],
                      "faces": {
                        "north": {},
                        "west": {}
                      }
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        // Two adjacent quads have eight edge occurrences and one shared edge.
        assertEquals(7, geometry.lines().size());
    }

    @Test
    void outlineAppliesEulerXyzElementRotation() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [1, 2, 3],
                      "to": [5, 6, 7],
                      "rotation": {
                        "origin": [0, 0, 0],
                        "x": 90,
                        "y": 0,
                        "z": 90
                      },
                      "faces": {"north": {}}
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        OutlineGeometry.Line firstEdge = geometry.lines().getFirst();
        assertVec3(new Vec3(3.0D, 1.0D, 2.0D), firstEdge.from());
        assertVec3(new Vec3(3.0D, 1.0D, 6.0D), firstEdge.to());
    }

    @Test
    void everyGeneratedPartModelProducesOutlineGeometry() throws IOException {
        List<String> models = List.of(
                "1x1x1/0_0_0",
                "1x1x2/0_0_0",
                "1x1x2/0_0_1",
                "1x2x1/0_0_0",
                "1x2x1/0_1_0",
                "1x2x2/0_0_0",
                "1x2x2/0_0_1",
                "1x2x2/0_1_0",
                "1x2x2/0_1_1",
                "1x3x1/0_0_0",
                "1x3x1/0_1_0",
                "1x3x1/0_2_0"
        );

        ClassLoader classLoader = RockeryBlockTest.class.getClassLoader();
        for (String model : models) {
            String resource = "assets/shadowsandpetals/models/block/rock/" + model + ".json";
            try (InputStream stream = classLoader.getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(
                        JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject()
                );
                assertNotNull(geometry, resource);
                assertTrue(!geometry.lines().isEmpty(), resource);
            }
        }
    }

    private static void assertClockwiseRotation(Vec3 source, Vec3 rotated) {
        assertEquals(16.0D - source.z, rotated.x, EPSILON);
        assertEquals(source.y, rotated.y, EPSILON);
        assertEquals(source.x, rotated.z, EPSILON);
    }

    private static void assertVec3(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }
}
