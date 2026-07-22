package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.ModBlockTagProvider;
import com.sshakusora.shadowsandpetals.registries.builder.RegBlockBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.*;

/**
 * Global registry that collects block → tag mappings declared via {@link RegBlockBuilder}
 * during block registration. These mappings are later consumed by
 * {@link ModBlockTagProvider} for automatic datagen.
 */
public final class BlockTagRegistry {
    public static final TagKey<Block> REQUIRES_IRORI_GRILL = create("requires_irori_grill");
    public static final TagKey<Block> WOOD_POST_HANGING_CONNECTIONS = create("wood_post_hanging_connections");

    private static final Map<TagKey<Block>, List<DeferredBlock<? extends Block>>> TAG_MAP = new HashMap<>();
    private static final Map<TagKey<Block>, List<TagKey<Block>>> INCLUDED_TAG_MAP = new HashMap<>();

    static {
        addDefaultIncludedTags();
    }

    private BlockTagRegistry() {
    }

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, ShadowsAndPetals.asResource(path));
    }

    private static void addDefaultIncludedTags() {
        include(WOOD_POST_HANGING_CONNECTIONS, BlockTags.LANTERNS);
        include(WOOD_POST_HANGING_CONNECTIONS, BlockTags.CEILING_HANGING_SIGNS);
    }

    public static void add(TagKey<Block> tag, DeferredBlock<? extends Block> block) {
        TAG_MAP.computeIfAbsent(tag, k -> new ArrayList<>()).add(block);
    }

    public static Map<TagKey<Block>, List<DeferredBlock<? extends Block>>> getAll() {
        return Collections.unmodifiableMap(TAG_MAP);
    }

    public static void include(TagKey<Block> tag, TagKey<Block> includedTag) {
        INCLUDED_TAG_MAP.computeIfAbsent(tag, k -> new ArrayList<>()).add(includedTag);
    }

    public static Map<TagKey<Block>, List<TagKey<Block>>> getAllIncludedTags() {
        return Collections.unmodifiableMap(INCLUDED_TAG_MAP);
    }

    public static void clear() {
        TAG_MAP.clear();
        INCLUDED_TAG_MAP.clear();
        addDefaultIncludedTags();
    }
}
