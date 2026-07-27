package com.sshakusora.shadowsandpetals.data.model;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public final class SAPBlockModelGenerator {
    private final BlockModelGenerators vanilla;
    private final SuggestedItemModels suggestedItems;
    private final BiConsumer<Identifier, ModelInstance> modelOutput;
    private final Set<Identifier> emittedOptionalModels = new HashSet<>();

    public SAPBlockModelGenerator(BlockModelGenerators source) {
        this.suggestedItems = new SuggestedItemModels();
        this.modelOutput = source.modelOutput;
        this.vanilla = new BlockModelGenerators(source.blockStateOutput, suggestedItems, source.modelOutput);
    }

    public BlockModelGenerators vanilla() {
        return vanilla;
    }

    SuggestedItemModels suggestedItems() {
        return suggestedItems;
    }

    public Identifier modLoc(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    public Identifier mcLoc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    public Identifier blockModelId(Block block) {
        return ModelLocationUtils.getModelLocation(block);
    }

    public Identifier itemModelId(Item item) {
        return ModelLocationUtils.getModelLocation(item);
    }

    public void blockState(BlockModelDefinitionGenerator generator) {
        vanilla.blockStateOutput.accept(generator);
    }

    public void model(Identifier id, ModelInstance model) {
        modelOutput.accept(id, model);
    }

    public void jsonModel(Identifier id, JsonObject json) {
        modelOutput.accept(id, () -> json);
    }

    public void jsonModelOnce(Identifier id, JsonObject json) {
        if (emittedOptionalModels.add(id)) {
            jsonModel(id, json);
        }
    }

    public Identifier create(ModelTemplate template, Block block, TextureMapping mapping) {
        return template.create(block, mapping, modelOutput);
    }

    public Identifier create(ModelTemplate template, Identifier id, TextureMapping mapping) {
        return template.create(id, mapping, modelOutput);
    }

    public Identifier create(ModelTemplate template, Block block, TextureMapping mapping, String renderType) {
        return template.create(block, mapping, renderTypeOutput(renderType));
    }

    public Identifier create(ModelTemplate template, Identifier id, TextureMapping mapping, String renderType) {
        template.create(id, mapping, renderTypeOutput(renderType));
        return id;
    }

    public void parentModel(Identifier modelId, Identifier parent) {
        new ModelTemplate(java.util.Optional.of(parent), java.util.Optional.empty())
                .create(modelId, new TextureMapping(), modelOutput);
    }

    public void suggestItemModel(Item item, Identifier modelId) {
        suggestedItems.accept(item, net.minecraft.client.data.models.model.ItemModelUtils.plainModel(modelId));
    }

    private BiConsumer<Identifier, ModelInstance> renderTypeOutput(String renderType) {
        return (id, model) -> modelOutput.accept(id, () -> {
            JsonObject json = model.get().getAsJsonObject();
            json.addProperty("render_type", renderType);
            return json;
        });
    }
}
