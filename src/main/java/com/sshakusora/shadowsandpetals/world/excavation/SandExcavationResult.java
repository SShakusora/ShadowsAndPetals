package com.sshakusora.shadowsandpetals.world.excavation;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public record SandExcavationResult(Category category, ItemStack stack) {
    private static final SandExcavationResult EMPTY = new SandExcavationResult(Category.EMPTY, ItemStack.EMPTY);

    public static SandExcavationResult empty() {
        return EMPTY;
    }

    public enum Category implements StringRepresentable {
        SEAFOOD("seafood"),
        TRASH("trash"),
        EMPTY("empty");

        public static final StringRepresentable.EnumCodec<Category> CODEC =
                StringRepresentable.fromEnum(Category::values);

        private final String serializedName;

        Category(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
