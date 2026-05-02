package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, ShadowsAndPetals.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        SAPRegistries.BLOCKS.getEntries().forEach(entry -> {
            String path = entry.getId().getPath();
            add("block." + ShadowsAndPetals.MOD_ID + "." + path, toEnglishName(path));
        });

        SAPRegistries.ITEMS.getEntries().forEach(entry -> {
            String path = entry.getId().getPath();
            add("item." + ShadowsAndPetals.MOD_ID + "." + path, toEnglishName(path));
        });

        SAPRegistries.CREATIVE_TABS.getEntries().forEach(entry -> {
            String path = entry.getId().getPath();
            add("itemGroup." + ShadowsAndPetals.MOD_ID + "." + path, toEnglishName(path));
        });

        SAPRegistries.ENTITIES.getEntries().forEach(entry -> {
            String path = entry.getId().getPath();
            add("entity." + ShadowsAndPetals.MOD_ID + "." + path, toEnglishName(path));
        });
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
