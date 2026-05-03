package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CreativeTabRegistry {
    public static DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = SAPRegistries
            .creativeTab("main")
            .lang("Shadows & Petals")
            .addItems(output -> {
                BlockRegistry.CAFE_CHAIRS.forEach(chair -> output.accept(chair.get()));
            })
            .register();

    public static void init() {}
}
