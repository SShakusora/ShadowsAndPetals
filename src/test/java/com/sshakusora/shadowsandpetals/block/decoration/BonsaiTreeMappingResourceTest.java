package com.sshakusora.shadowsandpetals.block.decoration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BonsaiTreeMappingResourceTest {
    private static final List<String> MAPPINGS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "cherry", "mangrove",
            "pale_oak", "sakura", "maple", "ginkgo", "autumn_oak"
    );

    @Test
    void everyMappingHasResolvableIdentifiers() throws IOException {
        ClassLoader classLoader = BonsaiTreeMappingResourceTest.class.getClassLoader();
        for (String mapping : MAPPINGS) {
            String resource = "data/shadowsandpetals/bonsai_trees/" + mapping + ".json";
            try (InputStream stream = classLoader.getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                JsonObject json = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)
                ).getAsJsonObject();
                assertIdentifier(json, "sapling", resource);
                assertIdentifier(json, "trunk", resource);
                if (json.has("leaves")) {
                    assertIdentifier(json, "leaves", resource);
                }
            }
        }
    }

    @Test
    void treeModelsDoNotDuplicateThePotGeometry() throws IOException {
        ClassLoader classLoader = BonsaiTreeMappingResourceTest.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(
                "assets/shadowsandpetals/models/block/bonsai/bonsai.json")) {
            assertNotNull(stream);
            JsonObject pot = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject();
            assertEquals(7, pot.getAsJsonArray("elements").size());
        }

        for (String shape : List.of(
                "semi_cascade", "slanting", "twin", "windswept",
                "semi_cascade_dead", "slanting_dead", "twin_dead", "windswept_dead"
        )) {
            String resource = "assets/shadowsandpetals/models/block/bonsai/bonsai_" + shape + ".json";
            try (InputStream stream = classLoader.getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                JsonObject model = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)
                ).getAsJsonObject();
                assertFalse(model.getAsJsonObject("textures").has("pot"), resource);
                assertFalse(model.getAsJsonArray("elements").isEmpty(), resource);
                for (JsonElement element : model.getAsJsonArray("elements")) {
                    assertFalse(element.toString().contains("#pot"), resource);
                }
                if (model.has("groups")) {
                    int elementCount = model.getAsJsonArray("elements").size();
                    for (JsonElement group : model.getAsJsonArray("groups")) {
                        for (JsonElement child : group.getAsJsonObject().getAsJsonArray("children")) {
                            int index = child.getAsInt();
                            assertTrue(index >= 0 && index < elementCount, resource);
                        }
                    }
                }
            }
        }
    }

    @Test
    void accessTransformerExposesTreeGrowerConfigurationFields() throws IOException {
        ClassLoader classLoader = BonsaiTreeMappingResourceTest.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream("META-INF/accesstransformer.cfg")) {
            assertNotNull(stream);
            String accessTransformer = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(accessTransformer.contains(
                    "public net.minecraft.world.level.block.SaplingBlock treeGrower"));
            for (String field : List.of(
                    "tree", "secondaryTree", "flowers", "secondaryFlowers", "megaTree", "secondaryMegaTree"
            )) {
                assertTrue(accessTransformer.contains(
                        "public net.minecraft.world.level.block.grower.TreeGrower " + field));
            }
        }
    }

    private static void assertIdentifier(JsonObject json, String key, String resource) {
        assertTrue(json.has(key), resource + " missing " + key);
        Identifier parsed = Identifier.tryParse(json.get(key).getAsString());
        assertNotNull(parsed, resource + " has invalid " + key);
    }
}
