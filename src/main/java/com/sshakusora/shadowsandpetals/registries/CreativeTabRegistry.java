package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CreativeTabRegistry {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = SAPRegistries
            .creativeTab("main")
            .lang("Shadows & Petals")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花")
            .icon(() -> ItemRegistry.HAMMER.get())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.MAIN))
            .register();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NATURE = SAPRegistries
            .creativeTab("nature")
            .lang("Shadows & Petals: Nature")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花：自然")
            .icon(() -> BlockRegistry.MAPLE_SET.sapling())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.NATURE))
            .withTabsBefore(MAIN.getId())
            .register();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AGRICULTURE = SAPRegistries
            .creativeTab("agriculture")
            .lang("Shadows & Petals: Agriculture")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花：农业")
            .icon(() -> ItemRegistry.ORANGE_SEED.get())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.AGRICULTURE))
            .withTabsBefore(NATURE.getId())
            .register();

    private CreativeTabRegistry() {
    }

    public static void init() {
    }
}
