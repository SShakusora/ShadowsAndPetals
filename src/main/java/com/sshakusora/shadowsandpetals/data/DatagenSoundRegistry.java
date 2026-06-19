package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DatagenSoundRegistry {
    private static final Map<Identifier, String> SUBTITLES = new LinkedHashMap<>();

    private DatagenSoundRegistry() {}

    public static void addSubtitle(Identifier soundId, String subtitleKey) {
        SUBTITLES.put(soundId, subtitleKey);
    }

    public static String getSubtitle(Identifier soundId) {
        return SUBTITLES.get(soundId);
    }
}
