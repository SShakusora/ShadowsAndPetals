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

    public static final BlockAnimationDefinition CURTAIN_UPPER_RIGHT =
            SAPAnimationRegistries.blockAnimation("long_curtain/upper_right")
                    .rig("long_curtain/upper_right")
                    .controller("long_curtain/upper_right")
                    .clip("long_curtain/upper_right/opening")
                    .clip("long_curtain/upper_right/closing")
                    .defaultState("open")
                    .register();

    public static final BlockAnimationDefinition CURTAIN_LOWER_RIGHT =
            SAPAnimationRegistries.blockAnimation("long_curtain/lower_right")
                    .rig("long_curtain/lower_right")
                    .controller("long_curtain/lower_right")
                    .clip("long_curtain/lower_right/opening")
                    .clip("long_curtain/lower_right/closing")
                    .defaultState("open")
                    .register();

    public static final BlockAnimationDefinition CURTAIN_UPPER_LEFT =
            SAPAnimationRegistries.blockAnimation("long_curtain/upper_left")
                    .rig("long_curtain/upper_left")
                    .controller("long_curtain/upper_left")
                    .clip("long_curtain/upper_left/opening")
                    .clip("long_curtain/upper_left/closing")
                    .defaultState("open")
                    .register();

    public static final BlockAnimationDefinition CURTAIN_LOWER_LEFT =
            SAPAnimationRegistries.blockAnimation("long_curtain/lower_left")
                    .rig("long_curtain/lower_left")
                    .controller("long_curtain/lower_left")
                    .clip("long_curtain/lower_left/opening")
                    .clip("long_curtain/lower_left/closing")
                    .defaultState("open")
                    .register();

    public static final BlockAnimationDefinition LARGE_CURTAIN_RIGHT =
            SAPAnimationRegistries.blockAnimation("large_curtain/right")
                    .rig("large_curtain/right")
                    .controller("large_curtain/right")
                    .clip("large_curtain/right/opening")
                    .clip("large_curtain/right/closing")
                    .defaultState("closed")
                    .register();

    public static final BlockAnimationDefinition LARGE_CURTAIN_LEFT =
            SAPAnimationRegistries.blockAnimation("large_curtain/left")
                    .rig("large_curtain/left")
                    .controller("large_curtain/left")
                    .clip("large_curtain/left/opening")
                    .clip("large_curtain/left/closing")
                    .defaultState("closed")
                    .register();

    private SAPAnimations() {
    }

    /**
     * Triggers static registration before animation resources are reloaded.
     */
    public static void init() {
    }
}
