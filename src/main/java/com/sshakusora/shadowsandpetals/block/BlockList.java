package com.sshakusora.shadowsandpetals.block;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Base class for block lists indexed by enum constants (e.g., dye colors, wood types).
 *
 * @param <K> the enum type used as the key
 * @param <T> the value type stored in the list
 */
public abstract class BlockList<K extends Enum<K>, T> implements Iterable<T> {

    protected final Object[] values;

    protected BlockList(Class<K> keyClass, Function<K, ? extends T> filler) {
        K[] constants = keyClass.getEnumConstants();
        this.values = new Object[constants.length];
        for (K key : constants) {
            values[key.ordinal()] = filler.apply(key);
        }
    }

    @SuppressWarnings("unchecked")
    protected T getByOrdinal(int ordinal) {
        return (T) values[ordinal];
    }

    @SuppressWarnings("unchecked")
    public Object[] toArray() {
        return Arrays.copyOf(values, values.length);
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < values.length;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return (T) values[index++];
            }
        };
    }
}
