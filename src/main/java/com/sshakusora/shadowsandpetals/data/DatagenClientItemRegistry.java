package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DatagenClientItemRegistry {
    private static final Identifier VANILLA_MODEL_TYPE = Identifier.withDefaultNamespace("model");
    private static final Map<Identifier, Entry> ENTRIES = new LinkedHashMap<>();

    private DatagenClientItemRegistry() {}

    public static void add(Identifier itemId, Identifier modelId) {
        ENTRIES.put(itemId, Entry.vanillaModel(modelId));
    }

    public static void addCustomModel(Identifier itemId, Identifier modelType) {
        ENTRIES.put(itemId, Entry.customModel(modelType));
    }

    public static Map<Identifier, Entry> entries() {
        return ENTRIES;
    }

    public record Entry(Identifier type, Identifier modelId) {
        public static Entry vanillaModel(Identifier modelId) {
            return new Entry(VANILLA_MODEL_TYPE, modelId);
        }

        public static Entry customModel(Identifier modelType) {
            return new Entry(modelType, null);
        }
    }
}
