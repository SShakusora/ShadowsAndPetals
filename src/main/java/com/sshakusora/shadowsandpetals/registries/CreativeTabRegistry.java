package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CreativeTabRegistry {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = register(CreativeTabType.MAIN);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NATURE = register(CreativeTabType.NATURE);

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> register(CreativeTabType type) {
        DeferredHolder<CreativeModeTab, CreativeModeTab> holder = SAPRegistries
                .creativeTab(type.getName())
                .lang(type.getLangName())
                .lang("zh_cn", type.getZhCnLangName())
                .icon(type.getIcon())
                .addItems(CreativeTabContentsRegistry.generator(type))
                .register();
        type.bind(holder);
        return holder;
    }

    public static void init() {}
}
