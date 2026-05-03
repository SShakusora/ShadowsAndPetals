package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DatagenBlockLootRegistry {
    private static final Map<ResourceLocation, Consumer<ModBlockLootProvider>> GENERATORS = new LinkedHashMap<>();

    private DatagenBlockLootRegistry() {}

    public static void add(ResourceLocation id, Consumer<ModBlockLootProvider> generator) {
        if (GENERATORS.putIfAbsent(id, generator) != null) {
            throw new IllegalStateException("Duplicate block loot datagen generator registered for " + id);
        }
    }

    public static Collection<Consumer<ModBlockLootProvider>> generators() {
        return GENERATORS.values();
    }
}
