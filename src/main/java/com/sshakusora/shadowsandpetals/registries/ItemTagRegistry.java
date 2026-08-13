package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ItemTagRegistry {
    public static final TagKey<Item> MOD_ITEMS = create("mod_items");

    private ItemTagRegistry() {
    }

    private static TagKey<Item> create(String path) {
        return TagKey.create(Registries.ITEM, ShadowsAndPetals.asResource(path));
    }
}
