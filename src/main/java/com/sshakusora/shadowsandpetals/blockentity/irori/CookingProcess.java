package com.sshakusora.shadowsandpetals.blockentity.irori;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Internal cooking result selected for one item on an Irori grill. */
record CookingProcess(ItemStack result, int cookingTime) {
    CookingProcess {
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
