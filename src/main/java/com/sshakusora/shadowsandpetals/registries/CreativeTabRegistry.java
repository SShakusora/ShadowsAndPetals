package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.item.barrel.WoodenBarrelBlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COOKING = SAPRegistries
            .creativeTab("cooking")
            .lang("Shadows & Petals: Cooking")
            .lang(DatagenLangRegistry.ZH_CN, "织影落花：烹饪")
            .icon(() -> BlockRegistry.WOODEN_BARREL.get())
            .addItems(CreativeTabContentsRegistry.generator(CreativeTabKey.COOKING))
            .addItems(output -> {
                output.accept(WoodenBarrelBlockItem.filledWoodenBarrel(Fluids.WATER));
                output.accept(WoodenBarrelBlockItem.filledWoodenBarrel(NeoForgeMod.MILK.value()));
            })
            .withTabsBefore(AGRICULTURE.getId())
            .register();

    private CreativeTabRegistry() {
    }

    public static void init() {
    }
}
