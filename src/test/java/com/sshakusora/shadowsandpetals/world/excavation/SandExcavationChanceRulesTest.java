package com.sshakusora.shadowsandpetals.world.excavation;

import net.minecraft.world.level.MoonPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SandExcavationChanceRulesTest {
    @Test
    void usesConfiguredChanceTiers() {
        assertEquals(0.25F, SandExcavationChanceRules.NEAP_TIDE_SEAFOOD_CHANCE);
        assertEquals(0.50F, SandExcavationChanceRules.NORMAL_TIDE_SEAFOOD_CHANCE);
        assertEquals(0.75F, SandExcavationChanceRules.SPRING_TIDE_SEAFOOD_CHANCE);
        assertEquals(0.15F, SandExcavationChanceRules.TRASH_CHANCE);
    }

    @Test
    void keepsTonightsMoonPhaseThroughTheFollowingDaylight() {
        assertEquals(MoonPhase.WAXING_GIBBOUS, SandExcavationChanceRules.getEffectiveMoonPhase(12_999L));
        assertEquals(MoonPhase.FULL_MOON, SandExcavationChanceRules.getEffectiveMoonPhase(13_000L));
        assertEquals(MoonPhase.FULL_MOON, SandExcavationChanceRules.getEffectiveMoonPhase(36_999L));
        assertEquals(MoonPhase.WANING_GIBBOUS, SandExcavationChanceRules.getEffectiveMoonPhase(37_000L));
    }

    @Test
    void springAndNeapTidesUseSymmetricMoonPhaseChances() {
        assertEquals(SandExcavationChanceRules.SPRING_TIDE_SEAFOOD_CHANCE,
                SandExcavationChanceRules.getSeafoodChance(MoonPhase.FULL_MOON));
        assertEquals(SandExcavationChanceRules.SPRING_TIDE_SEAFOOD_CHANCE,
                SandExcavationChanceRules.getSeafoodChance(MoonPhase.NEW_MOON));
        assertEquals(SandExcavationChanceRules.NEAP_TIDE_SEAFOOD_CHANCE,
                SandExcavationChanceRules.getSeafoodChance(MoonPhase.FIRST_QUARTER));
        assertEquals(SandExcavationChanceRules.NEAP_TIDE_SEAFOOD_CHANCE,
                SandExcavationChanceRules.getSeafoodChance(MoonPhase.THIRD_QUARTER));
        assertEquals(SandExcavationChanceRules.NORMAL_TIDE_SEAFOOD_CHANCE,
                SandExcavationChanceRules.getSeafoodChance(MoonPhase.WAXING_CRESCENT));
        assertEquals(SandExcavationChanceRules.NORMAL_TIDE_SEAFOOD_CHANCE,
                SandExcavationChanceRules.getSeafoodChance(MoonPhase.WAXING_GIBBOUS));
        assertEquals(SandExcavationChanceRules.NORMAL_TIDE_SEAFOOD_CHANCE,
                SandExcavationChanceRules.getSeafoodChance(MoonPhase.WANING_CRESCENT));
        assertEquals(SandExcavationChanceRules.NORMAL_TIDE_SEAFOOD_CHANCE,
                SandExcavationChanceRules.getSeafoodChance(MoonPhase.WANING_GIBBOUS));
    }

    @Test
    void choosesExactlyOneCategoryAtProbabilityBoundaries() {
        assertEquals(SandExcavationResult.Category.SEAFOOD,
                SandExcavationChanceRules.selectCategory(0.2499F, 0.25F));
        assertEquals(SandExcavationResult.Category.TRASH,
                SandExcavationChanceRules.selectCategory(0.25F, 0.25F));
        assertEquals(SandExcavationResult.Category.TRASH,
                SandExcavationChanceRules.selectCategory(0.3999F, 0.25F));
        assertEquals(SandExcavationResult.Category.EMPTY,
                SandExcavationChanceRules.selectCategory(0.40F, 0.25F));

        assertEquals(SandExcavationResult.Category.SEAFOOD,
                SandExcavationChanceRules.selectCategory(0.7499F, 0.75F));
        assertEquals(SandExcavationResult.Category.TRASH,
                SandExcavationChanceRules.selectCategory(0.75F, 0.75F));
        assertEquals(SandExcavationResult.Category.EMPTY,
                SandExcavationChanceRules.selectCategory(0.90F, 0.75F));
    }
}
