package com.sshakusora.shadowsandpetals.client.ct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Selects one connected-texture atlas for a block position.
 * Implementations must be deterministic because the selected index participates
 * in the block model's geometry cache key.
 */
@FunctionalInterface
public interface CTTextureSelector {

    CTTextureSelector FIRST = (state, pos, face) -> 0;

    int select(BlockState state, BlockPos pos, Direction face);

    /**
     * Selects {@code periodicTextureIndex} at every {@code interval}-by-{@code interval}
     * grid intersection on each block face, and {@code defaultTextureIndex} elsewhere.
     */
    static CTTextureSelector everyNth(
            int interval,
            int defaultTextureIndex,
            int periodicTextureIndex
    ) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Connected-texture interval must be positive");
        }
        if (defaultTextureIndex < 0 || periodicTextureIndex < 0) {
            throw new IllegalArgumentException("Connected-texture indices must be non-negative");
        }

        return (state, pos, face) -> {
            int first;
            int second;
            switch (face.getAxis()) {
                case X -> {
                    first = pos.getY();
                    second = pos.getZ();
                }
                case Y -> {
                    first = pos.getX();
                    second = pos.getZ();
                }
                case Z -> {
                    first = pos.getX();
                    second = pos.getY();
                }
                default -> throw new IllegalStateException("Unexpected direction axis: " + face.getAxis());
            }
            return Math.floorMod(first, interval) == 0
                    && Math.floorMod(second, interval) == 0
                    ? periodicTextureIndex
                    : defaultTextureIndex;
        };
    }
}
