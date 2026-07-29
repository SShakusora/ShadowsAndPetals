package com.sshakusora.shadowsandpetals.client.animation;

import java.util.Set;

public final class AnimationMixer {
    private AnimationMixer() {
    }

    public static RigPose blend(RigPose from, RigPose to, float weight) {
        return RigPose.blend(from, to, weight, Set.of());
    }

    public static RigPose blend(RigPose from, RigPose to, float weight, Set<String> boneMask) {
        return RigPose.blend(from, to, weight, boneMask);
    }

    public static RigPose additive(RigPose base, RigPose overlay, float weight, Set<String> boneMask) {
        return RigPose.additive(base, overlay, weight, boneMask);
    }
}
