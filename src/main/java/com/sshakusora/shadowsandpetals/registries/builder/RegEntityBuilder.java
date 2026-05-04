package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for {@link EntityType} registration.
 *
 * @param <E> registered entity type
 */
public class RegEntityBuilder<E extends Entity> {
    private final DeferredRegister<EntityType<?>> registry;
    private final String name;
    private final MobCategory category;
    private EntityType.EntityFactory<E> factory;
    private float width = 0.6f;
    private float height = 1.8f;
    private boolean fireImmune;
    private boolean canSpawnFarFromPlayer;
    private boolean clientTrackingSet;
    private int clientTrackingRange;
    private int updateInterval = 3;
    private boolean summonable = true;
    private boolean serialize = true;
    private float spawnDimensionsScale = -1.0f;
    private float eyeHeight = -1.0f;
    private String langName;
    private final List<Block> immuneToBlocks = new ArrayList<>();
    private final List<ResourceLocation> aliases = new ArrayList<>();

    public RegEntityBuilder(DeferredRegister<EntityType<?>> registry, String name, MobCategory category) {
        this.registry = registry;
        this.name = name;
        this.category = category;
    }

    /**
     * Sets the entity factory used by the resulting {@link EntityType}.
     */
    public RegEntityBuilder<E> factory(EntityType.EntityFactory<E> factory) {
        this.factory = factory;
        return this;
    }

    /**
     * Sets the entity dimensions in blocks.
     */
    public RegEntityBuilder<E> dimensions(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Sets the entity dimensions from an existing {@link EntityDimensions} instance.
     */
    public RegEntityBuilder<E> dimensions(EntityDimensions dimensions) {
        this.width = dimensions.width();
        this.height = dimensions.height();
        return this;
    }

    /**
     * Marks the entity type as fire immune.
     */
    public RegEntityBuilder<E> fireImmune() {
        this.fireImmune = true;
        return this;
    }

    /**
     * Allows the entity to spawn farther away from players.
     */
    public RegEntityBuilder<E> canSpawnFarFromPlayer() {
        this.canSpawnFarFromPlayer = true;
        return this;
    }

    /**
     * Overrides the client tracking range.
     */
    public RegEntityBuilder<E> clientTrackingRange(int range) {
        this.clientTrackingSet = true;
        this.clientTrackingRange = range;
        return this;
    }

    /**
     * Overrides the entity update interval.
     */
    public RegEntityBuilder<E> updateInterval(int interval) {
        this.updateInterval = interval;
        return this;
    }

    /**
     * Controls whether the entity type can be summoned by commands.
     */
    public RegEntityBuilder<E> summonable(boolean summonable) {
        this.summonable = summonable;
        return this;
    }

    /**
     * Controls whether the entity type is saved to disk.
     */
    public RegEntityBuilder<E> serialize(boolean serialize) {
        this.serialize = serialize;
        return this;
    }

    /**
     * Sets the spawn-dimensions scale used by the entity type.
     */
    public RegEntityBuilder<E> spawnDimensionsScale(float scale) {
        this.spawnDimensionsScale = scale;
        return this;
    }

    /**
     * Sets the entity eye height.
     */
    public RegEntityBuilder<E> eyeHeight(float height) {
        this.eyeHeight = height;
        return this;
    }

    /**
     * Adds a generated language entry for the entity type.
     */
    public RegEntityBuilder<E> lang(String name) {
        this.langName = name;
        return this;
    }

    /**
     * Declares blocks that the entity is immune to.
     */
    public RegEntityBuilder<E> immuneTo(Block... blocks) {
        for (Block block : blocks) {
            this.immuneToBlocks.add(block);
        }
        return this;
    }

    /**
     * Adds a same-namespace registry alias for this entity type.
     */
    public RegEntityBuilder<E> alias(String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath));
        return this;
    }

    /**
     * Adds a cross-namespace registry alias for this entity type.
     */
    public RegEntityBuilder<E> alias(String oldNamespace, String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    /**
     * Finalizes entity registration and applies aliases and lang generation.
     */
    public DeferredHolder<EntityType<?>, EntityType<E>> register() {
        DeferredHolder<EntityType<?>, EntityType<E>> deferredHolder = registry.register(name, key -> {
            EntityType.Builder<E> builder = EntityType.Builder.of(factory, category)
                    .sized(width, height)
                    .updateInterval(updateInterval);

            if (spawnDimensionsScale > 0) builder.spawnDimensionsScale(spawnDimensionsScale);
            if (eyeHeight > 0) builder.eyeHeight(eyeHeight);
            if (fireImmune) builder.fireImmune();
            if (canSpawnFarFromPlayer) builder.canSpawnFarFromPlayer();
            if (clientTrackingSet) builder.clientTrackingRange(clientTrackingRange);
            if (!summonable) builder.noSummon();
            if (!serialize) builder.noSave();
            if (!immuneToBlocks.isEmpty()) builder.immuneTo(immuneToBlocks.toArray(Block[]::new));

            return builder.build(name);
        });

        for (ResourceLocation alias : aliases) {
            registry.addAlias(alias, deferredHolder.getId());
        }
        if (langName != null) {
            DatagenLangRegistry.add("entity." + ShadowsAndPetals.MOD_ID + "." + deferredHolder.getId().getPath(), langName);
        }
        return deferredHolder;
    }

    /**
     * Specialized builder for mob entities with optional attribute wiring.
     *
     * @param <E> registered mob type
     */
    public static class MobBuilder<E extends Mob> extends RegEntityBuilder<E> {
        private AttributeSupplier.Builder attributes;

        public MobBuilder(DeferredRegister<EntityType<?>> registry, String name, MobCategory category) {
            super(registry, name, category);
        }

        @Override
        public MobBuilder<E> factory(EntityType.EntityFactory<E> factory) {
            super.factory(factory);
            return this;
        }

        @Override
        public MobBuilder<E> dimensions(float width, float height) {
            super.dimensions(width, height);
            return this;
        }

        @Override
        public MobBuilder<E> dimensions(EntityDimensions dimensions) {
            super.dimensions(dimensions);
            return this;
        }

        @Override
        public MobBuilder<E> fireImmune() {
            super.fireImmune();
            return this;
        }

        @Override
        public MobBuilder<E> canSpawnFarFromPlayer() {
            super.canSpawnFarFromPlayer();
            return this;
        }

        @Override
        public MobBuilder<E> clientTrackingRange(int range) {
            super.clientTrackingRange(range);
            return this;
        }

        @Override
        public MobBuilder<E> updateInterval(int interval) {
            super.updateInterval(interval);
            return this;
        }

        @Override
        public MobBuilder<E> summonable(boolean summonable) {
            super.summonable(summonable);
            return this;
        }

        @Override
        public MobBuilder<E> serialize(boolean serialize) {
            super.serialize(serialize);
            return this;
        }

        @Override
        public MobBuilder<E> spawnDimensionsScale(float scale) {
            super.spawnDimensionsScale(scale);
            return this;
        }

        @Override
        public MobBuilder<E> eyeHeight(float height) {
            super.eyeHeight(height);
            return this;
        }

        @Override
        public MobBuilder<E> lang(String name) {
            super.lang(name);
            return this;
        }

        @Override
        public MobBuilder<E> immuneTo(Block... blocks) {
            super.immuneTo(blocks);
            return this;
        }

        @Override
        public MobBuilder<E> alias(String oldPath) {
            super.alias(oldPath);
            return this;
        }

        @Override
        public MobBuilder<E> alias(String oldNamespace, String oldPath) {
            super.alias(oldNamespace, oldPath);
            return this;
        }

        /**
         * Stores the attribute builder that should be associated with the mob type.
         */
        public MobBuilder<E> attributes(AttributeSupplier.Builder attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Returns the attribute builder configured for this mob, if any.
         */
        public AttributeSupplier.Builder getAttributesBuilder() {
            return attributes;
        }
    }
}
