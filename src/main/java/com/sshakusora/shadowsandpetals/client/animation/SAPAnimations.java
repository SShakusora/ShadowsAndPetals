package com.sshakusora.shadowsandpetals.client.animation;

/**
 * Central registration point for reusable use-animation profiles. Item classes
 * reference registered profiles but keep trigger, duration, state and time
 * selection in their own business logic.
 */
public final class SAPAnimations {
    public static final UseAnimationProfile HAMMER =
            SAPAnimationRegistries.useAnimation("hammer")
                    .clip("use/hammer_intro")
                    .clip("use/hammer")
                    .clip("use/hammer_outro")
                    .sequence(
                            "use/hammer_intro",
                            "use/hammer",
                            "use/hammer_outro")
                    .firstPerson()
                    .thirdPerson()
                    .register();

    public static final UseAnimationProfile HARROW =
            SAPAnimationRegistries.useAnimation("harrow")
                    .clip("use/harrow_intro")
                    .clip("use/harrow")
                    .clip("use/harrow_outro")
                    .sequence(
                            "use/harrow_intro",
                            "use/harrow",
                            "use/harrow_outro")
                    .firstPerson()
                    .thirdPerson()
                    .register();

    public static final BlockAnimationDefinition CURTAIN_UPPER_R =
            SAPAnimationRegistries.blockAnimation("curtain_upper_r")
                    .rig("animation/curtain_upper_r")
                    .controller("animation/curtain_upper_r")
                    .clip("curtain/on")
                    .clip("curtain/off")
                    .defaultState("on")
                    .register();

    public static final BlockAnimationDefinition CURTAIN_LOWER_R =
            SAPAnimationRegistries.blockAnimation("curtain_lower_r")
                    .rig("animation/curtain_lower_r")
                    .controller("animation/curtain_lower_r")
                    .clip("curtain/on_lower")
                    .clip("curtain/off_lower")
                    .defaultState("on")
                    .register();

    private SAPAnimations() {
    }

    /**
     * Triggers static registration before animation resources are reloaded.
     */
    public static void init() {
    }
}
