package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CreativeTabContentsRegistry {
    private static final Map<CreativeTabType, List<Supplier<? extends ItemLike>>> CONTENTS = new EnumMap<>(CreativeTabType.class);

    private CreativeTabContentsRegistry() {}

    public static void add(CreativeTabType tab, Supplier<? extends ItemLike> supplier) {
        CONTENTS.computeIfAbsent(tab, ignored -> new ArrayList<>()).add(supplier);
    }

    public static Consumer<CreativeModeTab.Output> generator(CreativeTabType tab) {
        return output -> {
            for (Supplier<? extends ItemLike> supplier : CONTENTS.getOrDefault(tab, List.of())) {
                output.accept(supplier.get());
            }
        };
    }
}
