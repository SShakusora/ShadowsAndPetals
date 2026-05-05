package com.sshakusora.shadowsandpetals.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class WoodBlockList<T extends Block> extends BlockList<WoodBlockList.WoodType, T> {

    public WoodBlockList(Function<WoodType, DeferredBlock<? extends T>> filler) {
        super(WoodType.class, filler);
    }

    public DeferredBlock<T> get(WoodType type) {
        return getByOrdinal(type.ordinal());
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
