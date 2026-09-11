package com.sshakusora.shadowsandpetals.data.curtain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

import static com.sshakusora.shadowsandpetals.data.curtain.CurtainAssetCompiler.*;

final class LargeCurtainCompiler {
    private static final List<String> SIDES = List.of("right", "left");
    private static final List<String> POSES = List.of("closed", "open");
    private static final List<String> HALVES = List.of("upper", "lower");
    private static final List<String> COLUMNS = List.of("outer", "inner");

    private final CurtainAssetCompiler compiler;
    private final Map<String, JsonElement> assets;

    LargeCurtainCompiler(CurtainAssetCompiler compiler, Map<String, JsonElement> assets) {
        this.compiler = compiler;
        this.assets = assets;
    }

    void compile() {
        processSide("right", true);
        processSide("left", false);
        deriveColorVariants();
    }

    private void processSide(String side, boolean writeItemMaster) {
        JsonObject closed = compiler.readSourceObject("large_curtain/source/" + side + "/model.json");
        if (!closed.has("elements") || !closed.get("elements").isJsonArray()
                || closed.getAsJsonArray("elements").isEmpty()) {
            throw failure("large_curtain/" + side + "/model.json has no elements");
        }
        JsonObject animation = compiler.readSourceObject("large_curtain/source/" + side + "/animation.json");
        JsonObject opening = animation.getAsJsonObject("animations").getAsJsonObject("OPENING");
        JsonObject closing = animation.getAsJsonObject("animations").getAsJsonObject("CLOSING");
        if (opening == null || closing == null) {
            throw failure("large_curtain/" + side + "/animation.json lacks OPENING/CLOSING clips");
        }

        List<String> boneOf = boneBindings(closed);
        Map<String, JsonArray> pivots = groupPivots(closed, boneOf);
        Map<String, ModelElementTransforms.Pose> pose = poseOf(opening);
        JsonArray openElements = bakeElements(closed.getAsJsonArray("elements"), boneOf, pose, pivots);

        List<Double> pileX = new ArrayList<>();
        JsonArray closedElements = closed.getAsJsonArray("elements");
        for (int index = 0; index < openElements.size(); index++) {
            if (boneOf.get(index).endsWith("_fabric")) {
                JsonObject element = openElements.get(index).getAsJsonObject();
                pileX.add(element.getAsJsonArray("from").get(0).getAsDouble());
                pileX.add(element.getAsJsonArray("to").get(0).getAsDouble());
            }
        }
        if (!pileX.isEmpty()) {
            double min = pileX.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            double max = pileX.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
            if (max - min > 10.0D + 1.0e-4D) {
                throw failure("large_curtain/" + side + " baked pile is "
                        + String.format(Locale.ROOT, "%.2f", max - min)
                        + "px wide, expected ~8");
            }
        }

        validateStaticOpenMasters(side, openElements);

        if (writeItemMaster) {
            compiler.put(assets, "models/block/large_curtain/item/white.json", closed);
        }

        List<String> bones = new ArrayList<>();
        for (String bone : boneOf) {
            if (!bones.contains(bone)) {
                bones.add(bone);
            }
        }

        for (String bone : bones) {
            JsonObject boneModel = new JsonObject();
            boneModel.add("textures", masterTextures());
            JsonArray boneElements = new JsonArray();
            for (int index = 0; index < closedElements.size(); index++) {
                if (bone.equals(boneOf.get(index))) {
                    boneElements.add(closedElements.get(index).deepCopy());
                }
            }
            boneModel.add("elements", boneElements);
            compiler.put(assets, "models/block/large_curtain/animated/" + side
                    + "/white/" + bone + ".json", boneModel);
        }

        JsonArray rigBones = new JsonArray();
        for (String bone : bones) {
            JsonArray pivot = pivots.get(bone);
            if (pivot == null) {
                throw failure("large_curtain/" + side + " has no pivot for bone " + bone);
            }
            JsonObject rigBone = new JsonObject();
            rigBone.addProperty("name", bone);
            rigBone.add("pivot", pivot.deepCopy());
            if (bone.endsWith("_fabric")) {
                rigBone.addProperty("parent", bone.substring(0, bone.length() - "_fabric".length()) + "_anchor");
            }
            rigBones.add(rigBone);
        }

        JsonObject rig = new JsonObject();
        rig.addProperty("format_version", 1);
        rig.add("bones", rigBones);
        compiler.put(assets, "sap/animations/rigs/large_curtain/" + side + ".json", rig);
        compiler.put(assets, "neoforge/animations/entity/large_curtain/" + side + "/opening.json",
                runtimeClip(opening));
        compiler.put(assets, "neoforge/animations/entity/large_curtain/" + side + "/closing.json",
                runtimeClip(closing));
        compiler.put(assets, "sap/animations/controllers/large_curtain/" + side + ".json",
                controller(side, bones));
    }

    private List<String> boneBindings(JsonObject closed) {
        List<String> bindings = new ArrayList<>();
        for (int index = 0; index < closed.getAsJsonArray("elements").size(); index++) {
            bindings.add(null);
        }
        walkGroups(closed.has("groups") ? closed.getAsJsonArray("groups") : new JsonArray(), null, bindings);
        if (bindings.stream().anyMatch(value -> value == null)) {
            throw failure("large curtain outline has elements without a bone");
        }
        return bindings;
    }

    private void walkGroups(JsonArray nodes, String parent, List<String> bindings) {
        for (JsonElement node : nodes) {
            if (node.isJsonPrimitive() && node.getAsJsonPrimitive().isNumber()) {
                int elementIndex = node.getAsInt();
                if (elementIndex < 0 || elementIndex >= bindings.size()) {
                    throw failure("large curtain outline references element " + elementIndex);
                }
                bindings.set(elementIndex, parent);
            } else if (node.isJsonObject()) {
                JsonObject group = node.getAsJsonObject();
                String name = group.has("name") ? group.get("name").getAsString() : parent;
                walkGroups(group.has("children") ? group.getAsJsonArray("children") : new JsonArray(), name, bindings);
            }
        }
    }

    private Map<String, JsonArray> groupPivots(JsonObject closed, List<String> boneOf) {
        Map<String, JsonArray> pivots = new LinkedHashMap<>();
        JsonArray elements = closed.getAsJsonArray("elements");
        for (int index = 0; index < elements.size(); index++) {
            JsonObject element = elements.get(index).getAsJsonObject();
            String bone = boneOf.get(index);
            if (!pivots.containsKey(bone) && element.has("rotation")
                    && element.getAsJsonObject("rotation").has("origin")) {
                pivots.put(bone, element.getAsJsonObject("rotation").getAsJsonArray("origin"));
            }
        }
        collectGroupPivots(closed.has("groups") ? closed.getAsJsonArray("groups") : new JsonArray(), pivots);
        return pivots;
    }

    private void collectGroupPivots(JsonArray nodes, Map<String, JsonArray> pivots) {
        for (JsonElement node : nodes) {
            if (!node.isJsonObject()) {
                continue;
            }
            JsonObject group = node.getAsJsonObject();
            if (group.has("name") && !pivots.containsKey(group.get("name").getAsString())
                    && group.has("origin")) {
                pivots.put(group.get("name").getAsString(), group.getAsJsonArray("origin"));
            }
            if (group.has("children")) {
                collectGroupPivots(group.getAsJsonArray("children"), pivots);
            }
        }
    }

    private Map<String, ModelElementTransforms.Pose> poseOf(JsonObject clip) {
        Map<String, ModelElementTransforms.Pose> pose = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> boneEntry : clip.getAsJsonObject("bones").entrySet()) {
            JsonObject channels = boneEntry.getValue().getAsJsonObject();
            double tx = 0.0D;
            double ry = 0.0D;
            if (channels.has("position")) {
                JsonArray value = lastFrame(channels.getAsJsonObject("position"));
                tx = -value.get(0).getAsDouble();
            }
            if (channels.has("rotation")) {
                JsonArray value = lastFrame(channels.getAsJsonObject("rotation"));
                ry = -value.get(1).getAsDouble();
            }
            pose.put(boneEntry.getKey(), new ModelElementTransforms.Pose(tx, 0.0D, 0.0D, ry));
        }
        return pose;
    }

    private static JsonArray lastFrame(JsonObject frames) {
        String lastKey = null;
        double lastTime = Double.NEGATIVE_INFINITY;
        for (String key : frames.keySet()) {
            double time = Double.parseDouble(key);
            if (time > lastTime) {
                lastTime = time;
                lastKey = key;
            }
        }
        if (lastKey == null) {
            throw failure("animation channel has no keyframes");
        }
        return frames.getAsJsonArray(lastKey);
    }

    private JsonArray bakeElements(
            JsonArray closed,
            List<String> boneOf,
            Map<String, ModelElementTransforms.Pose> pose,
            Map<String, JsonArray> pivots
    ) {
        JsonArray output = new JsonArray();
        for (int index = 0; index < closed.size(); index++) {
            JsonObject element = closed.get(index).getAsJsonObject();
            String bone = boneOf.get(index);
            ModelElementTransforms.Pose bonePose = pose.getOrDefault(bone, ModelElementTransforms.Pose.zero());
            double tx = bonePose.tx();
            if (bone.endsWith("_fabric")) {
                ModelElementTransforms.Pose anchor = pose.get(bone.substring(0,
                        bone.length() - "_fabric".length()) + "_anchor");
                if (anchor != null) {
                    tx += anchor.tx();
                }
            }
            if ("rail".equals(bone) || (tx == 0.0D && bonePose.ry() == 0.0D)) {
                output.add(element.deepCopy());
                continue;
            }
            JsonArray pivot;
            if (hasYRotation(element)) {
                pivot = element.getAsJsonObject("rotation").getAsJsonArray("origin");
            } else {
                pivot = pivots.get(bone);
            }
            if (pivot == null) {
                throw failure("large curtain has no pivot for bone " + bone);
            }
            output.add(ModelElementTransforms.rotateElement(element,
                    new ModelElementTransforms.Pose(tx, 0.0D, 0.0D, bonePose.ry()), pivot));
        }
        return output;
    }

    /**
     * The open block models are curated because the 2x2 structure translates
     * the upper half and duplicates the visible rail at a quadrant boundary.
     * Their face UVs must nevertheless be the result of the same baked pose as
     * the animation source.  Validate that invariant while running DataGen so
     * a copied right-side UV table cannot silently reach every dye variant.
     */
    private void validateStaticOpenMasters(String side, JsonArray bakedElements) {
        for (String half : HALVES) {
            for (String column : COLUMNS) {
                String quadrant = half + "_" + column;
                String path = "models/block/large_curtain/static/" + side + "/open/white/"
                        + quadrant + ".json";
                JsonObject master = compiler.readExistingObject(path);
                JsonArray elements = master.has("elements") && master.get("elements").isJsonArray()
                        ? master.getAsJsonArray("elements") : new JsonArray();
                for (int index = 0; index < elements.size(); index++) {
                    JsonObject actual = elements.get(index).getAsJsonObject();
                    JsonObject expected = matchingBakedElement(bakedElements, actual, side, half);
                    if (expected == null) {
                        throw failure(path + " element " + index
                                + " has no matching baked open-pose geometry");
                    }
                    if (!expected.getAsJsonObject("faces").equals(actual.getAsJsonObject("faces"))) {
                        throw failure(path + " element " + index
                                + " has UVs/faces inconsistent with the baked " + side + " open pose");
                    }
                }
            }
        }
    }

    private static JsonObject matchingBakedElement(
            JsonArray bakedElements, JsonObject actual, String side, String half
    ) {
        for (JsonElement candidate : bakedElements) {
            JsonObject expected = candidate.getAsJsonObject();
            if (sameStaticGeometry(expected, actual, side, half)) {
                return expected;
            }
        }
        return null;
    }

    private static boolean sameStaticGeometry(JsonObject expected, JsonObject actual, String side, String half) {
        double xOffset = "right".equals(side) ? 16.0D : 0.0D;
        double yOffset = "upper".equals(half) ? -16.0D : 0.0D;
        return sameVector(expected.getAsJsonArray("from"), actual.getAsJsonArray("from"), xOffset, yOffset)
                && sameVector(expected.getAsJsonArray("to"), actual.getAsJsonArray("to"), xOffset, yOffset);
    }

    private static boolean sameVector(JsonArray expected, JsonArray actual, double xOffset, double yOffset) {
        return close(expected.get(0).getAsDouble() + xOffset, actual.get(0).getAsDouble())
                && close(expected.get(1).getAsDouble() + yOffset, actual.get(1).getAsDouble())
                && close(expected.get(2).getAsDouble(), actual.get(2).getAsDouble());
    }

    private static boolean close(double expected, double actual) {
        return Math.abs(expected - actual) <= 1.0e-4D;
    }

    private JsonObject runtimeClip(JsonObject clip) {
        JsonObject output = new JsonObject();
        output.add("length", clip.get("animation_length").deepCopy());
        output.addProperty("loop", false);
        JsonArray animations = new JsonArray();
        for (Map.Entry<String, JsonElement> boneEntry : clip.getAsJsonObject("bones").entrySet()) {
            String bone = boneEntry.getKey();
            for (Map.Entry<String, JsonElement> channelEntry : boneEntry.getValue().getAsJsonObject().entrySet()) {
                String channel = channelEntry.getKey();
                String targetName = "position".equals(channel)
                        ? "minecraft:position" : "minecraft:rotation";
                JsonArray keyframes = new JsonArray();
                JsonObject frames = channelEntry.getValue().getAsJsonObject();
                List<String> keys = new ArrayList<>(frames.keySet());
                keys.sort((left, right) -> Double.compare(Double.parseDouble(left), Double.parseDouble(right)));
                for (String key : keys) {
                    JsonArray value = frames.getAsJsonArray(key);
                    JsonArray converted = new JsonArray();
                    if ("position".equals(channel)) {
                        converted.add(-value.get(0).getAsDouble());
                        converted.add(value.get(1).getAsDouble());
                        converted.add(value.get(2).getAsDouble());
                    } else {
                        converted.add(value.get(0).getAsDouble());
                        converted.add(-value.get(1).getAsDouble());
                        converted.add(value.get(2).getAsDouble());
                    }
                    JsonObject keyframe = new JsonObject();
                    keyframe.addProperty("timestamp", Double.parseDouble(key));
                    keyframe.add("target", converted);
                    keyframe.addProperty("interpolation", "minecraft:linear");
                    keyframes.add(keyframe);
                }
                JsonObject animation = new JsonObject();
                animation.addProperty("bone", bone);
                animation.addProperty("target", targetName);
                animation.add("keyframes", keyframes);
                animations.add(animation);
            }
        }
        output.add("animations", animations);
        return output;
    }

    private JsonObject controller(String side, List<String> bones) {
        JsonObject controller = new JsonObject();
        controller.addProperty("format_version", 1);
        controller.addProperty("rig", MOD_ID + ":large_curtain/" + side);
        controller.addProperty("initial", "closed");

        JsonObject states = new JsonObject();
        states.add("open", controllerState(
                MOD_ID + ":large_curtain/" + side + "/opening", bones));
        states.add("closed", controllerState(
                MOD_ID + ":large_curtain/" + side + "/closing", bones));
        controller.add("states", states);

        JsonArray transitions = new JsonArray();
        transitions.add(transition("open", "closed"));
        transitions.add(transition("closed", "open"));
        controller.add("transitions", transitions);
        return controller;
    }

    private static JsonObject controllerState(String clip, List<String> bones) {
        JsonObject state = new JsonObject();
        state.addProperty("clip", clip);
        state.addProperty("speed", 1);
        state.addProperty("wrap", "clamp");
        JsonArray mask = new JsonArray();
        bones.forEach(mask::add);
        state.add("mask", mask);
        return state;
    }

    private static JsonObject transition(String from, String to) {
        JsonObject transition = new JsonObject();
        transition.addProperty("from", from);
        transition.addProperty("to", to);
        transition.addProperty("duration", 0.08D);
        return transition;
    }

    private void deriveColorVariants() {
        for (String side : SIDES) {
            for (String pose : POSES) {
                for (String half : HALVES) {
                    for (String column : COLUMNS) {
                        String quadrant = half + "_" + column;
                        String whitePath = "models/block/large_curtain/static/" + side + "/" + pose
                                + "/white/" + quadrant + ".json";
                        JsonObject master = compiler.readExistingObject(whitePath);
                        List<String> keys = whiteTextureKeys(master);
                        if (keys.isEmpty()) {
                            throw failure(whitePath + " does not reference the white curtain texture");
                        }
                        for (String color : COLORS) {
                            if (!"white".equals(color)) {
                                compiler.put(assets,
                                        "models/block/large_curtain/static/" + side + "/" + pose
                                                + "/" + color + "/" + quadrant + ".json",
                                        parentVariant(resourceId("models/block/large_curtain/static/"
                                                + side + "/" + pose + "/white/" + quadrant + ".json"), keys, color));
                            }
                        }
                    }
                }
            }
        }

        for (String side : SIDES) {
            String bonesPrefix = "models/block/large_curtain/animated/" + side + "/white/";
            for (String path : new ArrayList<>(assets.keySet())) {
                String prefix = bonesPrefix;
                if (!path.startsWith(prefix) || !path.endsWith(".json")) {
                    continue;
                }
                String boneFile = path.substring(prefix.length());
                JsonObject whiteBone = compiler.generated(assets, path).getAsJsonObject();
                List<String> keys = whiteTextureKeys(whiteBone);
                for (String color : COLORS) {
                    if (!"white".equals(color)) {
                        compiler.put(assets,
                                "models/block/large_curtain/animated/" + side + "/" + color + "/" + boneFile,
                                parentVariant(resourceId(path), keys, color));
                    }
                }
            }
        }

        for (String color : COLORS) {
            if (!"white".equals(color)) {
                JsonObject item = new JsonObject();
                item.addProperty("parent", MOD_ID + ":block/large_curtain/item/white");
                JsonObject textures = new JsonObject();
                textures.addProperty("1", MOD_ID + ":block/curtain/" + color);
                item.add("textures", textures);
                compiler.put(assets, "models/block/large_curtain/item/" + color + ".json", item);
            }
        }
    }

    private static JsonObject masterTextures() {
        JsonObject textures = new JsonObject();
        textures.addProperty("1", CURTAIN_WHITE_TEXTURE);
        textures.addProperty("0", CURTAIN_DECO_TEXTURE);
        textures.addProperty("particle", CURTAIN_WHITE_TEXTURE);
        return textures;
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
}