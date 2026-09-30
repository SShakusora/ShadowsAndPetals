package com.sshakusora.shadowsandpetals.data.curtain;

import com.google.gson.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full-output golden test for the Java replacement of the two Node compilers.
 *
 * <p>The checked-in manifest was generated from the former Node pipeline's
 * resource output.  It stores canonical JSON digests instead of copies of
 * every generated file, so the runtime resource tree can be moved to
 * {@code src/generated/resources} without making the golden fixture huge.</p>
 */
class CurtainAssetCompilerGoldenTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String GOLDEN_RESOURCE = "curtain/asset-golden.json";

    @Test
    void javaCompilerMatchesNodeGeneratedCurtainAssets() throws IOException {
        Path projectRoot = projectRoot();
        Path goldenPath = projectRoot.resolve("src/test/resources").resolve(GOLDEN_RESOURCE);
        assertTrue(Files.isRegularFile(goldenPath), "missing curtain golden manifest: " + goldenPath);

        JsonObject golden = readObject(goldenPath);
        assertEquals(1, golden.get("version").getAsInt(), "unsupported golden manifest version");
        JsonObject expected = golden.getAsJsonObject("assets");
        Map<String, JsonElement> actual = new CurtainAssetCompiler(projectRoot).compile();

        assertEquals(expected.keySet(), actual.keySet(), "generated curtain asset set changed");
        for (Map.Entry<String, JsonElement> entry : actual.entrySet()) {
            String expectedDigest = expected.get(entry.getKey()).getAsString();
            assertEquals(expectedDigest, digest(entry.getValue()),
                    "generated curtain asset differs: " + entry.getKey());
        }
        assertFalse(actual.isEmpty(), "curtain compiler produced no assets");
    }

    @Test
    void curatedOpenWhiteMastersMatchBakedUv() {
        assertDoesNotThrow(
                () -> new CurtainAssetCompiler(projectRoot()).compile(),
                "curated large-curtain open masters must preserve baked face UVs");
    }

    @Test
    void largeCurtainStaticParticlesUseFabricColor() {
        CurtainAssetCompiler compiler = new CurtainAssetCompiler(projectRoot());
        Map<String, JsonElement> actual = compiler.compile();

        for (String side : List.of("right", "left")) {
            for (String pose : List.of("closed", "open")) {
                for (String half : List.of("lower", "upper")) {
                    for (String column : List.of("inner", "outer")) {
                        String quadrant = half + "_" + column;
                        String base = "models/block/large_curtain/static/"
                                + side + "/" + pose + "/";
                        String whitePath = base + "white/" + quadrant + ".json";
                        JsonObject whiteMaster = compiler.readExistingObject(whitePath);
                        assertEquals(
                                CurtainAssetCompiler.CURTAIN_WHITE_TEXTURE,
                                whiteMaster.getAsJsonObject("textures")
                                        .get("particle").getAsString(),
                                whitePath + " must use the white curtain texture for particles"
                        );

                        for (String color : CurtainAssetCompiler.COLORS) {
                            if ("white".equals(color)) {
                                continue;
                            }
                            String path = base + color + "/" + quadrant + ".json";
                            JsonElement generated = actual.get(path);
                            assertNotNull(generated, "missing generated model: " + path);
                            assertEquals(
                                    CurtainAssetCompiler.MOD_ID + ":block/curtain/" + color,
                                    generated.getAsJsonObject().getAsJsonObject("textures")
                                            .get("particle").getAsString(),
                                    path + " must use its dyed curtain texture for particles"
                            );
                        }
                    }
                }
            }
        }
    }

    @Test
    void oneCellItemReusesLongCurtainDisplayTransformsWithoutReusingItsGeometry() {
        Map<String, JsonElement> actual = new CurtainAssetCompiler(projectRoot()).compile();
        JsonObject curtainItem = actual.get("models/block/curtain/item/white.json")
                .getAsJsonObject();
        JsonObject longCurtainItem = actual.get("models/block/long_curtain/item/white.json")
                .getAsJsonObject();

        assertEquals(longCurtainItem.get("display"), curtainItem.get("display"));
        assertNotEquals(longCurtainItem.get("elements"), curtainItem.get("elements"));
        assertEquals(14, longCurtainItem.getAsJsonArray("elements").size());
        assertEquals(10, curtainItem.getAsJsonArray("elements").size());
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            Path blockModels = current.resolve("src/main/resources/assets/shadowsandpetals/models/block");
            if (Files.isDirectory(blockModels.resolve("long_curtain/source"))
                    && Files.isDirectory(blockModels.resolve("large_curtain/source"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("could not locate project root from " + Path.of("").toAbsolutePath());
    }

    private static JsonObject readObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String digest(JsonElement value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte valueByte : bytes) {
                result.append(String.format(Locale.ROOT, "%02x", valueByte));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("JVM does not provide SHA-256", exception);
        }
    }

    private static String canonical(JsonElement value) {
        if (value.isJsonObject()) {
            StringBuilder result = new StringBuilder("{");
            value.getAsJsonObject().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        if (result.length() > 1) {
                            result.append(',');
                        }
                        result.append(GSON.toJson(entry.getKey())).append(':').append(canonical(entry.getValue()));
                    });
            return result.append('}').toString();
        }
        if (value.isJsonArray()) {
            StringBuilder result = new StringBuilder("[");
            for (JsonElement element : value.getAsJsonArray()) {
                if (result.length() > 1) {
                    result.append(',');
                }
                result.append(canonical(element));
            }
            return result.append(']').toString();
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return new BigDecimal(value.getAsString()).stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }
}
