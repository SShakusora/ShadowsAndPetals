package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.Objects;

/** Read-only server-side context used to calculate registered ash drops. */
public record IroriAshDropContext(Level level, BlockPos dropPos, RandomSource random) {
    public IroriAshDropContext {
        level = Objects.requireNonNull(level, "level");
        if (level.isClientSide()) {
            throw new IllegalArgumentException("Irori ash drops must be calculated on the logical server");
        }
        dropPos = Objects.requireNonNull(dropPos, "dropPos").immutable();
        random = Objects.requireNonNull(random, "random");
    }
}
