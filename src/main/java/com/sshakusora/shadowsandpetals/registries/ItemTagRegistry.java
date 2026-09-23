package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.ModItemTagProvider;
import com.sshakusora.shadowsandpetals.registries.builder.RegItemBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.*;

public final class ItemTagRegistry {
    public static final TagKey<Item> MOD_ITEMS = create("mod_items");
    public static final TagKey<Item> STRIPPED_WOOD_PILLARS = create("stripped_wood_pillars");

    private static final Map<TagKey<Item>, List<DeferredItem<? extends Item>>> TAG_MAP =
            new LinkedHashMap<>();

    private ItemTagRegistry() {
    }

    private static TagKey<Item> create(String path) {
        return TagKey.create(Registries.ITEM, ShadowsAndPetals.asResource(path));
    }

    /**
     * Adds an item declared with a tag in {@link RegItemBuilder}.
     * The deferred entry is retained until {@link ModItemTagProvider} runs,
     * avoiding early registry resolution during item registration.
     */
    public static void add(TagKey<Item> tag, DeferredItem<? extends Item> item) {
        TAG_MAP.computeIfAbsent(Objects.requireNonNull(tag), ignored -> new ArrayList<>())
                .add(Objects.requireNonNull(item));
    }

    /**
     * Returns all item-tag declarations collected during registration.
     */
    public static Map<TagKey<Item>, List<DeferredItem<? extends Item>>> getAll() {
        return Collections.unmodifiableMap(TAG_MAP);
    }

    /**
     * Clears collected declarations for isolated data-generation or test runs.
     */
    public static void clear() {
        TAG_MAP.clear();
    }
}
