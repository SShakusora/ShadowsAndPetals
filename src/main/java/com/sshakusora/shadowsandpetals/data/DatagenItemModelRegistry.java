package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DatagenItemModelRegistry {
    private static final Map<ResourceLocation, Consumer<ModItemModelProvider>> GENERATORS = new LinkedHashMap<>();

    private DatagenItemModelRegistry() {}

    public static void add(ResourceLocation id, Consumer<ModItemModelProvider> generator) {
        GENERATORS.put(id, generator);
    }

    public static Iterable<Consumer<ModItemModelProvider>> generators() {
        return GENERATORS.values();
    }
}
