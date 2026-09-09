package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.item.barrel.WoodenBarrelBlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CreativeTabRegistry {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NATURE = SAPRegistries
            .creativeTab("nature")
            .lang("Shadows & Petals: Nature")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花：自然")
            .icon(() -> BlockRegistry.MAPLE_SET.sapling())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.NATURE))
            .register();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARCHITECTURE = SAPRegistries
            .creativeTab("architecture")
            .lang("Shadows & Petals: Architecture")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花：建材")
            .icon(() -> BlockRegistry.RAW_CONCRETE.get())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.ARCHITECTURE))
            .withTabsBefore(NATURE.getId())
            .alias("main")
            .register();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FURNISHINGS = SAPRegistries
            .creativeTab("furnishings")
            .lang("Shadows & Petals: Furnishings")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花：陈设")
            .icon(() -> BlockRegistry.IRORI.get())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.FURNISHINGS))
            .withTabsBefore(ARCHITECTURE.getId())
            .register();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TRADITIONS = SAPRegistries
            .creativeTab("traditions")
            .lang("Shadows & Petals: Traditions")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花：传统")
            .icon(() -> BlockRegistry.WIND_CHIME.get())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.TRADITIONS))
            .withTabsBefore(FURNISHINGS.getId())
            .register();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CUISINE = SAPRegistries
            .creativeTab("cuisine")
            .lang("Shadows & Petals: Cuisine")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花：料理")
            .icon(() -> BlockRegistry.COPPER_TEAPOT.get())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.CUISINE))
            .addItems(output -> {
                output.accept(WoodenBarrelBlockItem.filledWoodenBarrel(Fluids.WATER));
                output.accept(WoodenBarrelBlockItem.filledWoodenBarrel(NeoForgeMod.MILK.value()));
            })
            .withTabsBefore(TRADITIONS.getId())
            .alias("agriculture")
            .alias("cooking")
            .register();

    private CreativeTabRegistry() {
    }

    public static void init() {
    }
}
