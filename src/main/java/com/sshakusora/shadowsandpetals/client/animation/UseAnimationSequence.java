package com.sshakusora.shadowsandpetals.client.animation;

import java.util.Objects;

/**
 * Three-stage lifecycle used by held-item animations.
 */
public record UseAnimationSequence(
        AnimationResourceRef.State intro,
        AnimationResourceRef.State loop,
        AnimationResourceRef.State outro
) {
    public UseAnimationSequence {
        Objects.requireNonNull(intro, "intro");
        Objects.requireNonNull(loop, "loop");
        Objects.requireNonNull(outro, "outro");
        var controller = intro.controller();
        if (!loop.controller().equals(controller) || !outro.controller().equals(controller)) {
            throw new IllegalArgumentException(
                    "Use-animation sequence states must use one controller");
        }
    }

    public AnimationResourceRef.State state(UseAnimationLifecycle.Phase phase) {
        return switch (phase) {
            case INTRO -> intro;
            case LOOP -> loop;
            case OUTRO -> outro;
            case FINISHED -> throw new IllegalArgumentException(
                    "A finished use-animation sequence has no state");
        };
    }
}
