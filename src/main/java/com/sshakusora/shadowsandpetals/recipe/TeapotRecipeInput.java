package com.sshakusora.shadowsandpetals.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record TeapotRecipeInput(FluidStack fluid, ItemStack ingredient) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException(index);
        }
        return ingredient;
    }

    @Override
    public int size() {
        return 1;
    }
}
