package com.sshakusora.shadowsandpetals.client.renderer;

import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

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
    public BonsaiPartCacheKey {
        if (trunkBlockId == null || dead) {
            leavesBlockId = null;
        }
    }

    public static BonsaiPartCacheKey forState(
            BonsaiBlockEntity.Shape shape,
            boolean planted,
            boolean dead,
            @Nullable Identifier trunkBlockId,
            @Nullable Identifier leavesBlockId
    ) {
        return new BonsaiPartCacheKey(
                shape,
                dead,
                planted ? trunkBlockId : null,
                planted && !dead ? leavesBlockId : null
        );
    }
}
