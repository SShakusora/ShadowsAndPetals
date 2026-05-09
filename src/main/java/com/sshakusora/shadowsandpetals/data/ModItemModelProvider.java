package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

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

    public Identifier modLoc(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    private String name(Item item) {
        return item.builtInRegistryHolder().key().identifier().getPath();
    }
}
