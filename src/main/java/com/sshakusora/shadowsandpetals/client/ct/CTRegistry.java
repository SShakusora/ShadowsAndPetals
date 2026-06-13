package com.sshakusora.shadowsandpetals.client.ct;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Static registry of connected-texture entries.
 * <p>
 * Populated after block registration and consumed at model-bake time
 * by {@link CTModelRegistry} to wrap applicable block-state models.
 */
public final class CTRegistry {

    private static final Map<Identifier, CTEntry> ENTRIES = new HashMap<>();

    private CTRegistry() {}

    /**
     * Registers a connected-texture entry for a block.
     *
     * @param blockId          the block's registry id
     * @param baseTexture      the normal 16×16 texture the model references
     * @param connectedTexture the connected sprite-sheet atlas
     * @param type             how to map connection context → atlas tile index
     */
    public static void register(Identifier blockId, Identifier baseTexture,
                                 Identifier connectedTexture, CTTextureType type) {
        ENTRIES.put(blockId, new CTEntry(baseTexture, connectedTexture, type));
    }

    public static Map<Identifier, CTEntry> entries() {
        return ENTRIES;
    }

    /**
     * Holds the texture pair and CT type for a single block.
     */
    public record CTEntry(Identifier baseTexture, Identifier connectedTexture, CTTextureType type) {}
}
