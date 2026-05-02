package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CreativeTabRegistry {
    public static DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = SAPRegistries
            .creativeTab("main")
            .icon(Items.APPLE)
            .addItems(output -> {
                BlockRegistry.CAFE_CHAIRS.forEach(chair -> output.accept(chair.get()));
                output.accept(Items.SWEET_BERRIES);
            })
            .register();

    public static void init() {}
}
