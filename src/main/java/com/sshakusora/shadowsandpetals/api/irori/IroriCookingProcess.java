package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Immutable result and duration selected for one item placed on an Irori grill. */
public record IroriCookingProcess(ItemStack result, int cookingTime) {
    public IroriCookingProcess {
        result = Objects.requireNonNull(result, "result").copy();
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Irori cooking result must not be empty");
        }
        if (cookingTime <= 0) {
            throw new IllegalArgumentException("Irori cooking time must be positive");
        }
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }
}
