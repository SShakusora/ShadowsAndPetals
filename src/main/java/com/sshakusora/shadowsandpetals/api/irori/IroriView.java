package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

import java.util.List;
import java.util.Objects;

/**
 * Read-only context passed to registered Irori rules.
 *
 * <p>Rules must not mutate the level from this view. State changes belong to the Irori mechanism
 * that invokes the rule, which keeps server synchronization and render invalidation centralized.
 */
public record IroriView(
        LevelReader level,
        BlockPos masterPos,
        IroriLayout layout,
        List<IroriContent> surfaceContents,
        boolean burning
) {
    public IroriView {
        level = Objects.requireNonNull(level, "level");
        masterPos = Objects.requireNonNull(masterPos, "masterPos").immutable();
        layout = Objects.requireNonNull(layout, "layout");
        surfaceContents = List.copyOf(Objects.requireNonNull(surfaceContents, "surfaceContents"));
    }
}
