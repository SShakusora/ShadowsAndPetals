package com.sshakusora.shadowsandpetals.world.excavation;

/** Eight-phase lunar cycle used by the excavation chance rules. */
public enum MoonPhase {
    FULL_MOON,
    WANING_GIBBOUS,
    THIRD_QUARTER,
    WANING_CRESCENT,
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS;

    public static final int COUNT = values().length;
    public static final long PHASE_LENGTH = 24_000L;
}
