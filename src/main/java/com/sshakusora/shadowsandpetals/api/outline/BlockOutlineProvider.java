package com.sshakusora.shadowsandpetals.api.outline;

import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Supplies the geometric wireframe used for a block's client-side selection
 * outline. The returned geometry is expressed in block-local coordinates.
 */
@FunctionalInterface
public interface BlockOutlineProvider {
    @Nullable
    OutlineGeometry getOutline(BlockState state, BlockOutlineContext context);
}
