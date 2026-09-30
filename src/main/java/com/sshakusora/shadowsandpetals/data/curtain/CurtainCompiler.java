package com.sshakusora.shadowsandpetals.data.curtain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import static com.sshakusora.shadowsandpetals.data.curtain.CurtainAssetCompiler.*;

/** Derives one-cell curtain assets from the upper long-curtain assets. */
final class CurtainCompiler {
    private static final List<String> SIDES = List.of("left", "right");
    private static final List<String> POSES = List.of("closed", "open");
    private static final List<String> UPPER_BONES = List.of(
            "panel_1_anchor", "panel_1_fabric", "panel_2_anchor", "panel_2_fabric",
            "panel_3_anchor", "panel_3_fabric", "panel_4_anchor", "panel_4_fabric", "rail"
    );

    private final CurtainAssetCompiler compiler;
    private final Map<String, JsonElement> assets;

    CurtainCompiler(CurtainAssetCompiler compiler, Map<String, JsonElement> assets) {
        this.compiler = compiler;
        this.assets = assets;
    }

    void compile() {
        for (String side : SIDES) {
            for (String pose : POSES) {
                copyStatic(side, pose);
            }
        }
        copyItem();
        copyAnimatedBones();
    }

    private void copyStatic(String side, String pose) {
        String source = "models/block/long_curtain/static/" + side + "/" + pose + "/white/upper.json";
        String target = "models/block/curtain/static/" + side + "/" + pose + "/white/upper.json";
        JsonObject white = compiler.generated(assets, source).getAsJsonObject();
        compiler.put(assets, target, white);
        putColorVariants(target, white, "models/block/curtain/static/" + side + "/" + pose + "/");
    }

    private void copyItem() {
        String target = "models/block/curtain/item/white.json";
        JsonObject item = compiler.generated(
                assets, "models/block/curtain/static/right/closed/white/upper.json"
        ).getAsJsonObject().deepCopy();
        JsonObject longCurtainItem = compiler.generated(
                assets, "models/block/long_curtain/item/white.json"
        ).getAsJsonObject();
        item.add("display", longCurtainItem.get("display").deepCopy());
        compiler.put(assets, target, item);
        putColorVariants(target, item, "models/block/curtain/item/");
    }

    private void copyAnimatedBones() {
        for (String side : SIDES) {
            for (String bone : UPPER_BONES) {
                String source = "models/block/long_curtain/animated/" + side + "/white/" + bone + ".json";
                String target = "models/block/curtain/animated/" + side + "/white/" + bone + ".json";
                JsonObject white = compiler.generated(assets, source).getAsJsonObject();
                compiler.put(assets, target, white);
                putColorVariants(target, white,
                        "models/block/curtain/animated/" + side + "/");
            }
        }
    }

    private void putColorVariants(String whitePath, JsonObject white, String colorPrefix) {
        List<String> keys = whiteTextureKeys(white);
        for (String color : COLORS) {
            if ("white".equals(color)) {
                continue;
            }
            JsonObject variant = new JsonObject();
            variant.addProperty("parent", resourceId(whitePath));
            JsonObject textures = new JsonObject();
            for (String key : keys) {
                textures.addProperty(key, MOD_ID + ":block/curtain/" + color);
            }
            variant.add("textures", textures);
            String fileName = whitePath.substring(whitePath.lastIndexOf('/') + 1);
            String output = colorPrefix.endsWith("/item/")
                    ? colorPrefix + color + ".json"
                    : colorPrefix + color + "/" + fileName;
            compiler.put(assets, output, variant);
        }
    }
}
