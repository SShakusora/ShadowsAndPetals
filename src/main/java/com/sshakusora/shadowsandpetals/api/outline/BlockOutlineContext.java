package com.sshakusora.shadowsandpetals.api.outline;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Context available while extracting a block's client-side selection outline.
 *
 * <p>This type deliberately contains no client-only rendering classes, so
 * blocks can implement {@link BlockOutlineProvider} safely on both sides.</p>
 */
public record BlockOutlineContext(
        BlockPos blockPos,
        BlockHitResult hitResult,
        CollisionContext collisionContext
) {
}
