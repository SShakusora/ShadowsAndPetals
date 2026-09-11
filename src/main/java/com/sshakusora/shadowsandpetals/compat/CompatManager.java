package com.sshakusora.shadowsandpetals.compat;

/**
 * Optional-mod gate retained for 1.21.1.  Integrations are disabled when the
 * corresponding mod is absent, which is the normal migration configuration.
 */
public final class CompatManager {
    private CompatManager() {}

    public static boolean isSereneSeasonsLoaded() {
        return false;
    }
}
