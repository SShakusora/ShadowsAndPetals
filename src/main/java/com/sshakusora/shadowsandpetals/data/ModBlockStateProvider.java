package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.IngotPileBlock;
import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;

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

    public void saplingBlock(SaplingBlock block) {
        saplingBlock(block, modLoc("block/" + name(block)));
    }

    public void saplingBlock(SaplingBlock block, Identifier texture) {
        Identifier modelId = blockModelId(block);
        this.models.put(modelId, crossModel(texture));
        simpleBlock(block, new ModelRef(modelId));
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
