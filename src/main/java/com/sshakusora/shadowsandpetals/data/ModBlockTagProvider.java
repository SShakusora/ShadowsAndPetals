package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockTagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ShadowsAndPetals.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (Map.Entry<TagKey<Block>, List<DeferredBlock<? extends Block>>> entry : BlockTagRegistry.getAll().entrySet()) {
            var appender = tag(entry.getKey());
            for (DeferredBlock<? extends Block> block : entry.getValue()) {
                appender.add(block.get());
            }
        }

        for (Map.Entry<TagKey<Block>, List<TagKey<Block>>> entry : BlockTagRegistry.getAllIncludedTags().entrySet()) {
            var appender = tag(entry.getKey());
            for (TagKey<Block> includedTag : entry.getValue()) {
                appender.addTag(includedTag);
            }
        }
    }
}
