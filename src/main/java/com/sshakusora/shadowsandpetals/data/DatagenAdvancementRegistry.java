package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.advancement.RegisteredAdvancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;

/** Collects builder-declared advancements and emits them in parent-first order. */
public final class DatagenAdvancementRegistry {
    private static final Map<Identifier, RegisteredAdvancement> ADVANCEMENTS = new LinkedHashMap<>();

    private DatagenAdvancementRegistry() {
    }

    public static RegisteredAdvancement add(RegisteredAdvancement advancement) {
        RegisteredAdvancement previous = ADVANCEMENTS.putIfAbsent(advancement.id(), advancement);
        if (previous != null) {
            throw new IllegalStateException("Duplicate advancement datagen entry for " + advancement.id());
        }
        return advancement;
    }

    public static void generate(
            HolderLookup.Provider registries,
            Consumer<AdvancementHolder> output
    ) {
        Map<RegisteredAdvancement, AdvancementHolder> generated = new IdentityHashMap<>();
        Set<RegisteredAdvancement> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RegisteredAdvancement advancement : ADVANCEMENTS.values()) {
            generate(advancement, registries, output, generated, visiting);
        }
    }

    private static AdvancementHolder generate(
            RegisteredAdvancement advancement,
            HolderLookup.Provider registries,
            Consumer<AdvancementHolder> output,
            Map<RegisteredAdvancement, AdvancementHolder> generated,
            Set<RegisteredAdvancement> visiting
    ) {
        AdvancementHolder existing = generated.get(advancement);
        if (existing != null) {
            return existing;
        }
        if (!visiting.add(advancement)) {
            throw new IllegalStateException("Cyclic advancement parent relation at " + advancement.id());
        }

        AdvancementHolder parentHolder = null;
        RegisteredAdvancement parent = advancement.parent();
        if (parent != null) {
            if (ADVANCEMENTS.get(parent.id()) != parent) {
                throw new IllegalStateException(
                        "Advancement " + advancement.id() + " references an unregistered parent " + parent.id()
                );
            }
            parentHolder = generate(parent, registries, output, generated, visiting);
        }

        AdvancementHolder holder = advancement.build(registries, parentHolder);
        output.accept(holder);
        generated.put(advancement, holder);
        visiting.remove(advancement);
        return holder;
    }
}
