package com.sshakusora.shadowsandpetals.data.model;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import java.util.HashSet;
import java.util.Optional;
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
        new ModelTemplate(Optional.of(parent), Optional.empty())
                .create(modelId, new TextureMapping(), modelOutput);
    }

    public void translatedParentModel(Identifier modelId, Identifier parent, float x, float y, float z) {
        ExtendedModelTemplateBuilder.builder()
                .parent(parent)
                .rootTransforms(transform -> transform.translation(x, y, z))
                .build()
                .create(modelId, new TextureMapping(), modelOutput);
    }

    public void suggestItemModel(Item item, Identifier modelId) {
        suggestedItems.accept(item, ItemModelUtils.plainModel(modelId));
    }

    private BiConsumer<Identifier, ModelInstance> renderTypeOutput(String renderType) {
        return (id, model) -> modelOutput.accept(id, () -> {
            JsonObject json = model.get().getAsJsonObject();
            json.addProperty("render_type", renderType);
            return json;
        });
    }
}
