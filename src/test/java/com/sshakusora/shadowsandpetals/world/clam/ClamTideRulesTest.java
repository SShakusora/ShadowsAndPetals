package com.sshakusora.shadowsandpetals.world.clam;

import net.minecraft.world.level.MoonPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClamTideRulesTest {
    @Test
    void usesConfiguredChanceTiers() {
        assertEquals(0.25F, ClamTideRules.NEAP_TIDE_CHANCE);
        assertEquals(0.50F, ClamTideRules.NORMAL_TIDE_CHANCE);
        assertEquals(0.75F, ClamTideRules.SPRING_TIDE_CHANCE);
    }

    @Test
    void keepsTonightsMoonPhaseThroughTheFollowingDaylight() {
        assertEquals(MoonPhase.WAXING_GIBBOUS, ClamTideRules.getEffectiveMoonPhase(12_999L));
        assertEquals(MoonPhase.FULL_MOON, ClamTideRules.getEffectiveMoonPhase(13_000L));
        assertEquals(MoonPhase.FULL_MOON, ClamTideRules.getEffectiveMoonPhase(36_999L));
        assertEquals(MoonPhase.WANING_GIBBOUS, ClamTideRules.getEffectiveMoonPhase(37_000L));
    }

    @Test
    void springAndNeapTidesUseSymmetricMoonPhaseChances() {
        assertEquals(ClamTideRules.SPRING_TIDE_CHANCE, ClamTideRules.getDropChance(MoonPhase.FULL_MOON));
        assertEquals(ClamTideRules.SPRING_TIDE_CHANCE, ClamTideRules.getDropChance(MoonPhase.NEW_MOON));
        assertEquals(ClamTideRules.NEAP_TIDE_CHANCE, ClamTideRules.getDropChance(MoonPhase.FIRST_QUARTER));
        assertEquals(ClamTideRules.NEAP_TIDE_CHANCE, ClamTideRules.getDropChance(MoonPhase.THIRD_QUARTER));
        assertEquals(ClamTideRules.NORMAL_TIDE_CHANCE, ClamTideRules.getDropChance(MoonPhase.WAXING_CRESCENT));
        assertEquals(ClamTideRules.NORMAL_TIDE_CHANCE, ClamTideRules.getDropChance(MoonPhase.WAXING_GIBBOUS));
        assertEquals(ClamTideRules.NORMAL_TIDE_CHANCE, ClamTideRules.getDropChance(MoonPhase.WANING_CRESCENT));
        assertEquals(ClamTideRules.NORMAL_TIDE_CHANCE, ClamTideRules.getDropChance(MoonPhase.WANING_GIBBOUS));
    }

    @Test
    void lunarCycleNeverDropsBelowTheMinimumChance() {
        for (MoonPhase phase : MoonPhase.values()) {
            assertTrue(ClamTideRules.getDropChance(phase) >= ClamTideRules.MINIMUM_DROP_CHANCE);
        }
    }
}
