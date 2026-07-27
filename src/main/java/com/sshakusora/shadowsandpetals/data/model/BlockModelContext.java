package com.sshakusora.shadowsandpetals.data.model;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

public record BlockModelContext<B extends Block>(Identifier id, DeferredBlock<B> entry) {
    public String name() {
        return id.getPath();
    }

    public B get() {
        return entry.get();
    }
}
