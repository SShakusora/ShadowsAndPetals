package com.sshakusora.shadowsandpetals.client.animation;

/**
 * Central registration point for reusable use-animation profiles. Item classes
 * reference registered profiles but keep trigger, duration, state and time
 * selection in their own business logic.
 */
public final class SAPAnimations {
    public static final UseAnimationProfile HAMMER =
            SAPAnimationRegistries.useAnimation("hammer")
                    .firstPerson()
                    .thirdPerson()
                    .register();

    private SAPAnimations() {
    }

    /**
     * Triggers static registration before animation resources are reloaded.
     */
    public static void init() {
    }
}
