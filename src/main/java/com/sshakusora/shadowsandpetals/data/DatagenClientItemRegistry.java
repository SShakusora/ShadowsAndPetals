package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DatagenClientItemRegistry {
    private static final Map<Identifier, Identifier> ENTRIES = new LinkedHashMap<>();

    private DatagenClientItemRegistry() {}

    public static void add(Identifier itemId, Identifier modelId) {
        ENTRIES.put(itemId, modelId);
    }

    public static Map<Identifier, Identifier> entries() {
        return ENTRIES;
    }
}
