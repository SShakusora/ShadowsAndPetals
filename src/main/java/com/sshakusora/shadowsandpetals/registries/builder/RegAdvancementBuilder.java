package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.advancement.RegisteredAdvancement;
import com.sshakusora.shadowsandpetals.data.DatagenAdvancementRegistry;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Fluent builder for gameplay advancement declarations and datagen wiring. */
public final class RegAdvancementBuilder {
    private final Identifier id;
    private final String titleKey;
    private final String descriptionKey;
    private final Map<String, LocalizedDisplayText> localizedText = new LinkedHashMap<>();
    private final Map<String, RegisteredAdvancement.CriterionFactory> criteria = new LinkedHashMap<>();

    private @Nullable RegisteredAdvancement parent;
    private @Nullable Supplier<? extends ItemLike> icon;
    private Component title;
    private Component description;
    private @Nullable Identifier background;
    private AdvancementType frame = AdvancementType.TASK;
    private boolean showToast = true;
    private boolean announceToChat = true;
    private boolean hidden;
    private AdvancementRewards rewards = AdvancementRewards.EMPTY;
    private AdvancementRequirements.Strategy requirementsStrategy = AdvancementRequirements.Strategy.AND;

    public RegAdvancementBuilder(String name) {
        this.id = ShadowsAndPetals.asResource(Objects.requireNonNull(name));
        String languagePath = name.replace('/', '.');
        this.titleKey = "advancements." + ShadowsAndPetals.MOD_ID + "." + languagePath + ".title";
        this.descriptionKey = "advancements." + ShadowsAndPetals.MOD_ID + "." + languagePath + ".description";
        this.title = Component.translatable(titleKey);
        this.description = Component.translatable(descriptionKey);
    }

    public RegAdvancementBuilder parent(RegisteredAdvancement parent) {
        this.parent = Objects.requireNonNull(parent);
        return this;
    }

    public RegAdvancementBuilder icon(ItemLike icon) {
        Objects.requireNonNull(icon);
        this.icon = () -> icon;
        return this;
    }

    public RegAdvancementBuilder title(Component title) {
        this.title = Objects.requireNonNull(title);
        return this;
    }

    public RegAdvancementBuilder description(Component description) {
        this.description = Objects.requireNonNull(description);
        return this;
    }

    public RegAdvancementBuilder background(Identifier background) {
        this.background = Objects.requireNonNull(background);
        return this;
    }

    public RegAdvancementBuilder frame(AdvancementType frame) {
        this.frame = Objects.requireNonNull(frame);
        return this;
    }

    public RegAdvancementBuilder showToast(boolean showToast) {
        this.showToast = showToast;
        return this;
    }

    public RegAdvancementBuilder announceToChat(boolean announceToChat) {
        this.announceToChat = announceToChat;
        return this;
    }

    public RegAdvancementBuilder hidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public RegAdvancementBuilder rewards(AdvancementRewards rewards) {
        this.rewards = Objects.requireNonNull(rewards);
        return this;
    }

    public RegAdvancementBuilder rewards(AdvancementRewards.Builder rewards) {
        return rewards(Objects.requireNonNull(rewards).build());
    }

    public RegAdvancementBuilder requirements(AdvancementRequirements.Strategy strategy) {
        this.requirementsStrategy = Objects.requireNonNull(strategy);
        return this;
    }

    public RegAdvancementBuilder criterion(
            String name,
            RegisteredAdvancement.CriterionFactory criterion
    ) {
        if (criteria.putIfAbsent(Objects.requireNonNull(name), Objects.requireNonNull(criterion)) != null) {
            throw new IllegalStateException("Duplicate criterion '" + name + "' for advancement " + id);
        }
        return this;
    }

    public RegAdvancementBuilder criterion(String name, Supplier<Criterion<?>> criterion) {
        Objects.requireNonNull(criterion);
        return criterion(name, ignored -> criterion.get());
    }

    public RegAdvancementBuilder lang(String locale, String title, String description) {
        localizedText.put(
                Objects.requireNonNull(locale),
                new LocalizedDisplayText(Objects.requireNonNull(title), Objects.requireNonNull(description))
        );
        return this;
    }

    public RegisteredAdvancement register() {
        if (criteria.isEmpty()) {
            throw new IllegalStateException("Advancement " + id + " must declare at least one criterion");
        }
        for (Map.Entry<String, LocalizedDisplayText> entry : localizedText.entrySet()) {
            DatagenLangRegistry.add(entry.getKey(), titleKey, entry.getValue().title());
            DatagenLangRegistry.add(entry.getKey(), descriptionKey, entry.getValue().description());
        }

        return DatagenAdvancementRegistry.add(new RegisteredAdvancement(
                id,
                parent,
                icon,
                title,
                description,
                background,
                frame,
                showToast,
                announceToChat,
                hidden,
                rewards,
                requirementsStrategy,
                criteria
        ));
    }

    private record LocalizedDisplayText(String title, String description) {
    }
}
