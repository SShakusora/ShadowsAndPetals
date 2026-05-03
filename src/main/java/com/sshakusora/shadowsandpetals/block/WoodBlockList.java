package com.sshakusora.shadowsandpetals.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class WoodBlockList<T extends Block> implements Iterable<DeferredBlock<T>> {

    private static final int WOOD_AMOUNT = WoodType.values().length;

    private final DeferredBlock<?>[] values = new DeferredBlock<?>[WOOD_AMOUNT];

    public WoodBlockList(Function<WoodType, DeferredBlock<? extends T>> filler) {
        for (WoodType type : WoodType.values()) {
            values[type.ordinal()] = filler.apply(type);
        }
    }

    @SuppressWarnings("unchecked")
    public DeferredBlock<T> get(WoodType type) {
        return (DeferredBlock<T>) values[type.ordinal()];
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

    public enum WoodType {
        OAK("oak", Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_SLAB),
        SPRUCE("spruce", Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB),
        BIRCH("birch", Blocks.BIRCH_PLANKS, Blocks.BIRCH_STAIRS, Blocks.BIRCH_SLAB),
        JUNGLE("jungle", Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB),
        ACACIA("acacia", Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB),
        DARK_OAK("dark_oak", Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB),
        MANGROVE("mangrove", Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB),
        CHERRY("cherry", Blocks.CHERRY_PLANKS, Blocks.CHERRY_STAIRS, Blocks.CHERRY_SLAB),
        BAMBOO("bamboo", Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_SLAB),
        CRIMSON("crimson", Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_SLAB),
        WARPED("warped", Blocks.WARPED_PLANKS, Blocks.WARPED_STAIRS, Blocks.WARPED_SLAB);

        private final String name;
        private final Block planks;
        private final Block stairs;
        private final Block slab;

        WoodType(String name, Block planks, Block stairs, Block slab) {
            this.name = name;
            this.planks = planks;
            this.stairs = stairs;
            this.slab = slab;
        }

        public String getName() {
            return name;
        }

        public Block getPlanks() {
            return planks;
        }

        public Block getStairs() {
            return stairs;
        }

        public Block getSlab() {
            return slab;
        }
    }
}
