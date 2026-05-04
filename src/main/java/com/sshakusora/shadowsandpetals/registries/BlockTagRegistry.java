package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.*;

/**
 * Global registry that collects block → tag mappings declared via {@link com.sshakusora.shadowsandpetals.registries.builder.RegBlockBuilder}
 * during block registration. These mappings are later consumed by
 * {@link com.sshakusora.shadowsandpetals.data.ModBlockTagProvider} for automatic datagen.
 */
public class BlockTagRegistry {
    private static final Map<TagKey<Block>, List<DeferredBlock<? extends Block>>> TAG_MAP = new HashMap<>();

    public static void add(TagKey<Block> tag, DeferredBlock<? extends Block> block) {
        TAG_MAP.computeIfAbsent(tag, k -> new ArrayList<>()).add(block);
    }

    public static Map<TagKey<Block>, List<DeferredBlock<? extends Block>>> getAll() {
        return Collections.unmodifiableMap(TAG_MAP);
    }

    public static void clear() {
        TAG_MAP.clear();
    }
}
