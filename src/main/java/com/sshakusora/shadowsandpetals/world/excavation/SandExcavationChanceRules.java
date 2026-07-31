package com.sshakusora.shadowsandpetals.world.excavation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.MoonPhase;

public final class SandExcavationChanceRules {
    public static final float MINIMUM_SEAFOOD_CHANCE = 0.25F;
    public static final float SPRING_TIDE_SEAFOOD_CHANCE = 0.75F;
    public static final float NORMAL_TIDE_SEAFOOD_CHANCE = 0.50F;
    public static final float NEAP_TIDE_SEAFOOD_CHANCE = MINIMUM_SEAFOOD_CHANCE;
    public static final float TRASH_CHANCE = 0.15F;

    private static final long NIGHT_START_TICK = 13_000L;

    private SandExcavationChanceRules() {
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

    public static float getSeafoodChance(ServerLevel level) {
        if (!supportsLunarTides(level)) {
            return MINIMUM_SEAFOOD_CHANCE;
        }

        return getSeafoodChance(getEffectiveMoonPhase(level));
    }

    static float getSeafoodChance(MoonPhase phase) {
        return switch (phase) {
            case FULL_MOON, NEW_MOON -> SPRING_TIDE_SEAFOOD_CHANCE;
            case FIRST_QUARTER, THIRD_QUARTER -> NEAP_TIDE_SEAFOOD_CHANCE;
            default -> NORMAL_TIDE_SEAFOOD_CHANCE;
        };
    }

    public static SandExcavationResult.Category rollCategory(ServerLevel level) {
        return selectCategory(level.getRandom().nextFloat(), getSeafoodChance(level));
    }

    static SandExcavationResult.Category selectCategory(float roll, float seafoodChance) {
        if (roll < 0.0F || roll >= 1.0F) {
            throw new IllegalArgumentException("roll must be in [0, 1)");
        }
        if (seafoodChance < 0.0F || seafoodChance + TRASH_CHANCE > 1.0F) {
            throw new IllegalArgumentException("category chances must fit in [0, 1]");
        }
        if (roll < seafoodChance) {
            return SandExcavationResult.Category.SEAFOOD;
        }
        if (roll < seafoodChance + TRASH_CHANCE) {
            return SandExcavationResult.Category.TRASH;
        }
        return SandExcavationResult.Category.EMPTY;
    }

    private static boolean supportsLunarTides(ServerLevel level) {
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime();
    }
}
