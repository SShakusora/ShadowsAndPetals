package com.sshakusora.shadowsandpetals.client.renderer;

import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Cache key identifying one fully-determined bonsai render configuration.
 * The rendered part list depends only on (shape, dead, trunk, leaves); two
 * entities with the same key share the same wrapped parts and remapped quads.
 */
public record BonsaiPartCacheKey(
        BonsaiBlockEntity.Shape shape,
        boolean dead,
        @Nullable Identifier trunkBlockId,
        @Nullable Identifier leavesBlockId
) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BonsaiPartCacheKey other)) {
            return false;
        }
        return shape == other.shape
                && dead == other.dead
                && Objects.equals(trunkBlockId, other.trunkBlockId)
                && Objects.equals(leavesBlockId, other.leavesBlockId);
    }

    @Override
    public int hashCode() {
        int result = shape.hashCode();
        result = 31 * result + (dead ? 1 : 0);
        result = 31 * result + Objects.hashCode(trunkBlockId);
        result = 31 * result + Objects.hashCode(leavesBlockId);
        return result;
    }
}