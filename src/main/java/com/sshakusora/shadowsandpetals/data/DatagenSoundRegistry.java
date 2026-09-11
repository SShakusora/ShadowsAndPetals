package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DatagenSoundRegistry {
    private static final Map<ResourceLocation, String> SUBTITLES = new LinkedHashMap<>();

    private DatagenSoundRegistry() {}

    public static void addSubtitle(ResourceLocation soundId, String subtitleKey) {
        SUBTITLES.put(soundId, subtitleKey);
    }

    public static String getSubtitle(ResourceLocation soundId) {
        return SUBTITLES.get(soundId);
    }
}