package com.sshakusora.shadowsandpetals.data.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

public record BlockModelContext<B extends Block>(ResourceLocation id, DeferredBlock<B> entry) {
    public String name() {
        return id.getPath();
    }

    public B get() {
        return entry.get();
    }
}