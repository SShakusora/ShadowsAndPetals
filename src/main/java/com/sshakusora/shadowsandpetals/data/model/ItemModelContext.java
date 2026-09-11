package com.sshakusora.shadowsandpetals.data.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public record ItemModelContext<I extends Item>(ResourceLocation id, DeferredItem<I> entry) {
    public String name() {
        return id.getPath();
    }

    public I get() {
        return entry.get();
    }
}