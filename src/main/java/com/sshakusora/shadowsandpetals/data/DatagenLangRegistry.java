package com.sshakusora.shadowsandpetals.data;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DatagenLangRegistry {
    public static final String DEFAULT_LOCALE = "en_us";

    private static final Map<String, Map<String, String>> TRANSLATIONS = new LinkedHashMap<>();
    private static final Map<String, String> FALLBACKS = new LinkedHashMap<>();

    private DatagenLangRegistry() {}

    public static void add(String key, String value) {
        add(DEFAULT_LOCALE, key, value);
    }

    public static void add(String locale, String key, String value) {
        TRANSLATIONS.computeIfAbsent(locale, ignored -> new LinkedHashMap<>()).put(key, value);
    }

    public static TranslationKey key(String key) {
        return new TranslationKey(key);
    }

    public static void addFallback(String key, String path) {
        FALLBACKS.putIfAbsent(key, path);
    }

    public static String get(String key) {
        return get(DEFAULT_LOCALE, key);
    }

    public static String get(String locale, String key) {
        Map<String, String> translations = TRANSLATIONS.get(locale);
        return translations != null ? translations.get(key) : null;
    }

    public static Map<String, String> fallbacks() {
        return FALLBACKS;
    }

    public static Map<String, String> translations(String locale) {
        return TRANSLATIONS.getOrDefault(locale, Map.of());
    }

    public static final class TranslationKey {
        private final String key;

        private TranslationKey(String key) {
            this.key = key;
        }

        public TranslationKey en_us(String value) {
            add(DEFAULT_LOCALE, key, value);
            return this;
        }

        public TranslationKey zh_cn(String value) {
            add("zh_cn", key, value);
            return this;
        }

        public TranslationKey lang(String locale, String value) {
            add(locale, key, value);
            return this;
        }

        public String key() {
            return key;
        }
    }
}
