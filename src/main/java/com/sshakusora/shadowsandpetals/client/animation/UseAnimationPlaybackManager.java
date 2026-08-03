package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.world.entity.HumanoidArm;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Client-side playback state shared by first- and third-person rendering.
 */
public final class UseAnimationPlaybackManager {
    public static final UseAnimationPlaybackManager INSTANCE = new UseAnimationPlaybackManager();

    private final Map<Key, Playback> playbacks = new HashMap<>();

    private UseAnimationPlaybackManager() {
    }

    public @Nullable Playback observe(
            int entityId,
            UseAnimationProfile profile,
            boolean using,
            HumanoidArm actualUseArm,
            float nowSeconds
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(actualUseArm, "actualUseArm");
        UseAnimationSequence sequence = profile.sequence();
        if (sequence == null) {
            return null;
        }

        Key key = new Key(entityId, profile.id());
        Playback playback = playbacks.get(key);
        if (playback == null) {
            if (!using) {
                return null;
            }
            playback = new Playback(profile, sequence, actualUseArm, nowSeconds);
            playbacks.put(key, playback);
        }
        playback.observe(using, actualUseArm, nowSeconds);
        if (playback.finished()) {
            playbacks.remove(key);
            return null;
        }
        return playback;
    }

    public @Nullable Playback find(
            int entityId,
            UseAnimationProfile profile,
            float nowSeconds
    ) {
        Key key = new Key(entityId, profile.id());
        Playback playback = playbacks.get(key);
        if (playback == null) {
            return null;
        }
        playback.advance(nowSeconds);
        if (playback.finished()) {
            playbacks.remove(key);
            return null;
        }
        return playback;
    }

    public void retainEntities(
            UseAnimationProfile profile,
            Set<Integer> entityIds
    ) {
        playbacks.keySet().removeIf(key -> key.profileId().equals(profile.id())
                && !entityIds.contains(key.entityId()));
    }

    public void clear() {
        playbacks.clear();
    }

    public static final class Playback {
        private final UseAnimationSequence sequence;
        private final AnimationControllerInstance controller;
        private final UseAnimationLifecycle lifecycle;
        private HumanoidArm actualUseArm;
        private boolean using = true;

        private Playback(
                UseAnimationProfile profile,
                UseAnimationSequence sequence,
                HumanoidArm actualUseArm,
                float nowSeconds
        ) {
            this.sequence = sequence;
            this.actualUseArm = actualUseArm;
            this.controller = new AnimationControllerInstance(
                    profile.controller(), nowSeconds);
            this.controller.play(sequence.intro(), nowSeconds);
            this.lifecycle = new UseAnimationLifecycle(
                    nowSeconds,
                    SAPAnimationResources.INSTANCE.stateDurationSeconds(
                            sequence.intro()),
                    SAPAnimationResources.INSTANCE.stateDurationSeconds(
                            sequence.outro()));
        }

        public HumanoidArm actualUseArm() {
            return actualUseArm;
        }

        public UseAnimationLifecycle.Phase phase() {
            return lifecycle.phase();
        }

        public RigPose sample(float nowSeconds) {
            advance(nowSeconds);
            if (finished()) {
                throw new IllegalStateException(
                        "Cannot sample a finished use animation");
            }
            return controller.sample(nowSeconds);
        }

        private void observe(
                boolean using,
                HumanoidArm actualUseArm,
                float nowSeconds
        ) {
            if (using) {
                this.actualUseArm = actualUseArm;
            }
            this.using = using;
            advance(nowSeconds);
        }

        private void advance(float nowSeconds) {
            UseAnimationLifecycle.Transition transition =
                    lifecycle.update(using, nowSeconds);
            if (transition != null
                    && transition.to() != UseAnimationLifecycle.Phase.FINISHED) {
                controller.play(
                        sequence.state(transition.to()), transition.atSeconds());
            }
        }

        private boolean finished() {
            return lifecycle.phase() == UseAnimationLifecycle.Phase.FINISHED;
        }
    }

    private record Key(int entityId, net.minecraft.resources.Identifier profileId) {
    }
}
