package com.sshakusora.shadowsandpetals.block.decoration.window;

import net.minecraft.util.StringRepresentable;

public enum CasementWindowSide implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    CasementWindowSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public CasementWindowSide mirror() {
        return this == LEFT ? RIGHT : LEFT;
    }
}
