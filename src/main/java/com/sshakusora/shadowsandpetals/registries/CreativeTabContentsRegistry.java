package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CreativeTabContentsRegistry {
    private static final Map<CreativeTabType, List<Entry>> CONTENTS = new EnumMap<>(CreativeTabType.class);

    private record Entry(Supplier<? extends ItemLike> supplier, CreativeTabOrder order) {}

    private CreativeTabContentsRegistry() {}

    public static void add(CreativeTabType tab, Supplier<? extends ItemLike> supplier) {
        add(tab, supplier, CreativeTabOrder.DEFAULT);
    }

    public static void add(CreativeTabType tab, Supplier<? extends ItemLike> supplier, CreativeTabOrder order) {
        CONTENTS.computeIfAbsent(tab, ignored -> new ArrayList<>()).add(new Entry(supplier, order));
    }

    public static Consumer<CreativeModeTab.Output> generator(CreativeTabType tab) {
        return output -> {
            List<Entry> entries = new ArrayList<>(CONTENTS.getOrDefault(tab, List.of()));
            entries.sort(Comparator.comparingInt(entry -> entry.order().ordinal()));
            for (Entry entry : entries) {
                output.accept(entry.supplier().get());
            }
        };
    }
}
