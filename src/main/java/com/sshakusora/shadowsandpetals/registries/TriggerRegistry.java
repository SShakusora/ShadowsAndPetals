package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.advancement.trigger.ShishiOdoshiFluidPouredTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Custom advancement criterion trigger registrations. */
public final class TriggerRegistry {
    public static final DeferredHolder<CriterionTrigger<?>, ShishiOdoshiFluidPouredTrigger>
            SHISHI_ODOSHI_FLUID_POURED = SAPRegistries
            .trigger("shishi_odoshi_fluid_poured", ShishiOdoshiFluidPouredTrigger::new)
            .register();

    private TriggerRegistry() {
    }

    public static void init() {
    }
}
