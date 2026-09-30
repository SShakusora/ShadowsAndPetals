package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import net.minecraft.util.StringRepresentable;

/** The side of a window on which a curtain panel is mounted. */
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
