package com.sshakusora.shadowsandpetals.data.curtain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

import static com.sshakusora.shadowsandpetals.data.curtain.CurtainAssetCompiler.*;

final class SmallCurtainCompiler {
    private static final List<Part> PARTS = List.of(
            new Part(
                    "upper_right", "right", "upper", "right/upper.json",
                    List.of("panel_1_fabric", "panel_1_anchor", "panel_2_fabric", "panel_2_anchor",
                            "panel_3_fabric", "panel_3_anchor", "panel_4_fabric", "panel_4_anchor", "rail", "rail")
            ),
            new Part(
                    "lower_right", "right", "lower", "right/lower.json",
                    List.of("panel_1", "panel_2", "panel_3", "panel_4")
            ),
            new Part(
                    "upper_left", "left", "upper", "left/upper.json",
                    List.of("panel_1_fabric", "panel_1_anchor", "panel_2_fabric", "panel_2_anchor",
                            "panel_3_fabric", "panel_3_anchor", "panel_4_fabric", "panel_4_anchor", "rail", "rail")
            ),
            new Part(
                    "lower_left", "left", "lower", "left/lower.json",
                    List.of("panel_1", "panel_2", "panel_3", "panel_4")
            )
    );

    private final CurtainAssetCompiler compiler;
    private final Map<String, JsonElement> assets;

    SmallCurtainCompiler(CurtainAssetCompiler compiler, Map<String, JsonElement> assets) {
        this.compiler = compiler;
        this.assets = assets;
    }

    void compile() {
        writeWhiteMasters();
        bakeOpenMasters();
        deriveColorVariants();
        splitParts();
    }

    private void writeWhiteMasters() {
        for (Part part : PARTS) {
            JsonObject master = compiler.readSourceObject("curtain/source/" + part.source());
            validateSource(master, part.source(), part.boneOfElement().size());
            compiler.put(assets, staticRel(part, "closed", "white"), master);
        }

        JsonObject item = compiler.readSourceObject("curtain/source/item.json");
        if (whiteTextureKeys(item).isEmpty()) {
            throw failure("curtain/item.json does not reference the white curtain texture");
        }
        compiler.put(assets, "models/block/curtain/item/white.json", item);
    }

    private void bakeOpenMasters() {
        for (Part part : PARTS) {
            JsonObject master = generatedObject(staticRel(part, "closed", "white"));
            Map<String, ModelElementTransforms.Pose> pose = openPoseOf(part);
            JsonObject rig = compiler.readExistingObject(rigPath(part));
            Map<String, String> parentOf = new HashMap<>();
            for (JsonElement element : rig.getAsJsonArray("bones")) {
                JsonObject bone = element.getAsJsonObject();
                if (bone.has("parent")) {
                    parentOf.put(bone.get("name").getAsString(), bone.get("parent").getAsString());
                }
            }

            Map<String, BoneTransform> transforms = new LinkedHashMap<>();
            for (String boneName : pose.keySet()) {
                double tx = 0.0D;
                double ty = 0.0D;
                double tz = 0.0D;
                double ry = 0.0D;
                String pivotBone = boneName;
                String current = boneName;
                while (current != null) {
                    ModelElementTransforms.Pose bonePose = pose.get(current);
                    if (bonePose != null) {
                        tx += bonePose.tx();
                        ty += bonePose.ty();
                        tz += bonePose.tz();
                        if (current.equals(boneName)) {
                            ry = bonePose.ry();
                        } else if (tx != 0.0D || ty != 0.0D || tz != 0.0D) {
                            pivotBone = current;
                        }
                    }
                    current = parentOf.get(current);
                }
                if (tx != 0.0D || ty != 0.0D || tz != 0.0D || ry != 0.0D) {
                    transforms.put(boneName, new BoneTransform(
                            new ModelElementTransforms.Pose(tx, ty, tz, ry), pivotBone));
                }
            }

            JsonArray outputElements = new JsonArray();
            JsonArray sourceElements = master.getAsJsonArray("elements");
            for (int index = 0; index < sourceElements.size(); index++) {
                JsonObject element = sourceElements.get(index).getAsJsonObject();
                BoneTransform transform = transforms.get(part.boneOfElement().get(index));
                if (transform == null) {
                    outputElements.add(element.deepCopy());
                    continue;
                }
                JsonArray pivot;
                if (hasYRotation(element)) {
                    pivot = element.getAsJsonObject("rotation").getAsJsonArray("origin");
                } else {
                    pivot = rigPivotOf(part, transform.pivotBone());
                }
                outputElements.add(ModelElementTransforms.rotateElement(element, transform.pose(), pivot));
            }

            JsonObject openModel = new JsonObject();
            openModel.add("textures", master.get("textures").deepCopy());
            openModel.add("elements", outputElements);
            compiler.put(assets, staticRel(part, "open", "white"), openModel);
        }
    }

    private Map<String, ModelElementTransforms.Pose> openPoseOf(Part part) {
        JsonObject clip = compiler.readExistingObject(openClipPath(part));
        Map<String, ModelElementTransforms.Pose> pose = new LinkedHashMap<>();
        for (JsonElement animationElement : clip.getAsJsonArray("animations")) {
            JsonObject animation = animationElement.getAsJsonObject();
            JsonArray target = animation.getAsJsonArray("keyframes")
                    .get(animation.getAsJsonArray("keyframes").size() - 1)
                    .getAsJsonObject().getAsJsonArray("target");
            String bone = animation.get("bone").getAsString();
            if ("minecraft:position".equals(animation.get("target").getAsString())) {
                pose.put(bone, new ModelElementTransforms.Pose(
                        target.get(0).getAsDouble(), target.get(1).getAsDouble(),
                        target.get(2).getAsDouble(), 0.0D));
            } else if ("minecraft:rotation".equals(animation.get("target").getAsString())) {
                ModelElementTransforms.Pose previous = pose.getOrDefault(
                        bone, ModelElementTransforms.Pose.zero());
                pose.put(bone, new ModelElementTransforms.Pose(
                        previous.tx(), previous.ty(), previous.tz(), target.get(1).getAsDouble()));
            }
        }
        return pose;
    }

    private JsonArray rigPivotOf(Part part, String boneName) {
        JsonObject rig = compiler.readExistingObject(rigPath(part));
        for (JsonElement element : rig.getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            if (boneName.equals(bone.get("name").getAsString())) {
                return bone.getAsJsonArray("pivot");
            }
        }
        throw failure(rigPath(part) + " has no bone " + boneName);
    }

    private void deriveColorVariants() {
        for (Part part : PARTS) {
            for (String pose : List.of("closed", "open")) {
                String whitePath = staticRel(part, pose, "white");
                JsonObject master = generatedObject(whitePath);
                validateSource(master, whitePath, part.boneOfElement().size());
                List<String> keys = whiteTextureKeys(master);
                for (String color : COLORS) {
                    if ("white".equals(color)) {
                        continue;
                    }
                    JsonObject variant = parentVariant(resourceId(whitePath), keys, color);
                    compiler.put(assets, staticRel(part, pose, color), variant);
                }
            }
        }

        String whiteItemPath = "models/block/curtain/item/white.json";
        JsonObject item = generatedObject(whiteItemPath);
        List<String> itemKeys = whiteTextureKeys(item);
        if (itemKeys.isEmpty()) {
            throw failure(whiteItemPath + " does not reference the white curtain texture");
        }
        for (String color : COLORS) {
            if (!"white".equals(color)) {
                compiler.put(assets, "models/block/curtain/item/" + color + ".json",
                        parentVariant(resourceId(whiteItemPath), itemKeys, color));
            }
        }
    }

    private void splitParts() {
        Map<String, Set<String>> sideBones = new LinkedHashMap<>();
        sideBones.put("left", new HashSet<>());
        sideBones.put("right", new HashSet<>());

        for (Part part : PARTS) {
            JsonObject master = generatedObject(staticRel(part, "closed", "white"));
            validateSource(master, staticRel(part, "closed", "white"), part.boneOfElement().size());
            List<String> keys = whiteTextureKeys(master);
            Map<String, JsonArray> byBone = new LinkedHashMap<>();
            JsonArray elements = master.getAsJsonArray("elements");
            for (int index = 0; index < elements.size(); index++) {
                String bone = part.boneOfElement().get(index);
                byBone.computeIfAbsent(bone, ignored -> new JsonArray())
                        .add(elements.get(index).deepCopy());
                sideBones.get(part.side()).add(bone);
            }

            for (Map.Entry<String, JsonArray> entry : byBone.entrySet()) {
                String bone = entry.getKey();
                JsonObject whiteBone = new JsonObject();
                whiteBone.add("textures", master.get("textures").deepCopy());
                whiteBone.add("elements", entry.getValue());
                String whitePath = "models/block/curtain/animated/" + part.side()
                        + "/white/" + bone + ".json";
                compiler.put(assets, whitePath, whiteBone);

                for (String color : COLORS) {
                    if (!"white".equals(color)) {
                        compiler.put(assets,
                                "models/block/curtain/animated/" + part.side() + "/" + color
                                        + "/" + bone + ".json",
                                parentVariant(resourceId(whitePath), keys, color));
                    }
                }
            }
        }
    }

    private JsonObject generatedObject(String path) {
        return compiler.generated(assets, path).getAsJsonObject();
    }

    private static JsonObject parentVariant(String parent, List<String> keys, String color) {
        JsonObject variant = new JsonObject();
        variant.addProperty("parent", parent);
        JsonObject textures = new JsonObject();
        for (String key : keys) {
            textures.addProperty(key, MOD_ID + ":block/curtain/" + color);
        }
        variant.add("textures", textures);
        return variant;
    }

    private static boolean hasYRotation(JsonObject element) {
        return element.has("rotation") && element.get("rotation").isJsonObject()
                && "y".equals(element.getAsJsonObject("rotation").get("axis").getAsString());
    }

    private static String staticRel(Part part, String pose, String color) {
        return "models/block/curtain/static/" + part.side() + "/" + pose + "/"
                + color + "/" + part.half() + ".json";
    }

    private static String openClipPath(Part part) {
        return "neoforge/animations/entity/curtain/" + part.id() + "/opening.json";
    }

    private static String rigPath(Part part) {
        return "sap/animations/rigs/curtain/" + part.id() + ".json";
    }

    private record Part(String id, String side, String half, String source, List<String> boneOfElement) {
    }

    private record BoneTransform(ModelElementTransforms.Pose pose, String pivotBone) {
    }
}
