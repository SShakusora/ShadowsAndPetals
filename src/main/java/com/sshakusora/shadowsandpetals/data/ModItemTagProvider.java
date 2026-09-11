package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.ItemTagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** Generates the item tags consumed by recipes and advancement predicates. */
public final class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<net.minecraft.world.level.block.Block>> blockTags,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTags, ShadowsAndPetals.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var modItems = tag(ItemTagRegistry.MOD_ITEMS);
        var strippedPillars = tag(ItemTagRegistry.STRIPPED_WOOD_PILLARS);

        for (var holder : BuiltInRegistries.ITEM.holders().toList()) {
            ResourceLocation id = holder.key().location();
            if (!ShadowsAndPetals.MOD_ID.equals(id.getNamespace())) {
                continue;
            }

            Item item = holder.value();
            modItems.add(item);
            String path = id.getPath();
            if (path.startsWith("stripped_") && path.endsWith("_wood_pillar")
                    || path.equals("red_lacquered_wood_pillar")) {
                strippedPillars.add(item);
            }
        }
    }
}
