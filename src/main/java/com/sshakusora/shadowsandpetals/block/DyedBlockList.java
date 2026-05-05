package com.sshakusora.shadowsandpetals.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class DyedBlockList<T extends Block> extends BlockList<DyeColor, T> {

    public DyedBlockList(Function<DyeColor, DeferredBlock<? extends T>> filler) {
        super(DyeColor.class, filler);
    }

    public DeferredBlock<T> get(DyeColor color) {
        return getByOrdinal(color.ordinal());
    }

}
