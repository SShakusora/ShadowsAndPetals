package com.sshakusora.shadowsandpetals.world.excavation;

import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface SandExcavationSeasonModifier {
    float modifySeafoodChance(ServerLevel level, float currentChance);
}
