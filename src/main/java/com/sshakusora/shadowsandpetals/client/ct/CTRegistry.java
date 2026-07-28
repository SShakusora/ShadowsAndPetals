package com.sshakusora.shadowsandpetals.client.ct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.*;

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
        register(blockId, baseTexture, connectedTexture, type, 0);
    }

    public static void register(Identifier blockId, Identifier baseTexture,
                                 Identifier connectedTexture, CTTextureType type, int padding) {
        register(
                blockId,
                baseTexture,
                List.of(connectedTexture),
                CTTextureSelector.FIRST,
                type,
                padding);
    }

    /**
     * Registers one or more connected textures and a rule that selects their
     * zero-based index for each block position.
     */
    public static void register(
            Identifier blockId,
            Identifier baseTexture,
            List<Identifier> connectedTextures,
            CTTextureSelector textureSelector,
            CTTextureType type,
            int padding
    ) {
        ENTRIES.put(blockId, new CTEntry(
                baseTexture,
                connectedTextures,
                textureSelector,
                type,
                padding));
    }

    public static Map<Identifier, CTEntry> entries() {
        return Collections.unmodifiableMap(ENTRIES);
    }

    /**
     * Holds the connected textures, selection rule, and CT type for a single block.
     */
    public record CTEntry(
            Identifier baseTexture,
            List<Identifier> connectedTextures,
            CTTextureSelector textureSelector,
            CTTextureType type,
            int padding
    ) {
        public CTEntry {
            Objects.requireNonNull(baseTexture, "baseTexture");
            connectedTextures = List.copyOf(connectedTextures);
            Objects.requireNonNull(textureSelector, "textureSelector");
            Objects.requireNonNull(type, "type");
            if (connectedTextures.isEmpty()) {
                throw new IllegalArgumentException("At least one connected texture is required");
            }
            if (padding < 0) {
                throw new IllegalArgumentException("Connected-texture padding cannot be negative");
            }
        }

        public int selectTextureIndex(BlockPos pos, Direction face) {
            int index = textureSelector.select(pos, face);
            if (index < 0 || index >= connectedTextures.size()) {
                throw new IllegalStateException(
                        "Connected-texture selector returned index " + index
                                + " for " + connectedTextures.size() + " textures at " + pos
                                + " on face " + face);
            }
            return index;
        }
    }
}
