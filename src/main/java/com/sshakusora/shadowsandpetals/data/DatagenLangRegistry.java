package com.sshakusora.shadowsandpetals.data;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DatagenLangRegistry {
    private static final Map<String, String> EN_US = new LinkedHashMap<>();
    private static final Map<String, String> EN_US_FALLBACKS = new LinkedHashMap<>();

    private DatagenLangRegistry() {}

    public static void add(String key, String value) {
        EN_US.put(key, value);
    }

    public static void addFallback(String key, String path) {
        EN_US_FALLBACKS.putIfAbsent(key, path);
    }

    public static String get(String key) {
        return EN_US.get(key);
    }

    public static Map<String, String> fallbacks() {
        return EN_US_FALLBACKS;
    }
}
