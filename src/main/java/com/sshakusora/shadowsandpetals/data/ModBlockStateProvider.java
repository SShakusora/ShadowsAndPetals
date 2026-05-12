package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.IngotPileBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
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

    public void logBlockWithItem(RotatedPillarBlock block) {
        Identifier sideTexture = modLoc("block/" + name(block));
        logBlockWithItem(block, sideTexture, sideTexture);
    }

    public void logBlockWithItem(RotatedPillarBlock block, Identifier sideTexture, Identifier endTexture) {
        Identifier verticalModel = blockModelId(block);
        Identifier horizontalModel = modLoc("block/" + name(block) + "_horizontal");

        this.models.put(verticalModel, axisModel(sideTexture, endTexture));
        this.models.put(horizontalModel, axisHorizontalModel(sideTexture, endTexture));

        JsonObject variants = new JsonObject();
        variants.add("axis=x", rotatedModel(horizontalModel, 90, 90));
        variants.add("axis=y", modelRef(verticalModel));
        variants.add("axis=z", rotatedModel(horizontalModel, 90, 0));

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), parentModel(verticalModel));
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

    public void saplingBlockWithItem(SaplingBlock block) {
        saplingBlockWithItem(block, modLoc("block/" + name(block)));
    }

    public void saplingBlockWithItem(SaplingBlock block, Identifier texture) {
        Identifier modelId = blockModelId(block);
        this.models.put(modelId, crossModel(texture));
        simpleBlockWithItem(block, new ModelRef(modelId));
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
        JsonObject json = modelRef(modelId);
        if (x != 0) {
            json.addProperty("x", x);
        }
        if (y != 0) {
            json.addProperty("y", y);
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

    private static JsonObject axisModel(Identifier sideTexture, Identifier endTexture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/cube_column"));
        JsonObject textures = new JsonObject();
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("end", endTexture.toString());
        json.add("textures", textures);
        return json;
    }

    private static JsonObject axisHorizontalModel(Identifier sideTexture, Identifier endTexture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/cube_column_horizontal"));
        JsonObject textures = new JsonObject();
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("end", endTexture.toString());
        json.add("textures", textures);
        return json;
    }

    public static final class Models {
        public ModelRef getExistingFile(Identifier id) {
            return new ModelRef(id);
        }
    }

    public record ModelRef(Identifier id) {}
}
