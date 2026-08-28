package com.sshakusora.shadowsandpetals.data;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.rockery.obj.*;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModRockeryModelProvider implements DataProvider {
    private static final Pattern SOURCE_MODEL_NAME = Pattern.compile("^(\\d+)_(\\d+)_(\\d+)\\.(json|bbmodel|obj)$");
    private static final String PARTICLE_TEXTURE = Identifier.withDefaultNamespace("block/stone").toString();
    private static final String OBJ_PARENT = ShadowsAndPetals.MOD_ID + ":block/rock/obj_parent";
    private static final String OBJ_OUTLINE_KEY = ShadowsAndPetals.MOD_ID + ":outline";
    private static final String OBJ_COLLISION_KEY = ShadowsAndPetals.MOD_ID + ":collision";
    private static final double BLOCK_SIZE = 16.0D;
    private static final double EPSILON = 1.0E-6D;

    private final PackOutput.PathProvider modelPathProvider;
    private final Path sourceDir;

    public ModRockeryModelProvider(PackOutput output) {
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.sourceDir = output.getOutputFolder()
                .toAbsolutePath()
                .normalize()
                .getParent()
                .getParent()
                .resolve("main")
                .resolve("resources")
                .resolve("assets")
                .resolve(ShadowsAndPetals.MOD_ID)
                .resolve("models")
                .resolve("block")
                .resolve("rock");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        for (SourceModel source : findSourceModels()) {
            if (source.extension().equals("obj")) {
                tasks.addAll(generateObjParts(cache, source, readObjModel(source)));
                continue;
            }
            RockeryModel model = readSourceModel(source);
            for (int x = 0; x < source.width(); x++) {
                for (int y = 0; y < source.height(); y++) {
                    for (int z = 0; z < source.depth(); z++) {
                        JsonObject partModel = cutPart(model, x, y, z);
                        Identifier modelId = ShadowsAndPetals.asResource(
                                "block/rock/" + source.width() + "x" + source.height() + "x" + source.depth()
                                        + "/" + x + "_" + y + "_" + z);
                        tasks.add(DataProvider.saveStable(cache, partModel, this.modelPathProvider.json(modelId)));
                    }
                }
            }
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Rockery Models";
    }

    private List<SourceModel> findSourceModels() {
        if (!Files.isDirectory(this.sourceDir)) {
            return List.of();
        }

        try (var paths = Files.list(this.sourceDir)) {
            List<SourceModel> models = paths
                    .filter(Files::isRegularFile)
                    .map(this::sourceModel)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(SourceModel::path))
                    .toList();
            Map<String, SourceModel> byDimensions = new LinkedHashMap<>();
            for (SourceModel model : models) {
                String dimensions = model.width() + "x" + model.height() + "x" + model.depth();
                SourceModel previous = byDimensions.putIfAbsent(dimensions, model);
                if (previous != null) {
                    throw new IllegalStateException(
                            "Multiple rockery source models for " + dimensions + ": "
                                    + previous.path() + " and " + model.path()
                    );
                }
            }
            return models;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan rockery source models in " + this.sourceDir, e);
        }
    }

    private SourceModel sourceModel(Path path) {
        Matcher matcher = SOURCE_MODEL_NAME.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            return null;
        }
        return new SourceModel(
                path,
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                matcher.group(4).toLowerCase(Locale.ROOT)
        );
    }

    private RockeryModel readSourceModel(SourceModel source) {
        if (source.extension().equals("obj")) {
            throw new IllegalArgumentException("OBJ source models must use the OBJ generation path: " + source.path());
        }
        try (Reader reader = Files.newBufferedReader(source.path())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return switch (source.extension()) {
                case "json" -> readVanillaModel(json);
                case "bbmodel" -> readBlockbenchModel(source, json);
                default -> throw new IllegalStateException("Unsupported rockery model extension: " + source.path());
            };
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read rockery source model " + source.path(), e);
        }
    }

    private ObjModel readObjModel(SourceModel source) {
        try {
            return ObjModelParser.parse(source.path());
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to read OBJ rockery model " + source.path(), e);
        }
    }

    private List<CompletableFuture<?>> generateObjParts(
            CachedOutput cache,
            SourceModel source,
            ObjModel model
    ) {
        ObjModelCutter.CutModel cut = ObjModelCutter.cut(
                model,
                source.width(),
                source.height(),
                source.depth()
        );
        List<ObjModelOutline.Cuboid> outlineCuboids = ObjModelOutline.extract(cut.normalizedModel());
        String modelDirectory = source.width() + "x" + source.height() + "x" + source.depth();
        List<CompletableFuture<?>> tasks = new ArrayList<>();

        Identifier materialId = ShadowsAndPetals.asResource("block/rock/" + modelDirectory + "/material");
        tasks.add(writeBytes(cache, rawPath(modelPathProvider.json(materialId), ".mtl"), ObjModelWriter.writeMtl()));

        for (ObjModelCutter.Part part : cut.parts()) {
            String partName = part.x() + "_" + part.y() + "_" + part.z();
            Identifier modelId = ShadowsAndPetals.asResource("block/rock/" + modelDirectory + "/" + partName);
            Path jsonPath = modelPathProvider.json(modelId);
            tasks.add(writeBytes(cache, rawPath(jsonPath, ".obj"), ObjModelWriter.writeObj(part)));
            tasks.add(DataProvider.saveStable(
                    cache,
                    objWrapper(source, modelDirectory, partName, part, outlineCuboids),
                    jsonPath
            ));
        }

        return tasks;
    }

    private JsonObject objWrapper(
            SourceModel source,
            String modelDirectory,
            String partName,
            ObjModelCutter.Part part,
            List<ObjModelOutline.Cuboid> outlineCuboids
    ) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", OBJ_PARENT);
        model.addProperty("loader", "neoforge:obj");
        model.addProperty(
                "model",
                ShadowsAndPetals.asResource(
                        "models/block/rock/" + modelDirectory + "/" + partName + ".obj"
                ).toString()
        );
        model.addProperty("flip_v", true);
        model.addProperty("automatic_culling", false);
        model.addProperty("shade_quads", true);

        JsonObject textures = new JsonObject();
        textures.addProperty("texture", ShadowsAndPetals.asResource(
                "block/rock/" + source.width() + "_" + source.height() + "_" + source.depth()
        ).toString());
        textures.addProperty("particle", PARTICLE_TEXTURE);
        model.add("textures", textures);

        JsonArray collision = new JsonArray();
        for (ObjModel.Bounds bounds : part.collisionBoxes()) {
            JsonObject box = new JsonObject();
            box.add("from", vector(
                    bounds.minX() * BLOCK_SIZE,
                    bounds.minY() * BLOCK_SIZE,
                    bounds.minZ() * BLOCK_SIZE
            ));
            box.add("to", vector(
                    bounds.maxX() * BLOCK_SIZE,
                    bounds.maxY() * BLOCK_SIZE,
                    bounds.maxZ() * BLOCK_SIZE
            ));
            collision.add(box);
        }
        model.add(OBJ_COLLISION_KEY, collision);
        if (part.x() == 0 && part.y() == 0 && part.z() == 0) {
            model.add(OBJ_OUTLINE_KEY, outlineMetadata(outlineCuboids));
        }
        return model;
    }

    private JsonObject outlineMetadata(List<ObjModelOutline.Cuboid> cuboids) {
        JsonObject outline = new JsonObject();
        outline.addProperty("space", "structure");
        JsonArray cuboidArray = new JsonArray();
        for (ObjModelOutline.Cuboid cuboid : cuboids) {
            JsonObject serialized = new JsonObject();
            JsonArray vertices = new JsonArray();
            for (ObjModel.ObjVector3 vertex : cuboid.vertices()) {
                vertices.add(vector(
                        vertex.x() * BLOCK_SIZE,
                        vertex.y() * BLOCK_SIZE,
                        vertex.z() * BLOCK_SIZE
                ));
            }
            serialized.add("vertices", vertices);

            JsonArray faces = new JsonArray();
            for (ObjModelOutline.Face face : cuboid.faces()) {
                JsonArray indices = new JsonArray();
                indices.add(face.first());
                indices.add(face.second());
                indices.add(face.third());
                indices.add(face.fourth());
                faces.add(indices);
            }
            serialized.add("faces", faces);

            JsonArray edges = new JsonArray();
            for (ObjModelOutline.Edge edge : cuboid.edges()) {
                JsonArray indices = new JsonArray();
                indices.add(edge.first());
                indices.add(edge.second());
                edges.add(indices);
            }
            serialized.add("edges", edges);
            cuboidArray.add(serialized);
        }
        outline.add("cuboids", cuboidArray);
        return outline;
    }

    private static CompletableFuture<?> writeBytes(CachedOutput cache, Path path, byte[] bytes) {
        return CompletableFuture.runAsync(() -> {
            try {
                cache.writeIfNeeded(path, bytes, Hashing.sha256().hashBytes(bytes));
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static Path rawPath(Path jsonPath, String extension) {
        String fileName = jsonPath.getFileName().toString();
        if (!fileName.endsWith(".json")) {
            throw new IllegalArgumentException("Expected a JSON model path: " + jsonPath);
        }
        return jsonPath.resolveSibling(fileName.substring(0, fileName.length() - ".json".length()) + extension);
    }

    private RockeryModel readVanillaModel(JsonObject json) {
        JsonObject root = new JsonObject();
        json.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("elements"))
                .forEach(entry -> root.add(entry.getKey(), entry.getValue().deepCopy()));
        useStoneParticle(root);
        return new RockeryModel(root, json.getAsJsonArray("elements"));
    }

    private RockeryModel readBlockbenchModel(SourceModel source, JsonObject json) {
        JsonObject root = new JsonObject();
        TextureSize textureSize = readBlockbenchTextureSize(json);
        root.add("texture_size", textureSize.toJson());

        String textureKey = textureKey(source);
        JsonObject textures = new JsonObject();
        String texturePath = ShadowsAndPetals.asResource("block/rock/"
                + source.width() + "_" + source.height() + "_" + source.depth()).toString();
        textures.addProperty("particle", PARTICLE_TEXTURE);
        textures.addProperty(textureKey, texturePath);
        root.add("textures", textures);

        JsonArray elements = new JsonArray();
        double xOffset = source.width() * BLOCK_SIZE / 2.0D;
        double zOffset = source.depth() * BLOCK_SIZE / 2.0D;
        for (JsonElement element : json.getAsJsonArray("elements")) {
            JsonObject converted = convertBlockbenchElement(element.getAsJsonObject(), textureKey, textureSize, xOffset, zOffset);
            if (converted != null) {
                elements.add(converted);
            }
        }
        return new RockeryModel(root, elements);
    }

    private TextureSize readBlockbenchTextureSize(JsonObject json) {
        if (json.has("resolution")) {
            JsonObject resolution = json.getAsJsonObject("resolution");
            return new TextureSize(resolution.get("width").getAsInt(), resolution.get("height").getAsInt());
        }

        if (json.has("textures") && json.get("textures").isJsonArray()) {
            for (JsonElement textureElement : json.getAsJsonArray("textures")) {
                JsonObject texture = textureElement.getAsJsonObject();
                if (texture.has("uv_width") && texture.has("uv_height")) {
                    return new TextureSize(texture.get("uv_width").getAsInt(), texture.get("uv_height").getAsInt());
                }
                if (texture.has("width") && texture.has("height")) {
                    return new TextureSize(texture.get("width").getAsInt(), texture.get("height").getAsInt());
                }
            }
        }

        return new TextureSize(16, 16);
    }

    private JsonObject convertBlockbenchElement(JsonObject source, String textureKey, TextureSize textureSize, double xOffset, double zOffset) {
        if (source.has("export") && !source.get("export").getAsBoolean()) {
            return null;
        }
        if (source.has("type") && !source.get("type").getAsString().equals("cube")) {
            return null;
        }
        if (!source.has("from") || !source.has("to") || !source.has("faces")
                || !source.get("faces").isJsonObject()) {
            return null;
        }

        JsonObject element = new JsonObject();
        element.add("from", shiftedBlockbenchVector(source.getAsJsonArray("from"), xOffset, zOffset));
        element.add("to", shiftedBlockbenchVector(source.getAsJsonArray("to"), xOffset, zOffset));
        if (source.has("rotation")) {
            element.add("rotation", convertBlockbenchRotation(source, xOffset, zOffset));
        }
        if (source.has("shade") && !source.get("shade").getAsBoolean()) {
            element.addProperty("shade", false);
        }
        copyIfPresent(source, element, "light_emission");

        JsonObject faces = new JsonObject();
        JsonObject sourceFaces = source.getAsJsonObject("faces");
        for (String direction : List.of("north", "east", "south", "west", "up", "down")) {
            if (!sourceFaces.has(direction)) {
                continue;
            }
            JsonObject face = sourceFaces.getAsJsonObject(direction);
            if (!face.has("texture") || face.get("texture").isJsonNull()) {
                continue;
            }
            JsonObject convertedFace = new JsonObject();
            if (face.has("uv")) {
                convertedFace.add("uv", scaleBlockbenchUv(face.getAsJsonArray("uv"), textureSize));
            }
            copyIfPresent(face, convertedFace, "rotation");
            copyIfPresent(face, convertedFace, "cullface");
            copyIfPresent(face, convertedFace, "tintindex");
            if (face.has("tint") && face.get("tint").getAsInt() >= 0) {
                convertedFace.addProperty("tintindex", face.get("tint").getAsInt());
            }
            convertedFace.addProperty("texture", "#" + textureKey);
            faces.add(direction, convertedFace);
        }
        element.add("faces", faces);
        return element;
    }

    private static JsonArray scaleBlockbenchUv(JsonArray uv, TextureSize textureSize) {
        JsonArray scaled = new JsonArray();
        scaled.add(trimNumber(uv.get(0).getAsDouble() * 16.0D / textureSize.width()));
        scaled.add(trimNumber(uv.get(1).getAsDouble() * 16.0D / textureSize.height()));
        scaled.add(trimNumber(uv.get(2).getAsDouble() * 16.0D / textureSize.width()));
        scaled.add(trimNumber(uv.get(3).getAsDouble() * 16.0D / textureSize.height()));
        return scaled;
    }

    private JsonObject convertBlockbenchRotation(JsonObject source, double xOffset, double zOffset) {
        JsonObject rotation = new JsonObject();
        JsonArray angles = source.getAsJsonArray("rotation");
        double x = angles.get(0).getAsDouble();
        double y = angles.get(1).getAsDouble();
        double z = angles.get(2).getAsDouble();
        if (Math.abs(x) > EPSILON) {
            rotation.addProperty("angle", trimNumber(x));
            rotation.addProperty("axis", "x");
        } else if (Math.abs(y) > EPSILON) {
            rotation.addProperty("angle", trimNumber(y));
            rotation.addProperty("axis", "y");
        } else {
            rotation.addProperty("angle", trimNumber(z));
            rotation.addProperty("axis", "z");
        }
        if (source.has("origin")) {
            rotation.add("origin", shiftedBlockbenchVector(source.getAsJsonArray("origin"), xOffset, zOffset));
        }
        return rotation;
    }

    private static JsonArray shiftedBlockbenchVector(JsonArray source, double xOffset, double zOffset) {
        return vector(
                source.get(0).getAsDouble() + xOffset,
                source.get(1).getAsDouble(),
                source.get(2).getAsDouble() + zOffset
        );
    }

    private static String textureKey(SourceModel source) {
        return "rockery" + source.width() + "x" + source.height() + "x" + source.depth();
    }

    private JsonObject cutPart(RockeryModel model, int partX, int partY, int partZ) {
        JsonObject root = model.root().deepCopy();
        JsonArray elements = new JsonArray();
        Bounds cell = Bounds.cell(partX, partY, partZ);

        for (JsonElement element : model.elements()) {
            JsonObject cut = cutElement(element.getAsJsonObject(), cell);
            if (cut != null) {
                elements.add(cut);
            }
        }

        root.add("elements", elements);
        useStoneParticle(root);
        return root;
    }

    private static void useStoneParticle(JsonObject model) {
        JsonObject textures;
        if (model.has("textures") && model.get("textures").isJsonObject()) {
            textures = model.getAsJsonObject("textures");
        } else {
            textures = new JsonObject();
            model.add("textures", textures);
        }
        textures.addProperty("particle", PARTICLE_TEXTURE);
    }

    private JsonObject cutElement(JsonObject source, Bounds cell) {
        Bounds bounds = Bounds.fromElement(source);
        Bounds clipped = bounds.intersection(cell);
        if (clipped == null) {
            return null;
        }

        JsonObject element = source.deepCopy();
        element.add("from", vector(clipped.minX() - cell.minX(), clipped.minY() - cell.minY(), clipped.minZ() - cell.minZ()));
        element.add("to", vector(clipped.maxX() - cell.minX(), clipped.maxY() - cell.minY(), clipped.maxZ() - cell.minZ()));
        if (element.has("rotation")) {
            shiftRotationOrigin(element.getAsJsonObject("rotation"), cell);
        }

        JsonObject faces = new JsonObject();
        JsonObject sourceFaces = source.getAsJsonObject("faces");
        for (FaceDirection direction : FaceDirection.values()) {
            if (!sourceFaces.has(direction.serializedName())) {
                continue;
            }
            if (!direction.isOnOriginalSurface(bounds, clipped)) {
                continue;
            }

            JsonObject face = sourceFaces.getAsJsonObject(direction.serializedName()).deepCopy();
            if (face.has("uv")) {
                face.add("uv", cropUv(face.getAsJsonArray("uv"), bounds, clipped, direction));
            }
            faces.add(direction.serializedName(), face);
        }

        element.add("faces", faces);
        return faces.size() == 0 ? null : element;
    }

    private static JsonArray cropUv(JsonArray uv, Bounds source, Bounds clipped, FaceDirection direction) {
        double u0 = uv.get(0).getAsDouble();
        double v0 = uv.get(1).getAsDouble();
        double u1 = uv.get(2).getAsDouble();
        double v1 = uv.get(3).getAsDouble();

        FaceUv sourceUv = FaceUv.fromBounds(source, direction);
        FaceUv clippedUv = FaceUv.fromBounds(clipped, direction);
        double uStart = fraction(sourceUv.minU(), sourceUv.maxU(), clippedUv.minU());
        double uEnd = fraction(sourceUv.minU(), sourceUv.maxU(), clippedUv.maxU());
        double vStart = fraction(sourceUv.minV(), sourceUv.maxV(), clippedUv.minV());
        double vEnd = fraction(sourceUv.minV(), sourceUv.maxV(), clippedUv.maxV());

        JsonArray cropped = new JsonArray();
        cropped.add(trimNumber(lerp(u0, u1, uStart)));
        cropped.add(trimNumber(lerp(v0, v1, vStart)));
        cropped.add(trimNumber(lerp(u0, u1, uEnd)));
        cropped.add(trimNumber(lerp(v0, v1, vEnd)));
        return cropped;
    }

    private static double fraction(double min, double max, double value) {
        if (Math.abs(max - min) < EPSILON) {
            return 0.0D;
        }
        return (value - min) / (max - min);
    }

    private static double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    private static void shiftRotationOrigin(JsonObject rotation, Bounds cell) {
        if (!rotation.has("origin")) {
            return;
        }

        double[] origin = vector(rotation.getAsJsonArray("origin"));
        rotation.add("origin", vector(origin[0] - cell.minX(), origin[1] - cell.minY(), origin[2] - cell.minZ()));
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(trimNumber(x));
        array.add(trimNumber(y));
        array.add(trimNumber(z));
        return array;
    }

    private static double[] vector(JsonArray array) {
        return new double[]{
                array.get(0).getAsDouble(),
                array.get(1).getAsDouble(),
                array.get(2).getAsDouble()
        };
    }

    private static Number trimNumber(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < EPSILON) {
            return (int) rounded;
        }
        return value;
    }

    private static void copyIfPresent(JsonObject source, JsonObject target, String key) {
        if (source.has(key)) {
            target.add(key, source.get(key).deepCopy());
        }
    }

    private enum Axis {
        X,
        Y,
        Z
    }

    private enum FaceDirection {
        NORTH("north", Axis.Z, false),
        EAST("east", Axis.X, true),
        SOUTH("south", Axis.Z, true),
        WEST("west", Axis.X, false),
        UP("up", Axis.Y, true),
        DOWN("down", Axis.Y, false);

        private final String serializedName;
        private final Axis normalAxis;
        private final boolean positive;

        FaceDirection(String serializedName, Axis normalAxis, boolean positive) {
            this.serializedName = serializedName;
            this.normalAxis = normalAxis;
            this.positive = positive;
        }

        private String serializedName() {
            return this.serializedName;
        }

        private boolean isOnOriginalSurface(Bounds source, Bounds clipped) {
            if (this.positive) {
                return same(clipped.max(this.normalAxis), source.max(this.normalAxis));
            }
            return same(clipped.min(this.normalAxis), source.min(this.normalAxis));
        }
    }

    private record FaceUv(double minU, double minV, double maxU, double maxV) {
        private static FaceUv fromBounds(Bounds bounds, FaceDirection direction) {
            return switch (direction) {
                case DOWN -> new FaceUv(bounds.minX(), 16.0D - bounds.maxZ(), bounds.maxX(), 16.0D - bounds.minZ());
                case UP -> new FaceUv(bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
                case NORTH -> new FaceUv(16.0D - bounds.maxX(), 16.0D - bounds.maxY(), 16.0D - bounds.minX(), 16.0D - bounds.minY());
                case SOUTH -> new FaceUv(bounds.minX(), 16.0D - bounds.maxY(), bounds.maxX(), 16.0D - bounds.minY());
                case WEST -> new FaceUv(bounds.minZ(), 16.0D - bounds.maxY(), bounds.maxZ(), 16.0D - bounds.minY());
                case EAST -> new FaceUv(16.0D - bounds.maxZ(), 16.0D - bounds.maxY(), 16.0D - bounds.minZ(), 16.0D - bounds.minY());
            };
        }
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private static Bounds fromElement(JsonObject element) {
            double[] from = vector(element.getAsJsonArray("from"));
            double[] to = vector(element.getAsJsonArray("to"));
            return new Bounds(from[0], from[1], from[2], to[0], to[1], to[2]);
        }

        private static Bounds cell(int x, int y, int z) {
            return new Bounds(
                    x * BLOCK_SIZE,
                    y * BLOCK_SIZE,
                    z * BLOCK_SIZE,
                    (x + 1) * BLOCK_SIZE,
                    (y + 1) * BLOCK_SIZE,
                    (z + 1) * BLOCK_SIZE
            );
        }

        private Bounds intersection(Bounds other) {
            Bounds intersection = new Bounds(
                    Math.max(this.minX, other.minX),
                    Math.max(this.minY, other.minY),
                    Math.max(this.minZ, other.minZ),
                    Math.min(this.maxX, other.maxX),
                    Math.min(this.maxY, other.maxY),
                    Math.min(this.maxZ, other.maxZ)
            );
            if (intersection.maxX - intersection.minX <= EPSILON
                    || intersection.maxY - intersection.minY <= EPSILON
                    || intersection.maxZ - intersection.minZ <= EPSILON) {
                return null;
            }
            return intersection;
        }

        private double min(Axis axis) {
            return switch (axis) {
                case X -> this.minX;
                case Y -> this.minY;
                case Z -> this.minZ;
            };
        }

        private double max(Axis axis) {
            return switch (axis) {
                case X -> this.maxX;
                case Y -> this.maxY;
                case Z -> this.maxZ;
            };
        }
    }

    private record SourceModel(Path path, int width, int height, int depth, String extension) {
    }

    private record RockeryModel(JsonObject root, JsonArray elements) {
    }

    private record TextureSize(int width, int height) {
        private JsonArray toJson() {
            JsonArray size = new JsonArray();
            size.add(this.width);
            size.add(this.height);
            return size;
        }
    }

    private static boolean same(double left, double right) {
        return Math.abs(left - right) < EPSILON;
    }
}
