package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.advancement.trigger.*;
import net.minecraft.advancements.CriterionTrigger;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Custom advancement criterion trigger registrations. */
public final class TriggerRegistry {
    public static final DeferredHolder<CriterionTrigger<?>, Irori2x2FormedTrigger>
            IRORI_2X2_FORMED = SAPRegistries
            .trigger("irori_2x2_formed", Irori2x2FormedTrigger::new)
            .register();

    public static final DeferredHolder<CriterionTrigger<?>, RawConcrete3x3FormedTrigger>
            RAW_CONCRETE_3X3_FORMED = SAPRegistries
            .trigger("raw_concrete_3x3_formed", RawConcrete3x3FormedTrigger::new)
            .register();

    public static final DeferredHolder<CriterionTrigger<?>, SeafoodExcavatedTrigger>
            SEAFOOD_EXCAVATED = SAPRegistries
            .trigger("seafood_excavated", SeafoodExcavatedTrigger::new)
            .register();

    public static final DeferredHolder<CriterionTrigger<?>, GravelExcavatedTrigger>
            GRAVEL_EXCAVATED = SAPRegistries
            .trigger("gravel_excavated", GravelExcavatedTrigger::new)
            .register();

    public static final DeferredHolder<CriterionTrigger<?>, VanityDrawerOpenedTrigger>
            VANITY_DRAWER_OPENED = SAPRegistries
            .trigger("vanity_drawer_opened", VanityDrawerOpenedTrigger::new)
            .register();

    public static final DeferredHolder<CriterionTrigger<?>, LampLitTrigger>
            LAMP_LIT = SAPRegistries
            .trigger("lamp_lit", LampLitTrigger::new)
            .register();

    public static final DeferredHolder<CriterionTrigger<?>, RockeryCarvedTrigger>
            ROCKERY_CARVED = SAPRegistries
            .trigger("rockery_carved", RockeryCarvedTrigger::new)
            .register();

    public static final DeferredHolder<CriterionTrigger<?>, ShishiOdoshiFluidPouredTrigger>
            SHISHI_ODOSHI_FLUID_POURED = SAPRegistries
            .trigger("shishi_odoshi_fluid_poured", ShishiOdoshiFluidPouredTrigger::new)
            .register();

    private TriggerRegistry() {
    }

    public static void init() {
    }
}
