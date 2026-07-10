package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.model.WindChimeItemModel;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
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
        return DataProvider.saveAll(cache, ClientItem.CODEC, this.pathProvider::json, buildEntries());
    }

    private Map<Identifier, ClientItem> buildEntries() {
        Map<Identifier, ClientItem> result = new LinkedHashMap<>();
        for (var entry : DatagenClientItemRegistry.entries().entrySet()) {
            ClientItem clientItem = entry.getValue().modelId() != null
                    ? new ClientItem(ItemModelUtils.plainModel(entry.getValue().modelId()), ClientItem.Properties.DEFAULT)
                    : new ClientItem(customItemModel(entry.getValue().type()), ClientItem.Properties.DEFAULT);
            result.put(
                    ShadowsAndPetals.asResource(entry.getKey().getPath()),
                    clientItem
            );
        }
        return result;
    }

    private static ItemModel.Unbaked customItemModel(Identifier type) {
        if (type.equals(WindChimeItemModel.TYPE)) {
            return WindChimeItemModel.Unbaked.INSTANCE;
        }
        throw new IllegalArgumentException("Unsupported custom client item model type for datagen: " + type);
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Client Items";
    }
}
