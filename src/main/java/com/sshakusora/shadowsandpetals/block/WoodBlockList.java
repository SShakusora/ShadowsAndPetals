package com.sshakusora.shadowsandpetals.block;

import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

public class WoodBlockList<T extends Block> extends BlockList<WoodBlockList.WoodType, DeferredBlock<T>> {

    public WoodBlockList(Function<WoodType, DeferredBlock<? extends T>> filler) {
        super(WoodType.class, type -> cast(filler.apply(type)));
    }

    public DeferredBlock<T> get(WoodType type) {
        return getByOrdinal(type.ordinal());
    }

    public boolean contains(Block block) {
        for (DeferredBlock<T> entry : this) {
            if (entry.get() == block) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public DeferredBlock<T>[] toArray() {
        return (DeferredBlock<T>[]) Arrays.copyOf(values, values.length, DeferredBlock[].class);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Block> DeferredBlock<T> cast(DeferredBlock<? extends T> block) {
        return (DeferredBlock<T>) block;
    }

    public enum WoodType {
        OAK("oak", "橡木", Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_SLAB),
        SPRUCE("spruce", "云杉木", Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB),
        BIRCH("birch", "白桦木", Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_PLANKS, Blocks.BIRCH_STAIRS, Blocks.BIRCH_SLAB),
        JUNGLE("jungle", "丛林木", Blocks.STRIPPED_JUNGLE_LOG, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB),
        ACACIA("acacia", "金合欢木", Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB),
        DARK_OAK("dark_oak", "深色橡木", Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB),
        MANGROVE("mangrove", "红树木", Blocks.STRIPPED_MANGROVE_LOG, Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB),
        CHERRY("cherry", "樱花木", Blocks.STRIPPED_CHERRY_LOG, Blocks.CHERRY_PLANKS, Blocks.CHERRY_STAIRS, Blocks.CHERRY_SLAB),
        PALE("pale_oak", "苍白橡木", Blocks.STRIPPED_PALE_OAK_LOG, Blocks.PALE_OAK_PLANKS, Blocks.PALE_OAK_STAIRS, Blocks.PALE_OAK_SLAB),
        SAKURA("sakura", "樱", BlockRegistry.SAKURA_SET),
        MAPLE("maple", "枫木", BlockRegistry.MAPLE_SET),
        GINKGO("ginkgo", "银杏木", BlockRegistry.GINKGO_SET),
        BAMBOO("bamboo", "竹", Blocks.STRIPPED_BAMBOO_BLOCK, Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_SLAB),
        CRIMSON("crimson", "绯红木", Blocks.STRIPPED_CRIMSON_STEM, Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_SLAB),
        WARPED("warped", "诡异木", Blocks.STRIPPED_WARPED_STEM, Blocks.WARPED_PLANKS, Blocks.WARPED_STAIRS, Blocks.WARPED_SLAB);

        private final String name;
        private final String zhName;
        private final Supplier<? extends Block> strippedLog;
        private final Supplier<? extends Block> planks;
        private final Supplier<? extends Block> stairs;
        private final Supplier<? extends Block> slab;

        WoodType(String name, String zhName, Block strippedLog, Block planks, Block stairs, Block slab) {
            this(name, zhName, () -> strippedLog, () -> planks, () -> stairs, () -> slab);
        }

        WoodType(String name, String zhName , WoodSetList.WoodSet set) {
            this(name, zhName, set.strippedLog(), set.planks(), set.stairs(), set.slab());
        }

        WoodType(
                String name,
                String zhName,
                Supplier<? extends Block> strippedLog,
                Supplier<? extends Block> planks,
                Supplier<? extends Block> stairs,
                Supplier<? extends Block> slab
        ) {
            this.name = name;
            this.zhName = zhName;
            this.strippedLog = strippedLog;
            this.planks = planks;
            this.stairs = stairs;
            this.slab = slab;
        }

        public String getName() {
            return name;
        }

        public String getZhName() {
            return zhName;
        }

        public Block getStrippedLog() {
            return strippedLog.get();
        }

        public Block getPlanks() {
            return planks.get();
        }

        public Block getStairs() {
            return stairs.get();
        }

        public Block getSlab() {
            return slab.get();
        }
    }
}
