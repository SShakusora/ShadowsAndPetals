package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModClientItemProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public ModClientItemProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return DataProvider.saveAll(cache, json -> json, this.pathProvider::json, buildEntries());
    }

    private Map<Identifier, JsonObject> buildEntries() {
        Map<Identifier, JsonObject> result = new LinkedHashMap<>();
        for (var entry : DatagenClientItemRegistry.entries().entrySet()) {
            JsonObject model = new JsonObject();
            model.addProperty("type", entry.getValue().type().toString());
            if (entry.getValue().modelId() != null) {
                model.addProperty("model", entry.getValue().modelId().toString());
            }
            JsonObject root = new JsonObject();
            root.add("model", model);

            result.put(
                ShadowsAndPetals.asResource(entry.getKey().getPath()),
                root
            );
        }
        return result;
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Client Items";
    }
}
