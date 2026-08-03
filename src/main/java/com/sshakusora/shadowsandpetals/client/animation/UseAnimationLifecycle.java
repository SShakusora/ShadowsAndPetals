package com.sshakusora.shadowsandpetals.client.animation;

import org.jspecify.annotations.Nullable;

/**
 * Pure timing state machine for intro -> loop -> outro playback.
 */
public final class UseAnimationLifecycle {
    private final float introDurationSeconds;
    private final float outroDurationSeconds;
    private Phase phase = Phase.INTRO;
    private float phaseStartSeconds;

    public UseAnimationLifecycle(
            float startSeconds,
            float introDurationSeconds,
            float outroDurationSeconds
    ) {
        requireTime(startSeconds, "start time");
        this.introDurationSeconds = requireDuration(
                introDurationSeconds, "intro duration");
        this.outroDurationSeconds = requireDuration(
                outroDurationSeconds, "outro duration");
        this.phaseStartSeconds = startSeconds;
    }

    public Phase phase() {
        return phase;
    }

    public float phaseStartSeconds() {
        return phaseStartSeconds;
    }

    public @Nullable Transition update(boolean using, float nowSeconds) {
        requireTime(nowSeconds, "current time");
        if (phase == Phase.FINISHED) {
            return null;
        }

        if (!using) {
            if (phase != Phase.OUTRO) {
                return transitionTo(Phase.OUTRO, nowSeconds);
            }
            float endSeconds = phaseStartSeconds + outroDurationSeconds;
            return nowSeconds >= endSeconds
                    ? transitionTo(Phase.FINISHED, endSeconds)
                    : null;
        }

        if (phase == Phase.OUTRO) {
            return transitionTo(Phase.INTRO, nowSeconds);
        }
        if (phase == Phase.INTRO) {
            float introEndSeconds = phaseStartSeconds + introDurationSeconds;
            if (nowSeconds >= introEndSeconds) {
                return transitionTo(Phase.LOOP, introEndSeconds);
            }
        }
        return null;
    }

    private Transition transitionTo(Phase next, float atSeconds) {
        Phase previous = phase;
        phase = next;
        phaseStartSeconds = atSeconds;
        return new Transition(previous, next, atSeconds);
    }

    private static float requireDuration(float value, String description) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(
                    "Use-animation " + description + " must be finite and positive");
        }
        return value;
    }

    private static void requireTime(float value, String description) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException(
                    "Use-animation " + description + " must be finite and non-negative");
        }
    }

    public enum Phase {
        INTRO,
        LOOP,
        OUTRO,
        FINISHED
    }

    public record Transition(Phase from, Phase to, float atSeconds) {
    }
}
