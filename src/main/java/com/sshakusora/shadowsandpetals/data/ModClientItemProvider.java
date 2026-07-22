package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.model.WindChimeItemModel;
import com.sshakusora.shadowsandpetals.registries.FluidRegistry;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ModClientItemProvider implements DataProvider {
    private static final Identifier TEA_BUCKET_MODEL_TYPE = ShadowsAndPetals.asResource("tea_bucket");
    private static final Material BUCKET_TEXTURE =
            new Material(Identifier.withDefaultNamespace("item/bucket"));
    private static final Material BUCKET_FLUID_MASK =
            new Material(Identifier.fromNamespaceAndPath("neoforge", "item/mask/bucket_fluid"));

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
        if (type.equals(TEA_BUCKET_MODEL_TYPE)) {
            return new DynamicFluidContainerModel.Unbaked(
                    new DynamicFluidContainerModel.Textures(
                            Optional.empty(),
                            Optional.of(BUCKET_TEXTURE),
                            Optional.of(BUCKET_FLUID_MASK),
                            Optional.empty()
                    ),
                    FluidRegistry.TEA.get(),
                    false,
                    true,
                    true
            );
        }
        throw new IllegalArgumentException("Unsupported custom client item model type for datagen: " + type);
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Client Items";
    }
}
