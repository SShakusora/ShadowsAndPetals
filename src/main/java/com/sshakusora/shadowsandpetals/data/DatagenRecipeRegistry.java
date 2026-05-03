package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DatagenRecipeRegistry {
    private static final Map<ResourceLocation, Consumer<ModRecipeProvider>> GENERATORS = new LinkedHashMap<>();

    private DatagenRecipeRegistry() {}

    public static void add(ResourceLocation id, Consumer<ModRecipeProvider> generator) {
        if (GENERATORS.putIfAbsent(id, generator) != null) {
            throw new IllegalStateException("Duplicate recipe datagen generator registered for " + id);
        }
    }

    public static Collection<Consumer<ModRecipeProvider>> generators() {
        return GENERATORS.values();
    }
}
