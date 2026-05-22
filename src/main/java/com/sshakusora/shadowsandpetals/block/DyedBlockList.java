package com.sshakusora.shadowsandpetals.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Arrays;
import java.util.function.Function;

public class DyedBlockList<T extends Block> extends BlockList<DyeColor, DeferredBlock<T>> {

    public DyedBlockList(Function<DyeColor, DeferredBlock<? extends T>> filler) {
        super(DyeColor.class, color -> cast(filler.apply(color)));
    }

    public DeferredBlock<T> get(DyeColor color) {
        return getByOrdinal(color.ordinal());
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

    public static String zhName(DyeColor color) {
        return switch (color) {
            case WHITE -> "白色";
            case ORANGE -> "橙色";
            case MAGENTA -> "品红色";
            case LIGHT_BLUE -> "淡蓝色";
            case YELLOW -> "黄色";
            case LIME -> "黄绿色";
            case PINK -> "粉红色";
            case GRAY -> "灰色";
            case LIGHT_GRAY -> "淡灰色";
            case CYAN -> "青色";
            case PURPLE -> "紫色";
            case BLUE -> "蓝色";
            case BROWN -> "棕色";
            case GREEN -> "绿色";
            case RED -> "红色";
            case BLACK -> "黑色";
        };
    }

}
