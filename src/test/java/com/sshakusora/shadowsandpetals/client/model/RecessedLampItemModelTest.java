package com.sshakusora.shadowsandpetals.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RecessedLampItemModelTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/shadowsandpetals");

    @Test
    void itemKeepsEveryOriginalDisplayTransform() throws IOException {
        JsonObject item = read(ASSETS.resolve("models/item/recessed_lamp.json"));
        JsonObject original = read(Path.of("src/test/resources/recessed_lamp/item-display.json"));
        assertEquals(original, item.getAsJsonObject("display"));
        assertEquals("shadowsandpetals:block/recessed_lamp/up_off", item.get("parent").getAsString());
        JsonObject definition = read(Path.of(
                "src/generated/resources/assets/shadowsandpetals/items/recessed_lamp.json"));
        assertEquals("shadowsandpetals:item/recessed_lamp",
                definition.getAsJsonObject("model").get("model").getAsString());
    }

    @Test
    void blockstateFallbacksUseCompleteModelsAtTheirActualMountingHeights() throws IOException {
        for (String family : new String[]{"recessed_lamp", "recessed_lamp_composite"}) {
            JsonObject blockstate = read(Path.of("src/generated/resources/assets/shadowsandpetals/blockstates/" + family + ".json"));
            for (var variant : blockstate.getAsJsonObject("variants").entrySet()) {
                String key = variant.getKey();
                JsonObject model = read(modelPath(variant.getValue().getAsJsonObject().get("model").getAsString()));
                boolean ceiling = key.contains("mount=ceiling");
                boolean slab = key.contains("mount=floor_slab") || key.contains("mount=ceiling_slab");
                double translation = 0;
                if (model.has("parent")) {
                    translation = model.getAsJsonObject("transform").getAsJsonArray("translation").get(1).getAsDouble();
                    model = read(modelPath(model.get("parent").getAsString()));
                }
                double expected = slab ? (ceiling ? 0.5 : -0.5) : 0;
                if (family.endsWith("_composite")) {
                    expected = -expected;
                }
                assertEquals(expected, translation, 0.00001, key);
                assertFalse(model.has("loader"));
                assertEquals(16, model.getAsJsonArray("elements").size());
                double minX = 16, maxX = 0, minZ = 16, maxZ = 0;
                for (var entry : model.getAsJsonArray("elements")) {
                    var element = entry.getAsJsonObject();
                    minX = Math.min(minX, element.getAsJsonArray("from").get(0).getAsDouble());
                    maxX = Math.max(maxX, element.getAsJsonArray("to").get(0).getAsDouble());
                    minZ = Math.min(minZ, element.getAsJsonArray("from").get(2).getAsDouble());
                    maxZ = Math.max(maxZ, element.getAsJsonArray("to").get(2).getAsDouble());
                    for (var face : element.getAsJsonObject("faces").entrySet()) {
                        var data = face.getValue().getAsJsonObject();
                        if (key.contains("lit=true") && data.get("texture").getAsString().equals("#light")) {
                            assertEquals(15, data.getAsJsonObject("neoforge_data").get("light_emission").getAsInt());
                            assertFalse(data.getAsJsonObject("neoforge_data").get("ambient_occlusion").getAsBoolean());
                        }
                    }
                }
                assertEquals(1, minX, key);
                assertEquals(15, maxX, key);
                assertEquals(1, minZ, key);
                assertEquals(15, maxZ, key);
            }
        }
    }

    @Test
    void everyNonemptyConnectionModelResolvesAndObsoleteModelsAreAbsent() throws IOException {
        for (String direction : new String[]{"up", "down"}) {
            assertFalse(Files.exists(ASSETS.resolve("models/block/recessed_lamp/" + direction + "_0.json")));
            for (int mask = 1; mask <= 15; mask++) {
                JsonObject source = read(ASSETS.resolve("models/block/recessed_lamp/" + direction + "_" + mask + ".json"));
                assertFalse(source.getAsJsonArray("elements").isEmpty());
                for (String mode : new String[]{"on", "off"}) {
                    JsonObject model = read(ASSETS.resolve("models/block/recessed_lamp/" + direction + "_" + mask + "_" + mode + ".json"));
                    assertEquals("shadowsandpetals:block/recessed_lamp/" + direction + "_" + mask,
                            model.get("parent").getAsString());
                    JsonObject textures = source.getAsJsonObject("textures").deepCopy();
                    model.getAsJsonObject("textures").entrySet().forEach(e -> textures.add(e.getKey(), e.getValue()));
                    for (var element : source.getAsJsonArray("elements")) {
                        for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                            String alias = face.getValue().getAsJsonObject().get("texture").getAsString().substring(1);
                            String texture = textures.get(alias).getAsString();
                            String resource = texture.substring("shadowsandpetals:".length());
                            assertTrue(Files.exists(ASSETS.resolve("textures/" + resource + ".png")), texture);
                        }
                    }
                }
            }
        }
        try (var files = Files.list(ASSETS.resolve("models/block/recessed_lamp"))) {
            assertTrue(files.noneMatch(p -> p.toString().endsWith(".obj") || p.toString().endsWith(".mtl")));
        }
        Path obsoleteGeometry = ASSETS.resolve("lamp_geometry/recessed_lamp");
        if (Files.exists(obsoleteGeometry)) {
            try (var files = Files.list(obsoleteGeometry)) {
                assertTrue(files.findAny().isEmpty());
            }
        }
        for (int texture = 1; texture <= 4; texture++) {
            assertFalse(Files.exists(ASSETS.resolve("textures/block/recessed_lamp/" + texture + ".png")));
        }
    }

    private static Path modelPath(String id) {
        String path = "assets/shadowsandpetals/models/" + id.substring("shadowsandpetals:".length()) + ".json";
        Path authored = Path.of("src/main/resources").resolve(path);
        return Files.exists(authored) ? authored : Path.of("src/generated/resources").resolve(path);
    }

    @Test
    void assembledModelsBindAllEightFrameSectionsToTheOpaqueFrameTexture() throws IOException {
        for (String direction : new String[]{"up", "down"}) {
            for (String light : new String[]{"on", "off"}) {
                JsonObject model = read(ASSETS.resolve("models/block/recessed_lamp/" + direction + "_" + light + ".json"));
                JsonObject textures = model.getAsJsonObject("textures");
                assertEquals("shadowsandpetals:block/recessed_lamp/edge_binding",
                        textures.get("edge_binding").getAsString());
                assertEquals("shadowsandpetals:block/recessed_lamp/" + light,
                        textures.get("light").getAsString());
                int frames = 0;
                for (var entry : model.getAsJsonArray("elements")) {
                    JsonObject faces = entry.getAsJsonObject().getAsJsonObject("faces");
                    boolean frame = false;
                    for (var face : faces.entrySet()) {
                        String reference = face.getValue().getAsJsonObject().get("texture").getAsString();
                        assertTrue(reference.startsWith("#"));
                        assertTrue(textures.has(reference.substring(1)), reference);
                        frame |= reference.equals("#edge_binding");
                    }
                    if (frame) {
                        frames++;
                        for (var face : faces.entrySet()) {
                            assertEquals("#edge_binding", face.getValue().getAsJsonObject().get("texture").getAsString());
                        }
                    }
                }
                assertEquals(8, frames, direction + "_" + light);
            }
        }
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
