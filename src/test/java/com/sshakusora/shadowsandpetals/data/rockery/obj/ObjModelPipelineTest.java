package com.sshakusora.shadowsandpetals.data.rockery.obj;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sshakusora.shadowsandpetals.data.rockery.obj.ObjModel.*;
import static org.junit.jupiter.api.Assertions.*;

class ObjModelPipelineTest {
    private static final double EPSILON = 1.0E-6D;
    private static final Path SOURCE_DIRECTORY = Path.of(
            "src/main/resources/assets/shadowsandpetals/models/block/rock"
    );
    private static final Path GENERATED_DIRECTORY = Path.of(
            "src/generated/resources/assets/shadowsandpetals/models/block/rock"
    );

    @Test
    void parsesAllImportedRockeryObjFiles() throws IOException {
        List<SourceFixture> fixtures = List.of(
                new SourceFixture("1_1_1", 1, 1, 1, 39),
                new SourceFixture("1_2_1", 1, 2, 1, 44),
                new SourceFixture("1_3_1", 1, 3, 1, 83),
                new SourceFixture("1_1_2", 1, 1, 2, 30),
                new SourceFixture("1_2_2", 1, 2, 2, 62)
        );

        for (SourceFixture fixture : fixtures) {
            ObjModel model = ObjModelParser.parse(SOURCE_DIRECTORY.resolve(fixture.name() + ".obj"));
            assertEquals(fixture.faceCount(), model.faces().size(), fixture.name());
            assertEquals(Set.of(model.faces().getFirst().material()), model.materials(), fixture.name());
            assertFalse(model.objects().stream().allMatch(object -> object.positions().isEmpty()), fixture.name());
        }
    }

    @Test
    void extractsCuboidTopologyFromAllImportedRockeryObjFiles() throws IOException {
        Map<String, Integer> expectedCuboidCounts = Map.of(
                "1_1_1", 9,
                "1_2_1", 10,
                "1_3_1", 18,
                "1_1_2", 5,
                "1_2_2", 14
        );

        for (Map.Entry<String, Integer> fixture : expectedCuboidCounts.entrySet()) {
            ObjModel model = ObjModelParser.parse(SOURCE_DIRECTORY.resolve(fixture.getKey() + ".obj"));
            List<ObjModelOutline.Cuboid> cuboids = ObjModelOutline.extract(model);

            assertEquals(fixture.getValue(), cuboids.size(), fixture.getKey());
            assertTrue(cuboids.stream().allMatch(cuboid ->
                    cuboid.vertices().size() == 8
                            && cuboid.faces().size() >= 4
                            && cuboid.faces().size() <= 6
                            && cuboid.edges().size() == 12
            ), fixture.getKey());
        }
    }

    @Test
    void cutsImportedModelsIntoExpectedPartGrids() throws IOException {
        List<SourceFixture> fixtures = List.of(
                new SourceFixture("1_1_1", 1, 1, 1, 39),
                new SourceFixture("1_2_1", 1, 2, 1, 44),
                new SourceFixture("1_3_1", 1, 3, 1, 83),
                new SourceFixture("1_1_2", 1, 1, 2, 30),
                new SourceFixture("1_2_2", 1, 2, 2, 62)
        );

        for (SourceFixture fixture : fixtures) {
            ObjModel model = ObjModelParser.parse(SOURCE_DIRECTORY.resolve(fixture.name() + ".obj"));
            ObjModelCutter.CutModel cut = ObjModelCutter.cut(
                    model,
                    fixture.width(),
                    fixture.height(),
                    fixture.depth()
            );

            assertEquals(fixture.width() * fixture.height() * fixture.depth(), cut.parts().size(), fixture.name());
            for (ObjModelCutter.Part part : cut.parts()) {
                for (ObjFace face : part.faces()) {
                    assertTrue(face.vertices().size() <= 4, fixture.name());
                    assertTrue(face.vertices().size() >= 3, fixture.name());
                    for (ObjVertex vertex : face.vertices()) {
                        assertBetweenZeroAndOne(vertex.position().x(), fixture.name());
                        assertBetweenZeroAndOne(vertex.position().y(), fixture.name());
                        assertBetweenZeroAndOne(vertex.position().z(), fixture.name());
                    }
                }
                for (ObjModel.Bounds box : part.collisionBoxes()) {
                    assertBetweenZeroAndOne(box.minX(), fixture.name());
                    assertBetweenZeroAndOne(box.minY(), fixture.name());
                    assertBetweenZeroAndOne(box.minZ(), fixture.name());
                    assertBetweenZeroAndOne(box.maxX(), fixture.name());
                    assertBetweenZeroAndOne(box.maxY(), fixture.name());
                    assertBetweenZeroAndOne(box.maxZ(), fixture.name());
                }
            }
        }
    }

    @Test
    void alignsTheNegativeZImportedModelIntoTheStructureGrid() throws IOException {
        ObjModel model = ObjModelParser.parse(SOURCE_DIRECTORY.resolve("1_2_2.obj"));
        ObjModelCutter.CutModel cut = ObjModelCutter.cut(model, 1, 2, 2);

        assertEquals(0.0D, cut.alignment().x(), EPSILON);
        assertEquals(0.0D, cut.alignment().y(), EPSILON);
        assertEquals(1.0D, cut.alignment().z(), EPSILON);
        assertTrue(cut.normalizedBounds().minZ() >= -EPSILON);
        assertTrue(cut.normalizedBounds().maxZ() <= 2.0D + EPSILON);
    }

    @Test
    void clipsCrossingFacesAndTriangulatesTheResult() {
        List<ObjVector3> positions = List.of(
                new ObjVector3(-0.25D, 0.25D, 0.0D),
                new ObjVector3(1.25D, 0.25D, 0.0D),
                new ObjVector3(1.25D, 0.75D, 0.0D),
                new ObjVector3(-0.25D, 0.75D, 0.0D)
        );
        ObjFace face = new ObjFace(
                0,
                "cube",
                "rockery",
                positions.stream().map(position -> new ObjVertex(position, null, null)).toList()
        );
        ObjModel source = new ObjModel(
                List.of(face),
                List.of(new ObjObject(0, "cube", positions)),
                Set.of("rockery")
        );

        ObjModelCutter.CutModel cut = ObjModelCutter.cut(source, 2, 1, 1);

        assertEquals(2, cut.parts().size());
        assertEquals(2, cut.parts().get(0).faces().size());
        assertEquals(2, cut.parts().get(1).faces().size());
    }

    @Test
    void generatedObjPartsCanBeParsedAgain() throws IOException {
        for (String directory : List.of("1x1x1", "1x2x1", "1x3x1", "1x1x2", "1x2x2")) {
            Path partDirectory = GENERATED_DIRECTORY.resolve(directory);
            try (var files = java.nio.file.Files.list(partDirectory)) {
                List<Path> objFiles = files.filter(path -> path.getFileName().toString().endsWith(".obj")).toList();
                assertFalse(objFiles.isEmpty(), directory);
                for (Path objFile : objFiles) {
                    ObjModel model = ObjModelParser.parse(objFile);
                    for (ObjFace face : model.faces()) {
                        assertTrue(face.vertices().size() <= 4, objFile.toString());
                    }
                }
            }
        }
    }

    @Test
    void generatedRootModelsCarryExactObjOutlineMetadata() throws IOException {
        Map<String, Integer> expectedCuboidCounts = Map.of(
                "1x1x1", 9,
                "1x2x1", 10,
                "1x3x1", 18,
                "1x1x2", 5,
                "1x2x2", 14
        );

        for (Map.Entry<String, Integer> fixture : expectedCuboidCounts.entrySet()) {
            Path modelPath = GENERATED_DIRECTORY.resolve(fixture.getKey()).resolve("0_0_0.json");
            JsonObject model = JsonParser.parseReader(
                    Files.newBufferedReader(modelPath, StandardCharsets.UTF_8)
            ).getAsJsonObject();
            assertTrue(model.has("shadowsandpetals:outline"), fixture.getKey());
            JsonObject outline = model.getAsJsonObject("shadowsandpetals:outline");
            assertEquals("structure", outline.get("space").getAsString(), fixture.getKey());
            assertEquals(
                    fixture.getValue(),
                    outline.getAsJsonArray("cuboids").size(),
                    fixture.getKey()
            );
        }
    }

    private static void assertBetweenZeroAndOne(double value, String message) {
        assertTrue(value >= -EPSILON && value <= 1.0D + EPSILON, message + ": " + value);
    }

    private record SourceFixture(String name, int width, int height, int depth, int faceCount) {
    }
}
