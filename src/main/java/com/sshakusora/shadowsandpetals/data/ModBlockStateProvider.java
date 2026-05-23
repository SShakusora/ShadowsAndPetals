package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModBlockStateProvider implements DataProvider {
    private final PackOutput.PathProvider blockStatePathProvider;
    private final PackOutput.PathProvider modelPathProvider;
    private final Map<Identifier, JsonObject> blockStates = new LinkedHashMap<>();
    private final Map<Identifier, JsonObject> models = new LinkedHashMap<>();
    private final Models modelsHelper = new Models();

    public ModBlockStateProvider(PackOutput output) {
        this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.blockStates.clear();
        this.models.clear();
        registerStatesAndModels();

        CompletableFuture<?> blockStateTask = DataProvider.saveAll(cache, json -> json, this.blockStatePathProvider::json, this.blockStates);
        CompletableFuture<?> modelTask = DataProvider.saveAll(cache, json -> json, this.modelPathProvider::json, this.models);
        return CompletableFuture.allOf(blockStateTask, modelTask);
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Block States";
    }

    protected void registerStatesAndModels() {
        for (var generator : DatagenBlockStateRegistry.generators()) {
            generator.accept(this);
        }
    }

    public Identifier modLoc(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    public Identifier mcLoc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    public Models models() {
        return this.modelsHelper;
    }

    public void simpleBlockWithItem(Block block, ModelRef model) {
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    public void simpleBlock(Block block, ModelRef model) {
        JsonObject variants = new JsonObject();
        JsonObject state = new JsonObject();
        state.addProperty("model", model.id().toString());
        variants.add("", state);

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
    }

    public void simpleBlockItem(Block block, ModelRef model) {
        this.models.put(itemModelId(block), parentModel(model.id()));
    }

    public void leavesBlockWithItem(LeavesBlock block) {
        cubeAllBlockWithItem(block, modLoc("block/" + name(block)));
    }

    public void leavesBlockWithItem(LeavesBlock block, Identifier texture) {
        cubeAllBlockWithItem(block, texture);
    }

    public void cubeAllBlockWithItem(Block block) {
        cubeAllBlockWithItem(block, modLoc("block/" + name(block)));
    }

    public void cubeAllBlockWithItem(Block block, Identifier texture) {
        Identifier modelId = blockModelId(block);
        this.models.put(modelId, cubeAllModel(texture));
        simpleBlockWithItem(block, new ModelRef(modelId));
    }

    public void axisBlockWithItem(RotatedPillarBlock block, Identifier sideTexture, Identifier endTexture) {
        Identifier modelId = blockModelId(block);
        this.models.put(modelId, cubeColumnModel(sideTexture, endTexture));

        JsonObject variants = new JsonObject();
        variants.add(RotatedPillarBlock.AXIS.getName() + "=x", rotatedModel(modelId, 90, 90));
        variants.add(RotatedPillarBlock.AXIS.getName() + "=y", modelRef(modelId));
        variants.add(RotatedPillarBlock.AXIS.getName() + "=z", rotatedModel(modelId, 90, 0));

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), parentModel(modelId));
    }

    public void woodPostBlockWithItem(WoodPostBlock block, Identifier sideTexture, Identifier endTexture) {
        Identifier coreModel = blockModelId(block);
        Identifier lowerLinkModel = modLoc(coreModel.getPath() + "_link");
        Identifier upperLinkModel = modLoc(coreModel.getPath() + "_link_top");

        this.models.put(coreModel, woodPostCoreModel(sideTexture, endTexture));
        this.models.put(lowerLinkModel, woodPostLinkModel(sideTexture, endTexture, false));
        this.models.put(upperLinkModel, woodPostLinkModel(sideTexture, endTexture, true));

        for (WoodPostBlock.ConnectionType type : WoodPostBlock.ConnectionType.values()) {
            if (!type.isChain()) {
                continue;
            }

            Identifier lowerChainModel = chainModelId(type, false);
            Identifier upperChainModel = chainModelId(type, true);
            this.models.put(lowerChainModel, woodPostChainModel(false, type.texture()));
            this.models.put(upperChainModel, woodPostChainModel(true, type.texture()));
        }

        JsonObject variants = new JsonObject();
        variants.add("axis=y", rotatedModel(coreModel, 0, 0));
        variants.add("axis=x", rotatedModel(coreModel, 90, 90));
        variants.add("axis=z", rotatedModel(coreModel, 90, 0));

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), parentModel(coreModel));
    }

    public void saplingBlock(SaplingBlock block) {
        saplingBlock(block, modLoc("block/" + name(block)));
    }

    public void saplingBlock(SaplingBlock block, Identifier texture) {
        Identifier modelId = blockModelId(block);
        this.models.put(modelId, crossModel(texture));
        simpleBlock(block, new ModelRef(modelId));
    }

    public void hedgeBlockWithItem(HedgeBlock block, Identifier texture) {
        Identifier inventoryModel = blockModelId(block);
        Identifier straightModel = modLoc("block/" + name(block) + "_5");

        JsonObject variants = new JsonObject();
        for (int mask = 0; mask < 16; mask++) {
            boolean north = (mask & 1) != 0;
            boolean east = (mask & 1 << 1) != 0;
            boolean south = (mask & 1 << 2) != 0;
            boolean west = (mask & 1 << 3) != 0;
            Identifier modelId = modLoc("block/" + name(block) + "_" + mask);
            this.models.put(modelId, hedgeStateModel(texture, north, east, south, west));
            variants.add(hedgeVariantKey(north, east, south, west, false), modelRef(modelId));
            variants.add(hedgeVariantKey(north, east, south, west, true), modelRef(modelId));
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), parentModel(straightModel));
    }

    public void ingotPileBlock(IngotPileBlock block) {
        String blockName = name(block);
        String metalName = blockName.endsWith("_ingot_pile")
                ? blockName.substring(0, blockName.length() - "_ingot_pile".length())
                : blockName;

        JsonObject variants = new JsonObject();
        for (SlabType type : SlabType.values()) {
            boolean isDouble = type == SlabType.DOUBLE;
            String modelPath = "block/ingot_pile/" + metalName + (isDouble ? "_double" : "_bottom");
            Identifier modelId = modLoc(modelPath);

            variants.add("axis=x,type=" + type.getSerializedName(), modelRef(modelId));
            variants.add("axis=z,type=" + type.getSerializedName(), rotatedModel(modelId, 0, 90));
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), parentModel(modLoc("block/ingot_pile/" + metalName + "_bottom")));
    }

    public void vanityBlock(VanityBlock block) {
        String blockName = name(block);
        String woodName = blockName.endsWith("_vanity")
                ? blockName.substring(0, blockName.length() - "_vanity".length())
                : blockName;

        Identifier lowerModel = modLoc("block/vanity/" + woodName + "_lower");
        Identifier upperModel = modLoc("block/vanity/" + woodName + "_upper");

        JsonObject variants = new JsonObject();
        for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
            Identifier model = half == DoubleBlockHalf.LOWER ? lowerModel : upperModel;
            for (boolean waterlogged : new boolean[]{false, true}) {
                int i = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    addVanityVariant(variants, half, waterlogged, dir, model, i * 90);
                    i++;
                }
            }
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
    }

    public void iroriBlock(IroriBlock block) {
        Identifier basePath = modLoc("block/irori/block");
        JsonObject variants = new JsonObject();

        for (boolean shiftPlaced : new boolean[]{false, true}) {
            for (boolean waterlogged : new boolean[]{false, true}) {
                for (boolean north : new boolean[]{false, true}) {
                    for (boolean east : new boolean[]{false, true}) {
                        for (boolean south : new boolean[]{false, true}) {
                            for (boolean west : new boolean[]{false, true}) {
                                addIroriVariant(variants, north, east, south, west, waterlogged, shiftPlaced);
                            }
                        }
                    }
                }
            }
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), parentModel(basePath));
    }

    private String name(Block block) {
        return id(block).getPath();
    }

    private Identifier id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    private Identifier blockModelId(Block block) {
        return modLoc("block/" + name(block));
    }

    private Identifier itemModelId(Block block) {
        return modLoc("item/" + name(block));
    }

    private static JsonObject modelRef(Identifier modelId) {
        JsonObject json = new JsonObject();
        json.addProperty("model", modelId.toString());
        return json;
    }

    private static JsonObject rotatedModel(Identifier modelId, int x, int y) {
        return rotatedModel(modelId, x, y, false);
    }

    private static JsonObject rotatedModel(Identifier modelId, int x, int y, boolean uvLock) {
        JsonObject json = modelRef(modelId);
        if (x != 0) {
            json.addProperty("x", x);
        }
        if (y != 0) {
            json.addProperty("y", y);
        }
        if (uvLock) {
            json.addProperty("uvlock", true);
        }
        return json;
    }

    private static JsonObject multipartPart(@Nullable JsonObject when, JsonObject apply) {
        JsonObject json = new JsonObject();
        if (when != null) {
            json.add("when", when);
        }
        json.add("apply", apply);
        return json;
    }

    private static JsonObject singleCondition(String key, String value) {
        JsonObject json = new JsonObject();
        json.addProperty(key, value);
        return json;
    }

    private static Identifier chainModelId(WoodPostBlock.ConnectionType type, boolean upperHalf) {
        return ShadowsAndPetals.asResource("block/wood_post_" + type.getSerializedName() + (upperHalf ? "_link_top" : "_link"));
    }

    private static void addVanityVariant(JsonObject variants, DoubleBlockHalf half, boolean waterlogged, Direction facing, Identifier modelId, int y) {
        variants.add(
                HorizontalDirectionalBlock.FACING.getName() + "=" + facing.getSerializedName()
                        + "," + VanityBlock.HALF.getName() + "=" + half.getSerializedName()
                        + "," + BlockStateProperties.WATERLOGGED.getName() + "=" + waterlogged,
                rotatedModel(modelId, 0, y)
        );
    }

    private void addIroriVariant(JsonObject variants, boolean north, boolean east, boolean south, boolean west, boolean waterlogged, boolean shiftPlaced) {
        boolean edgeNorth = !north;
        boolean edgeEast = !east;
        boolean edgeSouth = !south;
        boolean edgeWest = !west;
        int edgeCount = (edgeNorth ? 1 : 0) + (edgeEast ? 1 : 0) + (edgeSouth ? 1 : 0) + (edgeWest ? 1 : 0);

        Identifier modelId = switch (edgeCount) {
            case 0 -> modLoc("block/irori/center");
            case 1 -> modLoc("block/irori/single_edge");
            case 2 -> edgeNorth == edgeSouth || edgeEast == edgeWest
                    ? modLoc("block/irori/double_edge")
                    : modLoc("block/irori/corner");
            case 3 -> modLoc("block/irori/end");
            case 4 -> modLoc("block/irori/block");
            default -> throw new IllegalStateException("Unexpected edge count: " + edgeCount);
        };

        variants.add(
                IroriBlock.NORTH.getName() + "=" + north
                        + "," + IroriBlock.EAST.getName() + "=" + east
                        + "," + IroriBlock.SOUTH.getName() + "=" + south
                        + "," + IroriBlock.WEST.getName() + "=" + west
                        + "," + IroriBlock.SHIFT_PLACED.getName() + "=" + shiftPlaced,
                rotatedModel(modelId, 0, iroriModelRotation(edgeNorth, edgeEast, edgeSouth, edgeWest, edgeCount))
        );
    }

    private static int iroriModelRotation(boolean north, boolean east, boolean south, boolean west, int edgeCount) {
        return switch (edgeCount) {
            case 0, 4 -> 0;
            case 1 -> east ? 0 : south ? 90 : west ? 180 : 270;
            case 2 -> {
                if (east && west) {
                    yield 0;
                }
                if (north && south) {
                    yield 90;
                }
                if (north && east) {
                    yield 0;
                }
                if (east && south) {
                    yield 90;
                }
                if (south && west) {
                    yield 180;
                }
                yield 270;
            }
            case 3 -> !south ? 0 : !west ? 90 : !north ? 180 : 270;
            default -> throw new IllegalStateException("Unexpected edge count: " + edgeCount);
        };
    }

    private static JsonObject parentModel(Identifier parent) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent.toString());
        return json;
    }

    private static JsonObject cubeAllModel(Identifier texture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/cube_all"));
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture.toString());
        json.add("textures", textures);
        return json;
    }

    private static JsonObject cubeColumnModel(Identifier sideTexture, Identifier endTexture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/cube_column"));
        JsonObject textures = new JsonObject();
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("end", endTexture.toString());
        json.add("textures", textures);
        return json;
    }

    private static String hedgeVariantKey(boolean north, boolean east, boolean south, boolean west, boolean waterlogged) {
        return HedgeBlock.NORTH.getName() + "=" + north
                + "," + HedgeBlock.EAST.getName() + "=" + east
                + "," + HedgeBlock.SOUTH.getName() + "=" + south
                + "," + HedgeBlock.WEST.getName() + "=" + west
                + "," + BlockStateProperties.WATERLOGGED.getName() + "=" + waterlogged;
    }

    private static JsonObject hedgeStateModel(Identifier texture, boolean north, boolean east, boolean south, boolean west) {
        JsonObject json = hedgeBaseModel(texture);
        JsonArray elements = new JsonArray();
        elements.add(hedgeCoreModel(north, east, south, west));
        if (north) {
            elements.add(hedgeArmModel(Direction.NORTH));
        }
        if (east) {
            elements.add(hedgeArmModel(Direction.EAST));
        }
        if (south) {
            elements.add(hedgeArmModel(Direction.SOUTH));
        }
        if (west) {
            elements.add(hedgeArmModel(Direction.WEST));
        }
        json.add("elements", elements);
        return json;
    }

    private static JsonObject hedgeCoreModel(boolean north, boolean east, boolean south, boolean west) {
        return cuboidAllSelective(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D, "#all", true, true, !north, !south, !west, !east, false, false, false, false);
    }

    private static JsonObject hedgeArmModel(Direction direction) {
        return switch (direction) {
            case NORTH -> cuboidAllSelective(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 4.0D, "#all", true, true, true, false, true, true, true, false, false, false);
            case EAST -> cuboidAllSelective(12.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D, "#all", true, true, true, true, false, true, false, false, false, true);
            case SOUTH -> cuboidAllSelective(4.0D, 0.0D, 12.0D, 12.0D, 16.0D, 16.0D, "#all", true, true, false, true, true, true, false, true, false, false);
            case WEST -> cuboidAllSelective(0.0D, 0.0D, 4.0D, 4.0D, 16.0D, 12.0D, "#all", true, true, true, true, true, false, false, false, true, false);
            default -> throw new IllegalArgumentException("Unsupported hedge arm direction: " + direction);
        };
    }

    private static JsonObject hedgeBaseModel(Identifier texture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/block"));
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture.toString());
        textures.addProperty("particle", texture.toString());
        json.add("textures", textures);
        json.addProperty("render_type", "cutout_mipped");
        return json;
    }

    private static JsonObject woodPostCoreModel(Identifier sideTexture, Identifier endTexture) {
        JsonObject json = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", sideTexture.toString());
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("end", endTexture.toString());
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(cuboid(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D, "#side", "#end"));
        json.add("elements", elements);
        json.add("display", tallItemDisplay());
        return json;
    }

    private static JsonObject woodPostLinkModel(Identifier sideTexture, Identifier endTexture, boolean upperHalf) {
        JsonObject json = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", sideTexture.toString());
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("end", endTexture.toString());
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        double fromY = upperHalf ? 10.0D : 0.0D;
        double toY = upperHalf ? 16.0D : 6.0D;
        elements.add(cuboid(6.0D, fromY, 6.0D, 10.0D, toY, 10.0D, "#side", "#end"));
        json.add("elements", elements);
        return json;
    }

    private static JsonObject woodPostChainModel(boolean upperHalf, Identifier chainTexture) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Identifier.withDefaultNamespace("block/block").toString());
        JsonObject textures = new JsonObject();
        textures.addProperty("all", chainTexture.toString());
        textures.addProperty("particle", chainTexture.toString());
        json.add("textures", textures);
        json.addProperty("render_type", "cutout");

        JsonArray elements = new JsonArray();
        double fromY = upperHalf ? 10.0D : 0.0D;
        double toY = upperHalf ? 16.0D : 6.0D;
        double originY = upperHalf ? 18.0D : 8.0D;
        elements.add(chainPlane(6.5D, fromY, 8.0D, 9.5D, toY, 8.0D, originY, true));
        elements.add(chainPlane(8.0D, fromY, 6.5D, 8.0D, toY, 9.5D, originY, false));
        json.add("elements", elements);
        return json;
    }

    private static JsonObject cuboid(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String sideTexture, String endTexture) {
        JsonObject json = new JsonObject();
        json.add("from", vector(fromX, fromY, fromZ));
        json.add("to", vector(toX, toY, toZ));

        JsonObject faces = new JsonObject();
        faces.add("down", face(endTexture));
        faces.add("up", face(endTexture));
        faces.add("north", face(sideTexture));
        faces.add("south", face(sideTexture));
        faces.add("west", face(sideTexture));
        faces.add("east", face(sideTexture));
        json.add("faces", faces);
        return json;
    }

    private static JsonObject cuboidAll(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String texture) {
        return cuboidAllSelective(fromX, fromY, fromZ, toX, toY, toZ, texture, true, true, true, true, true, true, false, false, false, false);
    }

    private static JsonObject cuboidAllSelective(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            String texture,
            boolean includeDown,
            boolean includeUp,
            boolean includeNorth,
            boolean includeSouth,
            boolean includeWest,
            boolean includeEast,
            boolean cullNorth,
            boolean cullSouth,
            boolean cullWest,
            boolean cullEast
    ) {
        JsonObject json = new JsonObject();
        json.add("from", vector(fromX, fromY, fromZ));
        json.add("to", vector(toX, toY, toZ));

        JsonObject faces = new JsonObject();
        addFace(faces, "down", texture, includeDown, null);
        addFace(faces, "up", texture, includeUp, null);
        addFace(faces, "north", texture, includeNorth, cullNorth ? "north" : null);
        addFace(faces, "south", texture, includeSouth, cullSouth ? "south" : null);
        addFace(faces, "west", texture, includeWest, cullWest ? "west" : null);
        addFace(faces, "east", texture, includeEast, cullEast ? "east" : null);
        json.add("faces", faces);
        return json;
    }

    private static void addFace(JsonObject faces, String name, String texture, boolean include, @Nullable String cullface) {
        if (include) {
            faces.add(name, face(texture, cullface));
        }
    }

    private static JsonObject chainPlane(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double originY, boolean northSouthFaces) {
        JsonObject json = new JsonObject();
        json.add("from", vector(fromX, fromY, fromZ));
        json.add("to", vector(toX, toY, toZ));
        json.addProperty("shade", false);

        JsonObject rotation = new JsonObject();
        rotation.addProperty("angle", 45);
        rotation.addProperty("axis", "y");
        rotation.add("origin", vector(8.0D, originY, 8.0D));
        json.add("rotation", rotation);

        JsonObject faces = new JsonObject();
        if (northSouthFaces) {
            faces.add("north", faceWithUv("#all", 0.0D, upperLowerUvMin(fromY), 3.0D, upperLowerUvMax(fromY, toY)));
            faces.add("south", faceWithUv("#all", 0.0D, upperLowerUvMin(fromY), 3.0D, upperLowerUvMax(fromY, toY)));
        } else {
            faces.add("east", faceWithUv("#all", 3.0D, upperLowerUvMin(fromY), 6.0D, upperLowerUvMax(fromY, toY)));
            faces.add("west", faceWithUv("#all", 3.0D, upperLowerUvMin(fromY), 6.0D, upperLowerUvMax(fromY, toY)));
        }
        json.add("faces", faces);
        return json;
    }

    private static double upperLowerUvMin(double fromY) {
        return fromY <= 0.0D ? 10.0D : 0.0D;
    }

    private static double upperLowerUvMax(double fromY, double toY) {
        return fromY <= 0.0D ? 10.0D + (toY - fromY) : toY - fromY;
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }

    private static JsonArray vector(double a, double b, double c, double d) {
        JsonArray array = new JsonArray();
        array.add(a);
        array.add(b);
        array.add(c);
        array.add(d);
        return array;
    }

    private static JsonObject face(String texture) {
        return face(texture, null);
    }

    private static JsonObject face(String texture, @Nullable String cullface) {
        JsonObject json = new JsonObject();
        json.addProperty("texture", texture);
        if (cullface != null) {
            json.addProperty("cullface", cullface);
        }
        return json;
    }

    private static JsonObject faceWithUv(String texture, double u1, double v1, double u2, double v2) {
        JsonObject json = face(texture);
        json.add("uv", vector(u1, v1, u2, v2));
        return json;
    }

    private static JsonObject tallItemDisplay() {
        JsonObject display = new JsonObject();
        display.add("thirdperson_righthand", transform(new double[]{75.0D, 45.0D, 0.0D}, new double[]{0.0D, 1.5D, 0.0D}, new double[]{0.375D, 0.375D, 0.375D}));
        display.add("thirdperson_lefthand", transform(new double[]{75.0D, 45.0D, 0.0D}, new double[]{0.0D, 1.5D, 0.0D}, new double[]{0.375D, 0.375D, 0.375D}));
        display.add("firstperson_righthand", transform(new double[]{0.0D, 135.0D, 0.0D}, new double[]{0.0D, 1.0D, 0.0D}, new double[]{0.4D, 0.4D, 0.4D}));
        display.add("firstperson_lefthand", transform(new double[]{0.0D, 135.0D, 0.0D}, new double[]{0.0D, 1.0D, 0.0D}, new double[]{0.4D, 0.4D, 0.4D}));
        display.add("ground", transform(null, new double[]{0.0D, 3.0D, 0.0D}, new double[]{0.25D, 0.25D, 0.25D}));
        display.add("gui", transform(new double[]{30.0D, -135.0D, 0.0D}, new double[]{0.0D, 0.0D, 0.0D}, new double[]{0.65D, 0.65D, 0.65D}));
        display.add("fixed", transform(null, new double[]{0.0D, 0.0D, 0.0D}, new double[]{0.5D, 0.5D, 0.5D}));
        return display;
    }

    private static JsonObject transform(double[] rotation, double[] translation, double[] scale) {
        JsonObject json = new JsonObject();
        if (rotation != null) {
            json.add("rotation", vector(rotation[0], rotation[1], rotation[2]));
        }
        if (translation != null) {
            json.add("translation", vector(translation[0], translation[1], translation[2]));
        }
        if (scale != null) {
            json.add("scale", vector(scale[0], scale[1], scale[2]));
        }
        return json;
    }

    private static JsonObject crossModel(Identifier texture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/cross"));
        JsonObject textures = new JsonObject();
        textures.addProperty("cross", texture.toString());
        json.add("textures", textures);
        json.addProperty("render_type", "cutout");
        return json;
    }

    public static final class Models {
        public ModelRef getExistingFile(Identifier id) {
            return new ModelRef(id);
        }
    }

    public record ModelRef(Identifier id) {}
}
