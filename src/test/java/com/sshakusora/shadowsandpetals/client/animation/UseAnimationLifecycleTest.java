package com.sshakusora.shadowsandpetals.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UseAnimationLifecycleTest {
    @Test
    void introAutomaticallyAdvancesToLoopAtItsExactEnd() {
        var lifecycle = lifecycle();

        assertNull(lifecycle.update(true, 0.249F));
        assertEquals(
                new UseAnimationLifecycle.Transition(
                        UseAnimationLifecycle.Phase.INTRO,
                        UseAnimationLifecycle.Phase.LOOP,
                        0.25F),
                lifecycle.update(true, 0.25F));
    }

    @Test
    void earlyReleaseEntersAndCompletesOutro() {
        var lifecycle = lifecycle();

        assertEquals(
                UseAnimationLifecycle.Phase.OUTRO,
                lifecycle.update(false, 0.1F).to());
        assertNull(lifecycle.update(false, 0.349F));
        assertEquals(
                new UseAnimationLifecycle.Transition(
                        UseAnimationLifecycle.Phase.OUTRO,
                        UseAnimationLifecycle.Phase.FINISHED,
                        0.35F),
                lifecycle.update(false, 0.35F));
    }

    @Test
    void releaseFromLoopEntersOutro() {
        var lifecycle = lifecycle();
        lifecycle.update(true, 0.25F);

        assertEquals(
                UseAnimationLifecycle.Phase.OUTRO,
                lifecycle.update(false, 1.0F).to());
    }

    @Test
    void repressDuringOutroRestartsIntro() {
        var lifecycle = lifecycle();
        lifecycle.update(false, 0.1F);

        assertEquals(
                new UseAnimationLifecycle.Transition(
                        UseAnimationLifecycle.Phase.OUTRO,
                        UseAnimationLifecycle.Phase.INTRO,
                        0.2F),
                lifecycle.update(true, 0.2F));
    }

    private static UseAnimationLifecycle lifecycle() {
        return new UseAnimationLifecycle(0.0F, 0.25F, 0.25F);
    }
}
