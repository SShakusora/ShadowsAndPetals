package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.CafeChairBlock;
import com.sshakusora.shadowsandpetals.compat.jade.CafeChairBlockComponentProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Map;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, ShadowsAndPetals.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(CafeChairBlockComponentProvider.DYEABLE_TOOLTIP_KEY, "Dyeable");
        add(CafeChairBlock.DYE_HINT_MESSAGE_KEY, "%s can be dyed %s");
        add("config.jade.plugin_shadowsandpetals.jade.cafe_chair_dyeable", "Dyeable");
        add("config.jade.plugin_shadowsandpetals.jade.cafe_chair_dyeable_desc", "Show whether cafe chairs can be recolored with dye.");

        for (Map.Entry<String, String> entry : DatagenLangRegistry.fallbacks().entrySet()) {
            addWithFallback(entry.getKey(), entry.getValue());
        }
    }

    private void addWithFallback(String key, String path) {
        String explicitValue = DatagenLangRegistry.get(key);
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
