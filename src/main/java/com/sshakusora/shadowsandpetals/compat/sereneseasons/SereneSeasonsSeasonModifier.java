package com.sshakusora.shadowsandpetals.compat.sereneseasons;

import com.sshakusora.shadowsandpetals.world.excavation.SandExcavationChanceRules;
import com.sshakusora.shadowsandpetals.world.excavation.SandExcavationSeasonModifier;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public final class SereneSeasonsSeasonModifier implements SandExcavationSeasonModifier {
    private static final float SPRING_MULTIPLIER = 1.00F;
    private static final float SUMMER_MULTIPLIER = 1.15F;
    private static final float AUTUMN_MULTIPLIER = 0.90F;
    private static final float WINTER_MULTIPLIER = 0.60F;

    @Override
    public float modifySeafoodChance(ServerLevel level, float currentChance) {
        Season season = SeasonHelper.getSeasonState(level).getSeason();
        return modifySeafoodChance(currentChance, season);
    }

    static float modifySeafoodChance(float currentChance, Season season) {
        float adjustedChance = currentChance * getMultiplier(season);
        return Math.clamp(
                adjustedChance,
                SandExcavationChanceRules.MINIMUM_SEAFOOD_CHANCE,
                1.0F - SandExcavationChanceRules.TRASH_CHANCE
        );
    }

    static float getMultiplier(Season season) {
        return switch (season) {
            case SPRING -> SPRING_MULTIPLIER;
            case SUMMER -> SUMMER_MULTIPLIER;
            case AUTUMN -> AUTUMN_MULTIPLIER;
            case WINTER -> WINTER_MULTIPLIER;
        };
    }
}
