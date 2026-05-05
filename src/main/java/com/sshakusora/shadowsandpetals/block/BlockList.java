package com.sshakusora.shadowsandpetals.block;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Base class for block lists indexed by enum constants (e.g., dye colors, wood types).
 *
 * @param <K> the enum type used as the key
 * @param <T> the block type stored in the list
 */
public abstract class BlockList<K extends Enum<K>, T extends Block> implements Iterable<DeferredBlock<T>> {

    protected final DeferredBlock<?>[] values;

    protected BlockList(Class<K> keyClass, Function<K, DeferredBlock<? extends T>> filler) {
        K[] constants = keyClass.getEnumConstants();
        this.values = new DeferredBlock<?>[constants.length];
        for (K key : constants) {
            values[key.ordinal()] = filler.apply(key);
        }
    }

    @SuppressWarnings("unchecked")
    protected DeferredBlock<T> getByOrdinal(int ordinal) {
        return (DeferredBlock<T>) values[ordinal];
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
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return (DeferredBlock<T>) values[index++];
            }
        };
    }
}
