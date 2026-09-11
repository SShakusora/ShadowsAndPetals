package com.sshakusora.shadowsandpetals.data.irori;

import com.google.gson.*;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPart;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuts the existing BER grill meshes into block-local lower and upper assets.
 * The source models are deliberately kept hand-authored; only the clipped
 * derivatives are generated.
 */
public final class IroriGrillAssetCompiler {
    private static final double SPLIT_Y = 6.0D;
    private static final double SOURCE_MAX_Y = 11.5D;
    private static final double EPSILON = 1.0E-6D;

    private final Path sourceRoot;

    public IroriGrillAssetCompiler(Path sourceRoot) {
        this.sourceRoot = sourceRoot.resolve("src/main/resources/assets/shadowsandpetals/models/block/grill")
                .toAbsolutePath()
                .normalize();
    }

    public Map<String, JsonElement> compile() {
        Map<String, JsonElement> output = new LinkedHashMap<>();
        Map<String, JsonObject> sourceCache = new LinkedHashMap<>();
        for (IroriGrillPart part : IroriGrillPart.values()) {
            String sourceName = sourceName(part);
            JsonObject source = sourceCache.computeIfAbsent(sourceName, this::readSource);
            IroriGrillPart geometryPart = geometryPart(part);
            output.put(
                    "models/block/grill/double/" + part.modelName() + "_lower.json",
                    clipModel(source, geometryPart, true)
            );
            output.put(
                    "models/block/grill/double/" + part.modelName() + "_upper.json",
                    clipModel(source, geometryPart, false)
            );
        }
        return Map.copyOf(output);
    }

    /**
     * The old BER rotated the depth-oriented 1x2 mesh for an east/west strip.
     * Keep that rotation in the baked block-model state so Minecraft also
     * rotates the face winding and UVs.  West is the old south tile and east
     * is the old north tile after a clockwise quarter-turn.
     */
    private static IroriGrillPart geometryPart(IroriGrillPart part) {
        return switch (part) {
            case STRIP_WEST -> IroriGrillPart.STRIP_SOUTH;
            case STRIP_EAST -> IroriGrillPart.STRIP_NORTH;
            default -> part;
        };
    }

    private JsonObject readSource(String name) {
        Path path = sourceRoot.resolve(name + ".json");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw failure("source is not an object: " + path);
            }
            JsonObject source = parsed.getAsJsonObject();
            validateSource(source, path);
            return source;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalStateException illegalStateException
                    && illegalStateException.getMessage() != null
                    && illegalStateException.getMessage().startsWith("irori grill asset compiler:")) {
                throw illegalStateException;
            }
            throw failure("failed to read source: " + path + " (" + exception.getMessage() + ")");
        }
    }

    private static void validateSource(JsonObject source, Path path) {
        if (!source.has("elements") || !source.get("elements").isJsonArray()) {
            throw failure("missing elements array: " + path);
        }
        for (JsonElement elementValue : source.getAsJsonArray("elements")) {
            JsonObject element = elementValue.getAsJsonObject();
            if (!element.has("from") || !element.has("to")) {
                throw failure("element is missing from/to: " + path);
            }
            if (element.has("rotation") && hasNonZeroRotation(element.getAsJsonObject("rotation"))) {
                throw failure("rotated source elements are not supported: " + path);
            }
        }
    }

    private static boolean hasNonZeroRotation(JsonObject rotation) {
        if (rotation.has("angle") && Math.abs(rotation.get("angle").getAsDouble()) > EPSILON) {
            return true;
        }
        return number(rotation, "x") != 0.0D
                || number(rotation, "y") != 0.0D
                || number(rotation, "z") != 0.0D;
    }

    private static String sourceName(IroriGrillPart part) {
        return switch (part) {
            case SINGLE -> "1_1";
            case STRIP_NORTH, STRIP_SOUTH, STRIP_WEST, STRIP_EAST -> "1_2";
            case QUAD_NORTH_WEST, QUAD_NORTH_EAST, QUAD_SOUTH_WEST, QUAD_SOUTH_EAST -> "2_2";
        };
    }

    private static JsonObject clipModel(JsonObject source, IroriGrillPart part, boolean lower) {
        JsonObject result = new JsonObject();
        copyIfPresent(source, result, "format_version");
        copyIfPresent(source, result, "textures");
        copyIfPresent(source, result, "particle");

        JsonArray elements = new JsonArray();
        for (JsonElement elementValue : source.getAsJsonArray("elements")) {
            JsonObject clipped = clipElement(elementValue.getAsJsonObject(), part, lower);
            if (clipped != null) {
                elements.add(clipped);
            }
        }
        result.add("elements", elements);
        return result;
    }

    private static JsonObject clipElement(JsonObject source, IroriGrillPart part, boolean lower) {
        double[] from = vector(source.getAsJsonArray("from"));
        double[] to = vector(source.getAsJsonArray("to"));
        double x0;
        double x1;
        double z0;
        double z1;
        x0 = from[0];
        x1 = to[0];
        z0 = from[2];
        z1 = to[2];

        double layoutOffsetX = usesTwoWideLayout(part) ? 8.0D : 0.0D;
        double layoutOffsetZ = usesTwoDeepLayout(part) ? 8.0D : 0.0D;
        x0 += layoutOffsetX;
        x1 += layoutOffsetX;
        z0 += layoutOffsetZ;
        z1 += layoutOffsetZ;

        double tileMinX = part.offsetX() * 16.0D;
        double tileMaxX = tileMinX + 16.0D;
        double tileMinZ = part.offsetZ() * 16.0D;
        double tileMaxZ = tileMinZ + 16.0D;
        double clippedX0 = Math.max(x0, tileMinX);
        double clippedX1 = Math.min(x1, tileMaxX);
        double clippedZ0 = Math.max(z0, tileMinZ);
        double clippedZ1 = Math.min(z1, tileMaxZ);
        if (clippedX0 >= clippedX1 - EPSILON || clippedZ0 >= clippedZ1 - EPSILON) {
            return null;
        }

        double sourceY0 = from[1];
        double sourceY1 = to[1];
        double clipY0 = lower ? 0.0D : SPLIT_Y;
        double clipY1 = lower ? SPLIT_Y : SOURCE_MAX_Y;
        double clippedY0 = Math.max(sourceY0, clipY0);
        double clippedY1 = Math.min(sourceY1, clipY1);
        if (clippedY0 >= clippedY1 - EPSILON) {
            return null;
        }

        JsonObject result = new JsonObject();
        result.add("from", vector(clippedX0 - tileMinX, lower ? clippedY0 + 10.0D : clippedY0 - SPLIT_Y, clippedZ0 - tileMinZ));
        result.add("to", vector(clippedX1 - tileMinX, lower ? clippedY1 + 10.0D : clippedY1 - SPLIT_Y, clippedZ1 - tileMinZ));

        JsonObject sourceFaces = source.has("faces") ? source.getAsJsonObject("faces") : new JsonObject();
        JsonObject resultFaces = new JsonObject();
        for (Map.Entry<String, JsonElement> faceEntry : sourceFaces.entrySet()) {
            String originalFace = faceEntry.getKey();
            if (!shouldKeepFace(originalFace, from, to, x0, x1, z0, z1, clippedX0, clippedX1, clippedZ0, clippedZ1, clippedY0, clippedY1)) {
                continue;
            }
            String outputFace = originalFace;
            JsonObject face = faceEntry.getValue().getAsJsonObject().deepCopy();
            if (face.has("uv")) {
                face.add("uv", clippedUv(
                        face.getAsJsonArray("uv"),
                        originalFace,
                        false,
                        from,
                        to,
                        x0,
                        x1,
                        z0,
                        z1,
                        clippedX0,
                        clippedX1,
                        clippedZ0,
                        clippedZ1,
                        clippedY0,
                        clippedY1
                ));
            }
            if (face.has("cullface")) {
                face.addProperty("cullface", face.get("cullface").getAsString());
            }
            resultFaces.add(outputFace, face);
        }
        if (resultFaces.size() > 0) {
            result.add("faces", resultFaces);
        }
        return result;
    }

    private static boolean shouldKeepFace(
            String face,
            double[] sourceFrom,
            double[] sourceTo,
            double x0,
            double x1,
            double z0,
            double z1,
            double clippedX0,
            double clippedX1,
            double clippedZ0,
            double clippedZ1,
            double clippedY0,
            double clippedY1
    ) {
        return switch (face) {
            case "up" -> nearlyEqual(sourceTo[1], clippedY1);
            case "down" -> nearlyEqual(sourceFrom[1], clippedY0);
            case "north" -> nearlyEqual(z0, clippedZ0) || nearlyEqual(z0, clippedZ1);
            case "south" -> nearlyEqual(z1, clippedZ0) || nearlyEqual(z1, clippedZ1);
            case "west" -> nearlyEqual(x0, clippedX0) || nearlyEqual(x0, clippedX1);
            case "east" -> nearlyEqual(x1, clippedX0) || nearlyEqual(x1, clippedX1);
            default -> true;
        };
    }

    private static JsonArray clippedUv(
            JsonArray sourceUv,
            String originalFace,
            boolean rotate,
            double[] sourceFrom,
            double[] sourceTo,
            double x0,
            double x1,
            double z0,
            double z1,
            double clippedX0,
            double clippedX1,
            double clippedZ0,
            double clippedZ1,
            double clippedY0,
            double clippedY1
    ) {
        double uAxisMin;
        double uAxisMax;
        double uClipMin;
        double uClipMax;
        double vAxisMin;
        double vAxisMax;
        double vClipMin;
        double vClipMax;

        if ("up".equals(originalFace) || "down".equals(originalFace)) {
            if (rotate) {
                uAxisMin = z0;
                uAxisMax = z1;
                uClipMin = clippedZ0;
                uClipMax = clippedZ1;
                vAxisMin = x0;
                vAxisMax = x1;
                vClipMin = clippedX0;
                vClipMax = clippedX1;
            } else {
                uAxisMin = x0;
                uAxisMax = x1;
                uClipMin = clippedX0;
                uClipMax = clippedX1;
                vAxisMin = z0;
                vAxisMax = z1;
                vClipMin = clippedZ0;
                vClipMax = clippedZ1;
            }
        } else if (("north".equals(originalFace) || "south".equals(originalFace))) {
            uAxisMin = rotate ? z0 : x0;
            uAxisMax = rotate ? z1 : x1;
            uClipMin = rotate ? clippedZ0 : clippedX0;
            uClipMax = rotate ? clippedZ1 : clippedX1;
            vAxisMin = sourceFrom[1];
            vAxisMax = sourceTo[1];
            vClipMin = clippedY0;
            vClipMax = clippedY1;
        } else {
            uAxisMin = rotate ? x0 : z0;
            uAxisMax = rotate ? x1 : z1;
            uClipMin = rotate ? clippedX0 : clippedZ0;
            uClipMax = rotate ? clippedX1 : clippedZ1;
            vAxisMin = sourceFrom[1];
            vAxisMax = sourceTo[1];
            vClipMin = clippedY0;
            vClipMax = clippedY1;
        }

        double u0 = sourceUv.get(0).getAsDouble();
        double v0 = sourceUv.get(1).getAsDouble();
        double u1 = sourceUv.get(2).getAsDouble();
        double v1 = sourceUv.get(3).getAsDouble();
        double uFraction0 = fraction(uClipMin, uAxisMin, uAxisMax);
        double uFraction1 = fraction(uClipMax, uAxisMin, uAxisMax);
        double vFraction0 = fraction(vClipMin, vAxisMin, vAxisMax);
        double vFraction1 = fraction(vClipMax, vAxisMin, vAxisMax);

        JsonArray result = new JsonArray();
        result.add(number(u0, u1, uFraction0));
        result.add(number(v0, v1, vFraction0));
        result.add(number(u0, u1, uFraction1));
        result.add(number(v0, v1, vFraction1));
        return result;
    }

    private static double fraction(double value, double min, double max) {
        if (Math.abs(max - min) < EPSILON) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, (value - min) / (max - min)));
    }

    private static boolean usesTwoWideLayout(IroriGrillPart part) {
        return part == IroriGrillPart.STRIP_WEST
                || part == IroriGrillPart.STRIP_EAST
                || part.name().startsWith("QUAD_");
    }

    private static boolean usesTwoDeepLayout(IroriGrillPart part) {
        return part == IroriGrillPart.STRIP_NORTH
                || part == IroriGrillPart.STRIP_SOUTH
                || part.name().startsWith("QUAD_");
    }

    private static double[] vector(JsonArray array) {
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(clean(x)));
        array.add(new JsonPrimitive(clean(y)));
        array.add(new JsonPrimitive(clean(z)));
        return array;
    }

    private static double clean(double value) {
        return Math.abs(value - Math.rint(value)) < EPSILON ? Math.rint(value) : value;
    }

    private static JsonPrimitive number(double start, double end, double fraction) {
        return new JsonPrimitive(clean(start + (end - start) * fraction));
    }

    private static double number(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsDouble() : 0.0D;
    }

    private static boolean nearlyEqual(double first, double second) {
        return Math.abs(first - second) < EPSILON;
    }

    private static void copyIfPresent(JsonObject source, JsonObject target, String key) {
        if (source.has(key)) {
            target.add(key, source.get(key).deepCopy());
        }
    }

    private static IllegalStateException failure(String message) {
        return new IllegalStateException("irori grill asset compiler: " + message);
    }
}