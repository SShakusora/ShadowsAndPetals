package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.legacy.LegacyCompatIds;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DatagenBlockLootRegistry {
    private static final Map<Identifier, Consumer<ModBlockLootProvider>> GENERATORS = new LinkedHashMap<>();

    private DatagenBlockLootRegistry() {}

    public static void add(Identifier id, Consumer<ModBlockLootProvider> generator) {
        if (LegacyCompatIds.isLegacyCompatId(id)) {
            return;
        }
        if (GENERATORS.putIfAbsent(id, generator) != null) {
            throw new IllegalStateException("Duplicate block loot datagen generator registered for " + id);
        }
    }

    public static Collection<Consumer<ModBlockLootProvider>> generators() {
        return GENERATORS.values();
    }
}
