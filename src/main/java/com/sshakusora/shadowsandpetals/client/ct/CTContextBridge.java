package com.sshakusora.shadowsandpetals.client.ct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Supplies connected-texture context for a model wrapper owned by another
 * mod. Implementations are loaded only when their optional dependency exists.
 */
public interface CTContextBridge {
    /**
     * Returns whether this bridge owns the current pseudo-block render pass.
     */
    boolean matches(BlockAndTintGetter level, BlockPos pos, BlockState state);

    /**
     * Builds the eight-way connection context for one material face.
     */
    CTContext buildContext(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face);
}
