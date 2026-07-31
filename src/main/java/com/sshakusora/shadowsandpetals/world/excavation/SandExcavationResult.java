package com.sshakusora.shadowsandpetals.world.excavation;

import net.minecraft.world.item.ItemStack;

public record SandExcavationResult(Category category, ItemStack stack) {
    private static final SandExcavationResult EMPTY = new SandExcavationResult(Category.EMPTY, ItemStack.EMPTY);

    public static SandExcavationResult empty() {
        return EMPTY;
    }

    public enum Category {
        SEAFOOD,
        TRASH,
        EMPTY
    }
}
