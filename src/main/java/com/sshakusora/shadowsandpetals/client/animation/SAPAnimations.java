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
            SAPAnimationRegistries.blockAnimation("curtain/upper_right")
                    .rig("curtain/upper_right")
                    .controller("curtain/upper_right")
                    .clip("curtain/upper_right/opening")
                    .clip("curtain/upper_right/closing")
                    .defaultState("open")
                    .register();

    public static final BlockAnimationDefinition CURTAIN_LOWER_RIGHT =
            SAPAnimationRegistries.blockAnimation("curtain/lower_right")
                    .rig("curtain/lower_right")
                    .controller("curtain/lower_right")
                    .clip("curtain/lower_right/opening")
                    .clip("curtain/lower_right/closing")
                    .defaultState("open")
                    .register();

    public static final BlockAnimationDefinition CURTAIN_UPPER_LEFT =
            SAPAnimationRegistries.blockAnimation("curtain/upper_left")
                    .rig("curtain/upper_left")
                    .controller("curtain/upper_left")
                    .clip("curtain/upper_left/opening")
                    .clip("curtain/upper_left/closing")
                    .defaultState("open")
                    .register();

    public static final BlockAnimationDefinition CURTAIN_LOWER_LEFT =
            SAPAnimationRegistries.blockAnimation("curtain/lower_left")
                    .rig("curtain/lower_left")
                    .controller("curtain/lower_left")
                    .clip("curtain/lower_left/opening")
                    .clip("curtain/lower_left/closing")
                    .defaultState("open")
                    .register();

    private SAPAnimations() {
    }

    /**
     * Triggers static registration before animation resources are reloaded.
     */
    public static void init() {
    }
}
