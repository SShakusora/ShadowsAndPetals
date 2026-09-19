package com.sshakusora.shadowsandpetals.legacy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Keeps the temporary compatibility blocks created by state aliases addressable by their
 * original registry id.  Block-entity data aliases need the same block suppliers when they
 * construct the legacy block-entity type.
 */
public final class LegacyBlockAliasRegistry {
    private static final Map<ResourceLocation, Supplier<? extends Block>> BLOCKS = new LinkedHashMap<>();

    private LegacyBlockAliasRegistry() {}

    public static synchronized void add(ResourceLocation legacyId, Supplier<? extends Block> block) {
        Supplier<? extends Block> previous = BLOCKS.putIfAbsent(legacyId, block);
        if (previous != null && previous != block) {
            throw new IllegalStateException("Duplicate legacy block alias: " + legacyId);
        }
    }

    /** Returns the compatibility block registered for a state alias. */
    public static synchronized Supplier<? extends Block> require(ResourceLocation legacyId) {
        Supplier<? extends Block> block = BLOCKS.get(legacyId);
        if (block == null) {
            throw new IllegalStateException("No state-alias compatibility block registered for " + legacyId);
        }
        return block;
    }

    /** @deprecated use {@link #require(ResourceLocation)} to make the failure contract explicit. */
    @Deprecated(forRemoval = false)
    public static synchronized Supplier<? extends Block> get(ResourceLocation legacyId) {
        return require(legacyId);
    }
}
