package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Map;

public class ModLanguageProvider extends LanguageProvider {
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, ShadowsAndPetals.MOD_ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        BuiltinLanguageKeys.bootstrap();

        for (Map.Entry<String, String> entry : DatagenLangRegistry.translations(locale).entrySet()) {
            add(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : DatagenLangRegistry.fallbacks().entrySet()) {
            if (DatagenLangRegistry.get(locale, entry.getKey()) != null) {
                continue;
            }
            addWithFallback(entry.getKey(), entry.getValue());
        }
    }

    private void addWithFallback(String key, String path) {
        String explicitValue = DatagenLangRegistry.get(locale, key);
        if (explicitValue == null && !DatagenLangRegistry.DEFAULT_LOCALE.equals(locale)) {
            explicitValue = DatagenLangRegistry.get(key);
        }
        add(key, explicitValue != null ? explicitValue : toEnglishName(path));
    }

    private String toEnglishName(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
