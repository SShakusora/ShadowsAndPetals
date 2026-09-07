package com.sshakusora.shadowsandpetals.data.curtain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class CurtainAssetCompiler {
    public static final String MOD_ID = "shadowsandpetals";
    public static final String CURTAIN_WHITE_TEXTURE = MOD_ID + ":block/curtain/white";
    public static final String CURTAIN_DECO_TEXTURE = MOD_ID + ":block/curtain/curtain_deco";

    public static final List<String> COLORS = List.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    );

    private final Path sourceRoot;
    private final Path existingAssetsRoot;

    public CurtainAssetCompiler(Path projectRoot) {
        this(
                projectRoot.resolve("src").resolve("main").resolve("resources")
                        .resolve("assets").resolve(MOD_ID).resolve("models").resolve("block"),
                projectRoot.resolve("src").resolve("main").resolve("resources")
                        .resolve("assets").resolve(MOD_ID)
        );
    }

    public CurtainAssetCompiler(Path sourceRoot, Path existingAssetsRoot) {
        this.sourceRoot = sourceRoot.toAbsolutePath().normalize();
        this.existingAssetsRoot = existingAssetsRoot.toAbsolutePath().normalize();
    }

    public Path sourceRoot() {
        return sourceRoot;
    }

    public Path existingAssetsRoot() {
        return existingAssetsRoot;
    }

    /**
     * Compile every generated curtain asset.  The returned map is keyed by a
     * path relative to {@code assets/shadowsandpetals} and preserves compiler
     * insertion order for readable diagnostics.
     */
    public Map<String, JsonElement> compile() {
        Map<String, JsonElement> assets = new LinkedHashMap<>();
        new SmallCurtainCompiler(this, assets).compile();
        new LargeCurtainCompiler(this, assets).compile();
        return Collections.unmodifiableMap(assets);
    }

    void put(Map<String, JsonElement> assets, String relativePath, JsonElement value) {
        String normalized = normalize(relativePath);
        if (assets.putIfAbsent(normalized, value.deepCopy()) != null) {
            throw failure("duplicate generated asset: " + normalized);
        }
    }

    JsonElement generated(Map<String, JsonElement> assets, String relativePath) {
        String normalized = normalize(relativePath);
        JsonElement value = assets.get(normalized);
        if (value == null) {
            throw failure("missing generated asset: " + normalized);
        }
        return value;
    }

    JsonObject readSourceObject(String relativePath) {
        return readObject(sourceRoot.resolve(relativePath));
    }

    JsonObject readExistingObject(String relativePath) {
        return readObject(existingAssetsRoot.resolve(relativePath));
    }

    static JsonObject readObject(Path path) {
        if (!Files.isRegularFile(path)) {
            throw failure("missing JSON source: " + path);
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw failure("expected JSON object: " + path);
            }
            return parsed.getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalStateException illegalStateException
                    && illegalStateException.getMessage() != null
                    && illegalStateException.getMessage().startsWith("missing JSON source:")) {
                throw illegalStateException;
            }
            throw new IllegalStateException("failed to read JSON source: " + path, exception);
        }
    }

    static String normalize(String path) {
        return path.replace('\\', '/').replaceAll("^/+", "");
    }

    static String resourceId(String relativePath) {
        String normalized = normalize(relativePath);
        if (normalized.endsWith(".json")) {
            normalized = normalized.substring(0, normalized.length() - ".json".length());
        }
        if (normalized.startsWith("models/")) {
            normalized = normalized.substring("models/".length());
        }
        return MOD_ID + ":" + normalized;
    }

    static List<String> whiteTextureKeys(JsonObject model) {
        List<String> keys = new ArrayList<>();
        if (!model.has("textures") || !model.get("textures").isJsonObject()) {
            return keys;
        }
        for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
            if (CURTAIN_WHITE_TEXTURE.equals(entry.getValue().getAsString())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    static void validateSource(JsonObject model, String label, int expectedElements) {
        if (!model.has("elements") || !model.get("elements").isJsonArray()
                || model.getAsJsonArray("elements").size() != expectedElements) {
            int actual = model.has("elements") && model.get("elements").isJsonArray()
                    ? model.getAsJsonArray("elements").size() : -1;
            throw failure(label + ": expected " + expectedElements + " elements, found "
                    + (actual < 0 ? "none" : actual));
        }
        if (whiteTextureKeys(model).isEmpty()) {
            throw failure(label + " does not reference the white curtain texture");
        }
    }

    static IllegalStateException failure(String message) {
        return new IllegalStateException("curtain asset compiler: " + message);
    }
}
