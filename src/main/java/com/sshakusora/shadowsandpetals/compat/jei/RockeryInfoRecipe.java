package com.sshakusora.shadowsandpetals.compat.jei;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * JEI informational recipe data for rockery carving.
 * Each rockery dimension (1×1×1, 1×1×2, etc.) is one recipe
 * showing the stone input and rockery output relationship.
 */
public record RockeryInfoRecipe(RockeryBlock block, RockeryDimensions dimensions) {

    public ItemStack inputStack() {
        return new ItemStack(Blocks.STONE, dimensions.partCount());
    }

    public ItemStack outputStack() {
        return new ItemStack(block);
    }
}
