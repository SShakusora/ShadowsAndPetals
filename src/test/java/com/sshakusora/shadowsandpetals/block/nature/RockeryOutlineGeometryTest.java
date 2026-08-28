package com.sshakusora.shadowsandpetals.block.nature;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RockeryOutlineGeometryTest {
    private static final double EPSILON = 1.0E-4D;

    @Test
    void removesEdgesThatLieInTheInteriorOfAnotherCuboidFace() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [10, 10, 10],
                      "faces": {"north": {}}
                    },
                    {
                      "from": [2, 2, 0],
                      "to": [8, 4, 2],
                      "faces": {"north": {}}
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        // The smaller cuboid is completely hidden in the interior of the
        // larger cuboid's north face. Only the larger face perimeter remains.
        assertEquals(4, geometry.lines().size());
        assertTrue(geometry.lines().stream().allMatch(line ->
                Math.abs(line.from().z) <= EPSILON && Math.abs(line.to().z) <= EPSILON
        ));
    }

    @Test
    void extractsOnlyTheTwelveEdgesOfAnUnrotatedCuboid() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [10, 10, 10],
                      "faces": {
                        "down": {}, "up": {}, "north": {},
                        "south": {}, "west": {}, "east": {}
                      }
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        assertEquals(12, geometry.lines().size());
        assertTrue(geometry.lines().stream().noneMatch(line ->
                line.from().distanceToSqr(line.to()) <= EPSILON * EPSILON
        ));
    }

    @Test
    void splitsAnEdgeWhenOnlyPartOfItIsCovered() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [10, 10, 10],
                      "faces": {"north": {}}
                    },
                    {
                      "from": [5, 2, 0],
                      "to": [15, 4, 2],
                      "faces": {"north": {}}
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        assertTrue(geometry.lines().stream().anyMatch(line ->
                isPoint(line.from(), 10.0D, 2.0D, 0.0D) && isPoint(line.to(), 15.0D, 2.0D, 0.0D)
                        || isPoint(line.to(), 10.0D, 2.0D, 0.0D) && isPoint(line.from(), 15.0D, 2.0D, 0.0D)
        ));
        assertTrue(geometry.lines().stream().noneMatch(line ->
                isPoint(line.from(), 5.0D, 2.0D, 0.0D) && isPoint(line.to(), 10.0D, 2.0D, 0.0D)
                        || isPoint(line.to(), 5.0D, 2.0D, 0.0D) && isPoint(line.from(), 10.0D, 2.0D, 0.0D)
        ));
    }

    @Test
    void appliesCoverageTestInTheRotatedCuboidLocalSpace() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [10, 10, 10],
                      "rotation": {"origin": [0, 0, 0], "axis": "y", "angle": 30},
                      "faces": {"north": {}}
                    },
                    {
                      "from": [2, 2, 0],
                      "to": [8, 4, 2],
                      "rotation": {"origin": [0, 0, 0], "axis": "y", "angle": 30},
                      "faces": {"north": {}}
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        assertEquals(4, geometry.lines().size());
    }

    @Test
    void removesTheSeamBetweenCoplanarAdjacentFaces() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [5, 5, 5],
                      "faces": {"north": {}}
                    },
                    {
                      "from": [5, 0, 0],
                      "to": [10, 5, 5],
                      "faces": {"north": {}}
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        // The two rectangles form one flat face. Their shared vertical edge
        // at x=5 is a face seam, not an edge of the union.
        assertTrue(geometry.lines().stream().noneMatch(line ->
                isVerticalSeam(line, 5.0D, 0.0D, 5.0D)
        ));
    }

    @Test
    void splitsASeamWhenTheAdjacentFaceEndsBeforeTheCandidateEdge() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [5, 10, 5],
                      "faces": {"north": {}}
                    },
                    {
                      "from": [5, 0, 0],
                      "to": [10, 5, 5],
                      "faces": {"north": {}}
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        // The shared edge is a smooth-face seam for y=0..5, but becomes a
        // genuine outer edge for y=5..10 after the shorter cuboid ends.
        assertTrue(geometry.lines().stream().noneMatch(line ->
                isVerticalSegment(line, 5.0D, 0.0D, 0.0D, 5.0D)
        ));
        assertTrue(geometry.lines().stream().anyMatch(line ->
                isVerticalSegment(line, 5.0D, 0.0D, 5.0D, 10.0D)
        ));
    }

    @Test
    void createsTheIntersectionEdgeOfTwoNonCoplanarFaces() {
        OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [10, 10, 10],
                      "faces": {"north": {}}
                    },
                    {
                      "from": [2, 2, -2],
                      "to": [8, 8, 2],
                      "faces": {"east": {}}
                    }
                  ]
                }
                """).getAsJsonObject());

        assertNotNull(geometry);
        // x=8,z=0,y=2..8 is the intersection of the first cuboid's north
        // face and the second cuboid's east face. It is not an original edge
        // of either cuboid and must be generated by face clipping.
        assertTrue(geometry.lines().stream().anyMatch(line ->
                isSegment(line, new Vec3(8.0D, 2.0D, 0.0D), new Vec3(8.0D, 8.0D, 0.0D))
        ));
    }

    @Test
    void combinesPartModelsBeforeFilteringOverlappingEdges() {
        JsonObject part0 = JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [10, 10, 16],
                      "faces": {"north": {}, "south": {}, "east": {}, "west": {}, "up": {}, "down": {}}
                    }
                  ]
                }
                """).getAsJsonObject();
        JsonObject part1 = JsonParser.parseString("""
                {
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [10, 10, 16],
                      "faces": {"north": {}, "south": {}, "east": {}, "west": {}, "up": {}, "down": {}}
                    }
                  ]
                }
                """).getAsJsonObject();

        OutlineGeometry separate0 = RockeryOutlineGeometry.fromModel(part0);
        OutlineGeometry separate1 = RockeryOutlineGeometry.fromModel(part1);
        OutlineGeometry combined = RockeryOutlineGeometry.fromModels(List.of(
                new RockeryOutlineGeometry.ModelPart(part0, Vec3.ZERO),
                new RockeryOutlineGeometry.ModelPart(part1, new Vec3(0.0D, 0.0D, 16.0D))
        ));

        assertNotNull(separate0);
        assertNotNull(separate1);
        assertNotNull(combined);
        assertTrue(combined.lines().size() < separate0.lines().size() + separate1.lines().size());
    }

    @Test
    void filtersEveryGeneratedRockeryStructure() throws IOException {
        List<RockeryDimensions> dimensions = List.of(
                new RockeryDimensions(1, 1, 1),
                new RockeryDimensions(1, 1, 2),
                new RockeryDimensions(1, 2, 1),
                new RockeryDimensions(1, 2, 2),
                new RockeryDimensions(1, 3, 1)
        );
        ClassLoader classLoader = RockeryOutlineGeometryTest.class.getClassLoader();

        for (RockeryDimensions dimension : dimensions) {
            List<RockeryOutlineGeometry.ModelPart> parts = new java.util.ArrayList<>();
            int separateLineCount = 0;
            for (int part = 0; part < dimension.partCount(); part++) {
                String resource = "assets/shadowsandpetals/models/" + dimension.modelPath(part) + ".json";
                JsonObject model;
                try (InputStream stream = classLoader.getResourceAsStream(resource)) {
                    assertNotNull(stream, resource);
                    model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                            .getAsJsonObject();
                }
                OutlineGeometry separate = RockeryOutlineGeometry.fromModel(model);
                assertNotNull(separate, resource);
                separateLineCount += separate.lines().size();
                Vec3i local = dimension.localPos(part);
                parts.add(new RockeryOutlineGeometry.ModelPart(
                        model,
                        new Vec3(local.getX() * 16.0D, local.getY() * 16.0D, local.getZ() * 16.0D)
                ));
            }

            OutlineGeometry complete = RockeryOutlineGeometry.fromModels(parts);
            assertNotNull(complete, dimension.displayName());
            assertTrue(complete.lines().size() <= separateLineCount, dimension.displayName());
            if (dimension.partCount() > 1) {
                assertTrue(complete.lines().size() < separateLineCount, dimension.displayName());
            }
        }
    }

    private static boolean isPoint(Vec3 actual, double x, double y, double z) {
        return Math.abs(actual.x - x) <= EPSILON
                && Math.abs(actual.y - y) <= EPSILON
                && Math.abs(actual.z - z) <= EPSILON;
    }

    private static boolean isSegment(OutlineGeometry.Line line, Vec3 first, Vec3 second) {
        return isPoint(line.from(), first.x, first.y, first.z)
                && isPoint(line.to(), second.x, second.y, second.z)
                || isPoint(line.from(), second.x, second.y, second.z)
                && isPoint(line.to(), first.x, first.y, first.z);
    }

    private static boolean isVerticalSeam(OutlineGeometry.Line line, double x, double z, double yLength) {
        return isVerticalSegment(line, x, z, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
                && Math.abs(Math.abs(line.to().y - line.from().y) - yLength) <= EPSILON;
    }

    private static boolean isVerticalSegment(OutlineGeometry.Line line, double x, double z, double minY, double maxY) {
        return Math.abs(line.from().x - x) <= EPSILON
                && Math.abs(line.to().x - x) <= EPSILON
                && Math.abs(line.from().x - line.to().x) <= EPSILON
                && Math.abs(line.from().z - z) <= EPSILON
                && Math.abs(line.to().z - z) <= EPSILON
                && Math.abs(line.from().z - line.to().z) <= EPSILON
                && Math.min(line.from().y, line.to().y) >= minY - EPSILON
                && Math.max(line.from().y, line.to().y) <= maxY + EPSILON;
    }
}
