package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Server-side context used to resolve an item placed on an Irori grill. */
public record IroriCookingContext(
        ServerLevel level,
        BlockPos masterPos,
        BlockPos cookingPos,
        ItemStack input
) {
    public IroriCookingContext {
        level = Objects.requireNonNull(level, "level");
        masterPos = Objects.requireNonNull(masterPos, "masterPos").immutable();
        cookingPos = Objects.requireNonNull(cookingPos, "cookingPos").immutable();
        input = Objects.requireNonNull(input, "input").copy();
    }

    @Override
    public ItemStack input() {
        return input.copy();
    }
}
