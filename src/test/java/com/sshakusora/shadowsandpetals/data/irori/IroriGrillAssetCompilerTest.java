package com.sshakusora.shadowsandpetals.data.irori;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPart;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IroriGrillAssetCompilerTest {
    @Test
    void compilerProducesBothHalvesForEveryPhysicalGrillCell() {
        Map<String, JsonElement> assets = new IroriGrillAssetCompiler(projectRoot()).compile();

        assertEquals(IroriGrillPart.values().length * 2, assets.size());
        for (IroriGrillPart part : IroriGrillPart.values()) {
            JsonObject lower = assets.get(path(part, "lower")).getAsJsonObject();
            JsonObject upper = assets.get(path(part, "upper")).getAsJsonObject();
            assertTrue(lower.has("textures"));
            assertTrue(upper.has("textures"));
            assertNonEmptyElements(lower);
            assertNonEmptyElements(upper);
            assertBounds(lower, 0.0D, 16.0D, 10.0D, 16.0D, 0.0D, 16.0D);
            assertBounds(upper, 0.0D, 16.0D, 0.0D, 5.5D, 0.0D, 16.0D);
        }
    }

    private static String path(IroriGrillPart part, String half) {
        return "models/block/grill/double/" + part.modelName() + "_" + half + ".json";
    }

    private static void assertNonEmptyElements(JsonObject model) {
        JsonArray elements = model.getAsJsonArray("elements");
        assertFalse(elements.isEmpty());
    }

    private static void assertBounds(
            JsonObject model,
            double minX,
            double maxX,
            double minY,
            double maxY,
            double minZ,
            double maxZ
    ) {
        for (JsonElement elementValue : model.getAsJsonArray("elements")) {
            JsonObject element = elementValue.getAsJsonObject();
            double[] from = vector(element.getAsJsonArray("from"));
            double[] to = vector(element.getAsJsonArray("to"));
            assertTrue(from[0] >= minX && to[0] <= maxX);
            assertTrue(from[1] >= minY && to[1] <= maxY);
            assertTrue(from[2] >= minZ && to[2] <= maxZ);
        }
    }

    private static double[] vector(JsonArray value) {
        return new double[]{value.get(0).getAsDouble(), value.get(1).getAsDouble(), value.get(2).getAsDouble()};
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            Path models = current.resolve("src/main/resources/assets/shadowsandpetals/models/block/grill");
            if (Files.isRegularFile(models.resolve("1_1.json"))
                    && Files.isRegularFile(models.resolve("1_2.json"))
                    && Files.isRegularFile(models.resolve("2_2.json"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("could not locate project root from " + Path.of("").toAbsolutePath());
    }
}
