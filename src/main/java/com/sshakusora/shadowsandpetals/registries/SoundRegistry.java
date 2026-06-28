package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SoundRegistry {
    public static final DeferredHolder<SoundEvent, SoundEvent> SHISHI_ODOSHI = SAPRegistries
            .sound("shishi_odoshi")
            .subtitle("Shishi-odoshi clacks", "添水：敲击")
            .register();

    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_CHIME = SAPRegistries
            .sound("wind_chime")
            .subtitle("Wind chime rings", "风铃：铃响")
            .register();

    public static void init() {}
}
