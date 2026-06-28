package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.model.BlockModelTemplates;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
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
        JsonObject json = new JsonObject();
        json.addProperty("parent", Identifier.withDefaultNamespace("item/generated").toString());

        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", modLoc("item/" + name(item)).toString());
        json.add("textures", textures);

        this.models.put(modLoc("item/" + name(item)), json);
    }

    public void generatedBlockItem(Block block, Identifier texture) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Identifier.withDefaultNamespace("item/generated").toString());

        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture.toString());
        json.add("textures", textures);

        this.models.put(modLoc("item/" + name(block)), json);
    }

    public void windChimeItemModels() {
        this.models.put(WindChimeColors.itemBodyModelId(), BlockModelTemplates.parentModel(modLoc("item/wind_chime_body")));
        for (DyeColor ribbon : DyeColor.values()) {
            JsonObject json = BlockModelTemplates.parentModel(modLoc("item/wind_chime_ribbon"));
            JsonObject textures = new JsonObject();
            textures.addProperty("2", modLoc("block/wind_chime/ribbon/" + ribbon.getName()).toString());
            textures.addProperty("particle", modLoc("block/wind_chime/ribbon/" + ribbon.getName()).toString());
            json.add("textures", textures);
            this.models.put(WindChimeColors.itemRibbonModelId(ribbon), json);
        }
        for (DyeColor vane : DyeColor.values()) {
            JsonObject json = BlockModelTemplates.parentModel(modLoc("item/wind_chime_vane"));
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", modLoc("block/wind_chime/vane/" + vane.getName()).toString());
            textures.addProperty("windchime0", modLoc("block/wind_chime/vane/" + vane.getName()).toString());
            json.add("textures", textures);
            this.models.put(WindChimeColors.itemVaneModelId(vane), json);
        }
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
