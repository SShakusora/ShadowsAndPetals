package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DatagenBlockStateRegistry {
    private static final Map<ResourceLocation, Consumer<ModBlockStateProvider>> BLOCK_STATES = new LinkedHashMap<>();

    private DatagenBlockStateRegistry() {}

    public static void add(ResourceLocation id, Consumer<ModBlockStateProvider> generator) {
        BLOCK_STATES.put(id, generator);
    }

    public static Iterable<Consumer<ModBlockStateProvider>> generators() {
        return BLOCK_STATES.values();
    }
}
