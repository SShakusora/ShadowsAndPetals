package com.sshakusora.shadowsandpetals.world.clam;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.MoonPhase;

/**
 * Approximates lunar tides for clam harvesting. A tide period starts at night
 * and remains active through the following daylight period.
 */
public final class ClamTideRules {
    public static final float MINIMUM_DROP_CHANCE = 0.25F;
    public static final float SPRING_TIDE_CHANCE = 0.75F;
    public static final float NORMAL_TIDE_CHANCE = 0.50F;
    public static final float NEAP_TIDE_CHANCE = MINIMUM_DROP_CHANCE;

    private static final long NIGHT_START_TICK = 13_000L;

    private ClamTideRules() {
    }

    public static MoonPhase getEffectiveMoonPhase(ServerLevel level) {
        return getEffectiveMoonPhase(level.getOverworldClockTime());
    }

    static MoonPhase getEffectiveMoonPhase(long overworldClockTime) {
        long tidePeriod = Math.floorDiv(
                overworldClockTime - NIGHT_START_TICK,
                MoonPhase.PHASE_LENGTH
        );
        int phaseIndex = Math.floorMod(tidePeriod, MoonPhase.COUNT);
        return MoonPhase.values()[phaseIndex];
    }

    public static float getDropChance(ServerLevel level) {
        if (!supportsLunarTides(level)) {
            return MINIMUM_DROP_CHANCE;
        }

        return getDropChance(getEffectiveMoonPhase(level));
    }

    static float getDropChance(MoonPhase phase) {
        return switch (phase) {
            case FULL_MOON, NEW_MOON -> SPRING_TIDE_CHANCE;
            case FIRST_QUARTER, THIRD_QUARTER -> NEAP_TIDE_CHANCE;
            default -> NORMAL_TIDE_CHANCE;
        };
    }

    public static boolean rollClam(ServerLevel level) {
        return level.getRandom().nextFloat() < getDropChance(level);
    }

    private static boolean supportsLunarTides(ServerLevel level) {
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime();
    }
}
