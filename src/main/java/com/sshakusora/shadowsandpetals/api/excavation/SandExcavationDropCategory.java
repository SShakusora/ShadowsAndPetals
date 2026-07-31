package com.sshakusora.shadowsandpetals.api.excavation;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum SandExcavationDropCategory implements StringRepresentable {
    SEAFOOD("seafood"),
    TRASH("trash");

    public static final StringRepresentable.EnumCodec<SandExcavationDropCategory> CODEC =
            StringRepresentable.fromEnum(SandExcavationDropCategory::values);

    private final String serializedName;

    SandExcavationDropCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NonNull String getSerializedName() {
        return serializedName;
    }
}
