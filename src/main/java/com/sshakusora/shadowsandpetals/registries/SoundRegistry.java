package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SoundRegistry {
    public static final DeferredHolder<SoundEvent, SoundEvent> SHISHI_ODOSHI = SAPRegistries
            .sound("shishi_odoshi")
            .subtitle("Shishi-odoshi clacks", "添水：敲击")
            .register();

    public static void init() {}
}
