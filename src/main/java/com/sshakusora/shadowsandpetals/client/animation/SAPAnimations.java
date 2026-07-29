package com.sshakusora.shadowsandpetals.client.animation;

/**
 * Central registration point for reusable use-animation profiles. Item classes
 * reference registered profiles but keep trigger, duration, state and time
 * selection in their own business logic.
 */
public final class SAPAnimations {
    private static boolean registered;

    private SAPAnimations() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
    }
}
