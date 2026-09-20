package com.sshakusora.shadowsandpetals.compat;

import com.sshakusora.shadowsandpetals.compat.chinjufu.ChinjufuIds;

/** Shared identifiers for integrations that are not tied to one legacy mod's migration rules. */
public final class CompatInfo {
    /** @deprecated use {@link ChinjufuIds#MOD_ID} in Chinjufu-specific code. */
    @Deprecated(forRemoval = false)
    public static final String CHINJUFU_MOD = ChinjufuIds.MOD_ID;

    public static final String SERENE_SEASONS = "sereneseasons";
    public static final String CREATE = "create";

    private CompatInfo() {}
}
