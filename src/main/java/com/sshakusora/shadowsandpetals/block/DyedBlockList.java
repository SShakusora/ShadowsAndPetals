package com.sshakusora.shadowsandpetals.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class DyedBlockList<T extends Block> implements Iterable<DeferredBlock<T>> {

    private static final int COLOR_AMOUNT = DyeColor.values().length;

    private final DeferredBlock<?>[] values = new DeferredBlock<?>[COLOR_AMOUNT];

    public DyedBlockList(Function<DyeColor, DeferredBlock<? extends T>> filler) {
        for (DyeColor color : DyeColor.values()) {
            values[color.ordinal()] = filler.apply(color);
        }
    }

    @SuppressWarnings("unchecked")
    public DeferredBlock<T> get(DyeColor color) {
        return (DeferredBlock<T>) values[color.ordinal()];
    }

    public boolean contains(Block block) {
        for (DeferredBlock<?> entry : values) {
            if (entry.get() == block) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public DeferredBlock<T>[] toArray() {
        return (DeferredBlock<T>[]) Arrays.copyOf(values, values.length);
    }

    @Override
    public Iterator<DeferredBlock<T>> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < values.length;
            }

            @SuppressWarnings("unchecked")
            @Override
            public DeferredBlock<T> next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return (DeferredBlock<T>) values[index++];
            }
        };
    }

}
