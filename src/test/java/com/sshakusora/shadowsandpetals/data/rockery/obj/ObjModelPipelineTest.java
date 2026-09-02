package com.sshakusora.shadowsandpetals.data.rockery.obj;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
                new SourceFixture("1_2_2", 1, 2, 2, 58)
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
                "1_2_2", 13
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
    void extractsConvexNonCuboidSixFaceTopology() throws IOException {
        ObjModel model = ObjModelParser.parse(SOURCE_DIRECTORY.resolve("1_2_2.obj"));

        List<ObjModelOutline.Cuboid> solids = ObjModelOutline.extract(model);

        ObjModelOutline.Cuboid importedSolid = solids.getLast();
        assertEquals(13, solids.size());
        assertEquals(8, importedSolid.vertices().size());
        assertEquals(6, importedSolid.faces().size());
        assertEquals(12, importedSolid.edges().size());
    }

    @Test
    void rejectsNonPlanarSixFaceTopology() {
        List<ObjVector3> vertices = List.of(
                new ObjVector3(0.0D, 0.0D, 0.0D),
                new ObjVector3(1.0D, 0.0D, 0.0D),
                new ObjVector3(1.0D, 0.0D, 1.0D),
                new ObjVector3(0.0D, 0.0D, 1.0D),
                new ObjVector3(0.0D, 1.0D, 0.0D),
                new ObjVector3(1.0D, 1.0D, 0.0D),
                new ObjVector3(1.0D, 1.0D, 1.25D),
                new ObjVector3(0.0D, 1.0D, 1.0D)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ObjModelOutline.extract(singleObject(vertices, cubeFaces()))
        );

        assertTrue(exception.getMessage().contains("non-planar face"));
    }

    @Test
    void rejectsConcaveSixFaceTopology() {
        List<ObjVector3> vertices = concavePrismVertices();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ObjModelOutline.extract(singleObject(vertices, concavePrismFaces()))
        );

        assertTrue(exception.getMessage().contains("non-convex"));
    }

    @Test
    void rejectsIncompleteNonCuboidTopology() {
        List<int[]> faces = new ArrayList<>(Arrays.asList(concavePrismFaces()));
        faces.removeLast();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ObjModelOutline.extract(singleObject(concavePrismVertices(), faces.toArray(int[][]::new)))
        );

        assertTrue(exception.getMessage().contains("three edge directions"));
    }

    @Test
    void rejectsEdgesThatAreNotUsedByExactlyTwoFaces() {
        int[][] faces = cubeFaces();
        faces[5] = new int[]{3, 0, 4, 6};

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ObjModelOutline.extract(singleObject(cubeVertices(), faces))
        );

        assertTrue(exception.getMessage().contains("topological edges")
                || exception.getMessage().contains("closed six-face topology")
                || exception.getMessage().contains("shared by more than two faces"));
    }

    @Test
    void cutsImportedModelsIntoExpectedPartGrids() throws IOException {
        List<SourceFixture> fixtures = List.of(
                new SourceFixture("1_1_1", 1, 1, 1, 39),
                new SourceFixture("1_2_1", 1, 2, 1, 44),
                new SourceFixture("1_3_1", 1, 3, 1, 83),
                new SourceFixture("1_1_2", 1, 1, 2, 30),
                new SourceFixture("1_2_2", 1, 2, 2, 58)
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
        assertEquals(0.0D, cut.alignment().z(), EPSILON);
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
            try (var files = Files.list(partDirectory)) {
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
                "1x2x2", 13
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

    private static ObjModel singleObject(List<ObjVector3> vertices, int[][] faceIndices) {
        List<ObjFace> faces = new ArrayList<>();
        for (int[] faceIndicesForObject : faceIndices) {
            List<ObjVertex> corners = Arrays.stream(faceIndicesForObject)
                    .mapToObj(index -> new ObjVertex(vertices.get(index), null, null))
                    .toList();
            faces.add(new ObjFace(0, "test", "rockery", corners));
        }
        return new ObjModel(
                faces,
                List.of(new ObjObject(0, "test", vertices)),
                Set.of("rockery")
        );
    }

    private static List<ObjVector3> cubeVertices() {
        return List.of(
                new ObjVector3(0.0D, 0.0D, 0.0D),
                new ObjVector3(1.0D, 0.0D, 0.0D),
                new ObjVector3(1.0D, 0.0D, 1.0D),
                new ObjVector3(0.0D, 0.0D, 1.0D),
                new ObjVector3(0.0D, 1.0D, 0.0D),
                new ObjVector3(1.0D, 1.0D, 0.0D),
                new ObjVector3(1.0D, 1.0D, 1.0D),
                new ObjVector3(0.0D, 1.0D, 1.0D)
        );
    }

    private static int[][] cubeFaces() {
        return new int[][]{
                {0, 1, 2, 3},
                {4, 7, 6, 5},
                {0, 4, 5, 1},
                {3, 2, 6, 7},
                {0, 3, 7, 4},
                {1, 5, 6, 2}
        };
    }

    private static List<ObjVector3> concavePrismVertices() {
        return List.of(
                new ObjVector3(0.0D, 0.0D, 0.0D),
                new ObjVector3(0.0D, 2.0D, 0.0D),
                new ObjVector3(0.0D, 0.7D, 0.5D),
                new ObjVector3(0.0D, 0.0D, 1.0D),
                new ObjVector3(1.0D, 0.0D, 0.0D),
                new ObjVector3(1.0D, 2.0D, 0.0D),
                new ObjVector3(1.0D, 0.7D, 0.5D),
                new ObjVector3(1.0D, 0.0D, 1.0D)
        );
    }

    private static int[][] concavePrismFaces() {
        return new int[][]{
                {0, 1, 2, 3},
                {4, 7, 6, 5},
                {0, 4, 5, 1},
                {1, 5, 6, 2},
                {2, 6, 7, 3},
                {3, 7, 4, 0}
        };
    }

    private record SourceFixture(String name, int width, int height, int depth, int faceCount) {
    }
}
