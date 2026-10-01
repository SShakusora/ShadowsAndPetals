package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import net.minecraft.util.StringRepresentable;

/** The side of the window on which a curtain hangs. */
public enum CurtainSide implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    CurtainSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public CurtainSide mirror() {
        return this == LEFT ? RIGHT : LEFT;
    }
}
