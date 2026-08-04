package com.sshakusora.shadowsandpetals.world.excavation;

import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.compat.CompatManager;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public final class SandExcavationSeasonCompat {
    private static final String SERENE_SEASONS_MODIFIER_CLASS = "com.sshakusora.shadowsandpetals.compat.sereneseasons.SereneSeasonsSeasonModifier";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SandExcavationSeasonModifier NO_OP = (level, currentChance) -> currentChance;
    private static volatile SandExcavationSeasonModifier loadedModifier;

    private SandExcavationSeasonCompat() {
    }

    public static float modifySeafoodChance(ServerLevel level, float currentChance) {
        if (!CompatManager.isSereneSeasonsLoaded()) {
            return currentChance;
        }

        SandExcavationSeasonModifier modifier = loadedModifier;
        if (modifier == null) {
            modifier = createModifier();
            loadedModifier = modifier;
        }
        return modifier.modifySeafoodChance(level, currentChance);
    }

    private static SandExcavationSeasonModifier createModifier() {
        try {
            Class<?> modifierClass = Class.forName(
                    SERENE_SEASONS_MODIFIER_CLASS,
                    true,
                    SandExcavationSeasonCompat.class.getClassLoader()
            );
            return (SandExcavationSeasonModifier) modifierClass
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn(
                    "Unable to initialize the optional Serene Seasons Sand Excavation integration; "
                            + "seasonal adjustments are disabled.",
                    exception
            );
            return NO_OP;
        }
    }

}
