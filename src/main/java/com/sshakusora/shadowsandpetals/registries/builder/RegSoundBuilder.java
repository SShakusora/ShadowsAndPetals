package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.data.DatagenSoundRegistry;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for {@link SoundEvent} registration.
 * <p>
 * Supports automatic subtitle generation ({@code .subtitle()}) with
 * English name auto-derived from the registry path, as well as
 * explicit per-locale subtitle declarations.
 *
 * <pre>{@code
 * SAPRegistries.sound("shishi_odoshi")
 *     .subtitle("Shishi-odoshi clacks", "鹿威：敲击")
 *     .register();
 * }</pre>
 */
public class RegSoundBuilder {
    private final DeferredRegister<SoundEvent> registry;
    private final String name;
    private boolean hasSubtitle;
    private final Map<String, String> subtitleLangs = new LinkedHashMap<>();

    public RegSoundBuilder(DeferredRegister<SoundEvent> registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    /**
     * Enables subtitle generation with an auto-derived English name.
     * <p>
     * The resulting subtitle key is {@code subtitles.shadowsandpetals.<name>}.
     * English text is auto-derived from the registry path (underscores replaced
     * with spaces, each word capitalised), while other locales fall back to
     * either their explicit value or the English text.
     */
    public RegSoundBuilder subtitle() {
        this.hasSubtitle = true;
        return this;
    }

    /**
     * Enables subtitle generation with an explicit English value.
     */
    public RegSoundBuilder subtitle(String en_us) {
        this.hasSubtitle = true;
        this.subtitleLangs.put(DatagenLangRegistry.DEFAULT_LOCALE, en_us);
        return this;
    }

    /**
     * Enables subtitle generation with explicit English and Chinese values.
     */
    public RegSoundBuilder subtitle(String en_us, String zh_cn) {
        this.hasSubtitle = true;
        this.subtitleLangs.put(DatagenLangRegistry.DEFAULT_LOCALE, en_us);
        this.subtitleLangs.put(DatagenLangRegistry.ZH_CN, zh_cn);
        return this;
    }

    /**
     * Adds a locale-specific subtitle value.
     */
    public RegSoundBuilder subtitleLang(String locale, String value) {
        this.hasSubtitle = true;
        this.subtitleLangs.put(locale, value);
        return this;
    }

    /**
     * Finalizes sound event registration and wires subtitle datagen hooks.
     *
     * @return the bound {@link DeferredHolder} for the registered sound
     */
    public DeferredHolder<SoundEvent, SoundEvent> register() {
        var holder = registry.register(name,
                () -> SoundEvent.createVariableRangeEvent(ShadowsAndPetals.asResource(name)));

        if (hasSubtitle) {
            String subtitleKey = "subtitles." + ShadowsAndPetals.MOD_ID + "." + name;
            DatagenLangRegistry.addFallback(subtitleKey, name);
            for (var entry : subtitleLangs.entrySet()) {
                DatagenLangRegistry.add(entry.getKey(), subtitleKey, entry.getValue());
            }
            DatagenSoundRegistry.addSubtitle(holder.getId(), subtitleKey);
        }

        return holder;
    }
}
