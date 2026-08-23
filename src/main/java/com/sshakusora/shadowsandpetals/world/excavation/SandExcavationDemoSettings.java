package com.sshakusora.shadowsandpetals.world.excavation;

/**
 * Temporary overrides for demonstrating sand excavation.
 *
 * <p>Set {@link #ENABLED} to {@code false} after the demonstration to restore
 * the normal beach/water requirements and the production cooldown durations.</p>
 */
public final class SandExcavationDemoSettings {
    public static final boolean ENABLED = true;
    public static final long COOLDOWN_TICKS = 100L;

    private SandExcavationDemoSettings() {
    }
}
