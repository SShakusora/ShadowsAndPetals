package com.sshakusora.shadowsandpetals.compat.create.schematic;

/**
 * Entry point for Create schematic compatibility.
 *
 * <p>This class is loaded reflectively only when Create is present. Keeping
 * the Create API references behind that boundary lets the base mod continue
 * to load when the optional dependency is absent.</p>
 */
public final class Bootstrap {
    private static boolean registered;

    private Bootstrap() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        RockeryCompat.register();
        CurtainCompat.register();
        registered = true;
    }
}
