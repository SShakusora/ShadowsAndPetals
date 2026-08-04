package com.sshakusora.shadowsandpetals.compat;

import net.neoforged.fml.ModList;

public final class CompatManager {
    private CompatManager() {
    }

    public static boolean isLoaded(String modId) {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(modId);
    }

    public static boolean isSereneSeasonsLoaded() {
        return isLoaded(CompatInfo.SERENE_SEASONS);
    }
}
