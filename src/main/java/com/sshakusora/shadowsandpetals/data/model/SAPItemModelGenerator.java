package com.sshakusora.shadowsandpetals.data.model;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.1 compatibility facade for item model callbacks.
 */
public class SAPItemModelGenerator {
    private final @Nullable ItemModelProvider provider;

    public SAPItemModelGenerator() {
        this(null);
    }

    public SAPItemModelGenerator(@Nullable ItemModelProvider provider) {
        this.provider = provider;
    }

    public void generatedItem(Item item) {
        if (provider == null) {
            return;
        }
        String name = item.builtInRegistryHolder().key().location().getPath();
        provider.withExistingParent(name, "item/generated")
                .texture("layer0", ShadowsAndPetals.asResource("item/" + name));
    }

    public void model(ResourceLocation id, Object model) {
    }

    public void finalizeClientItem(Item item, ResourceLocation clientModel, ResourceLocation customClientType) {
        if (provider == null) {
            return;
        }
        String name = item.builtInRegistryHolder().key().location().getPath();
        if (clientModel != null) {
            provider.withExistingParent(name, clientModel);
            return;
        }
        if (customClientType == null) {
            return;
        }
        ResourceLocation fallback = switch (name) {
            case "tea_bucket" -> ResourceLocation.parse("minecraft:item/bucket");
            case "wind_chime" -> ShadowsAndPetals.asResource("item/wind_chime_body");
            case "wooden_barrel" -> ShadowsAndPetals.asResource("block/wooden_barrel/wooden_barrel");
            default -> ResourceLocation.parse("minecraft:item/generated");
        };
        provider.withExistingParent(name, fallback);
    }
}
