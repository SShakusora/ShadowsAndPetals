package com.sshakusora.shadowsandpetals.data;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DatagenLangRegistry {
    private static final Map<String, String> EN_US = new LinkedHashMap<>();

    private DatagenLangRegistry() {}

    public static void add(String key, String value) {
        EN_US.put(key, value);
    }

    public static String get(String key) {
        return EN_US.get(key);
    }
}
