package com.sshakusora.shadowsandpetals.advancement;

import net.minecraft.advancements.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Immutable datagen specification for one gameplay advancement. */
public final class RegisteredAdvancement {
    @FunctionalInterface
    public interface CriterionFactory {
        Criterion<?> create(HolderLookup.Provider registries);
    }

    private final Identifier id;
    private final ResourceKey<Advancement> key;
    private final @Nullable RegisteredAdvancement parent;
    private final @Nullable Supplier<? extends ItemLike> icon;
    private final Component title;
    private final Component description;
    private final @Nullable Identifier background;
    private final AdvancementType frame;
    private final boolean showToast;
    private final boolean announceToChat;
    private final boolean hidden;
    private final AdvancementRewards rewards;
    private final AdvancementRequirements.Strategy requirementsStrategy;
    private final Map<String, CriterionFactory> criteria;

    public RegisteredAdvancement(
            Identifier id,
            @Nullable RegisteredAdvancement parent,
            @Nullable Supplier<? extends ItemLike> icon,
            Component title,
            Component description,
            @Nullable Identifier background,
            AdvancementType frame,
            boolean showToast,
            boolean announceToChat,
            boolean hidden,
            AdvancementRewards rewards,
            AdvancementRequirements.Strategy requirementsStrategy,
            Map<String, CriterionFactory> criteria
    ) {
        this.id = Objects.requireNonNull(id);
        this.key = ResourceKey.create(Registries.ADVANCEMENT, id);
        this.parent = parent;
        this.icon = icon;
        this.title = Objects.requireNonNull(title);
        this.description = Objects.requireNonNull(description);
        this.background = background;
        this.frame = Objects.requireNonNull(frame);
        this.showToast = showToast;
        this.announceToChat = announceToChat;
        this.hidden = hidden;
        this.rewards = Objects.requireNonNull(rewards);
        this.requirementsStrategy = Objects.requireNonNull(requirementsStrategy);
        this.criteria = Collections.unmodifiableMap(new LinkedHashMap<>(criteria));
    }

    public Identifier id() {
        return id;
    }

    public ResourceKey<Advancement> key() {
        return key;
    }

    public @Nullable RegisteredAdvancement parent() {
        return parent;
    }

    public AdvancementHolder build(
            HolderLookup.Provider registries,
            @Nullable AdvancementHolder parentHolder
    ) {
        Advancement.Builder builder = Advancement.Builder.advancement();
        if (parentHolder != null) {
            builder.parent(parentHolder);
        }
        if (icon != null) {
            builder.display(
                    icon.get(),
                    title,
                    description,
                    background,
                    frame,
                    showToast,
                    announceToChat,
                    hidden
            );
        }
        if (rewards != AdvancementRewards.EMPTY) {
            builder.rewards(rewards);
        }
        for (Map.Entry<String, CriterionFactory> entry : criteria.entrySet()) {
            builder.addCriterion(entry.getKey(), entry.getValue().create(registries));
        }
        builder.requirements(requirementsStrategy);
        return builder.build(id);
    }
}
