package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ModItemModelProvider implements DataProvider {
    private final PackOutput.PathProvider modelPathProvider;
    private final Map<Identifier, JsonObject> models = new LinkedHashMap<>();

    public ModItemModelProvider(PackOutput output) {
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.models.clear();
        registerModels();
        return DataProvider.saveAll(cache, json -> json, this.modelPathProvider::json, this.models);
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Item Models";
    }

    protected void registerModels() {
        for (var generator : DatagenItemModelRegistry.generators()) {
            generator.accept(this);
        }
    }

    public void generatedItem(Item item) {
        Identifier modelId = modLoc("item/" + name(item));
        ModelTemplates.FLAT_ITEM.create(
                modelId,
                new TextureMapping().put(TextureSlot.LAYER0, new Material(modelId)),
                this::putGeneratedModel
        );
    }

    public void generatedBlockItem(Block block, Identifier texture) {
        ModelTemplates.FLAT_ITEM.create(
                modLoc("item/" + name(block)),
                new TextureMapping().put(TextureSlot.LAYER0, new Material(texture)),
                this::putGeneratedModel
        );
    }

    public void windChimeItemModels() {
        putParentModel(WindChimeColors.itemBodyModelId(), modLoc("item/wind_chime_body"));
        for (DyeColor ribbon : DyeColor.values()) {
            TextureSlot ribbonSlot = TextureSlot.create("2");
            Identifier ribbonTexture = modLoc("block/wind_chime/ribbon/" + ribbon.getName());
            new ModelTemplate(Optional.of(modLoc("item/wind_chime_ribbon")), Optional.empty(), ribbonSlot, TextureSlot.PARTICLE)
                    .create(
                            WindChimeColors.itemRibbonModelId(ribbon),
                            new TextureMapping()
                                    .put(ribbonSlot, new Material(ribbonTexture))
                                    .put(TextureSlot.PARTICLE, new Material(ribbonTexture)),
                            this::putGeneratedModel
                    );
        }
        for (DyeColor vane : DyeColor.values()) {
            TextureSlot vaneSlot = TextureSlot.create("windchime0");
            Identifier vaneTexture = modLoc("block/wind_chime/vane/" + vane.getName());
            new ModelTemplate(Optional.of(modLoc("item/wind_chime_vane")), Optional.empty(), TextureSlot.PARTICLE, vaneSlot)
                    .create(
                            WindChimeColors.itemVaneModelId(vane),
                            new TextureMapping()
                                    .put(TextureSlot.PARTICLE, new Material(vaneTexture))
                                    .put(vaneSlot, new Material(vaneTexture)),
                            this::putGeneratedModel
                    );
        }
    }

    private void putParentModel(Identifier modelId, Identifier parent) {
        new ModelTemplate(Optional.of(parent), Optional.empty())
                .create(modelId, new TextureMapping(), this::putGeneratedModel);
    }

    private void putGeneratedModel(Identifier modelId, ModelInstance model) {
        this.models.put(modelId, model.get().getAsJsonObject());
    }

    public Identifier modLoc(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    private String name(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }
}
