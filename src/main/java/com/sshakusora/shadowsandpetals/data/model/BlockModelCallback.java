package com.sshakusora.shadowsandpetals.data.model;

import net.minecraft.world.level.block.Block;

@FunctionalInterface
public interface BlockModelCallback<B extends Block> {
    void generate(BlockModelContext<B> context, SAPBlockModelGenerator generator);
}
