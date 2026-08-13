package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.registries.builder.RegCreativeTabBuilder;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CreativeTabRegistry {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = register(CreativeTabType.MAIN);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NATURE = register(CreativeTabType.NATURE);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AGRICULTURE = register(CreativeTabType.AGRICULTURE);

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> register(CreativeTabType type) {
        RegCreativeTabBuilder builder = SAPRegistries
                .creativeTab(type.getName())
                .lang(type.getLangName())
                .lang(DatagenLangRegistry.ZH_CN, type.getZhCnLangName())
                .icon(type.getIcon())
                .addItems(CreativeTabContentsRegistry.generator(type));

        CreativeTabType[] types = CreativeTabType.values();
        if (type.ordinal() > 0) {
            CreativeTabType previous = types[type.ordinal() - 1];
            builder.withTabsBefore(ShadowsAndPetals.asResource(previous.getName()));
        }

        DeferredHolder<CreativeModeTab, CreativeModeTab> holder = builder.register();
        type.bind(holder);
        return holder;
    }

    public static void init() {}
}
