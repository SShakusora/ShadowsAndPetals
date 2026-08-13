package com.sshakusora.shadowsandpetals.registries.builder;

import net.minecraft.advancements.CriterionTrigger;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Objects;
import java.util.function.Supplier;

/** Fluent registration builder for custom advancement criterion triggers. */
public final class RegCriterionTriggerBuilder<T extends CriterionTrigger<?>> {
    private final DeferredRegister<CriterionTrigger<?>> registry;
    private final String name;
    private final Supplier<T> factory;

    public RegCriterionTriggerBuilder(
            DeferredRegister<CriterionTrigger<?>> registry,
            String name,
            Supplier<T> factory
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.name = Objects.requireNonNull(name);
        this.factory = Objects.requireNonNull(factory);
    }

    public DeferredHolder<CriterionTrigger<?>, T> register() {
        return registry.register(name, factory);
    }
}
