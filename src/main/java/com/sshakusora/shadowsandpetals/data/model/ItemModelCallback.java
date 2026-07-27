package com.sshakusora.shadowsandpetals.data.model;

import net.minecraft.world.item.Item;

@FunctionalInterface
public interface ItemModelCallback<I extends Item> {
    void generate(ItemModelContext<I> context, SAPItemModelGenerator generator);
}
