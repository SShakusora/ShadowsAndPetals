package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.legacy.BlockEntityAliasRegistry;
import com.sshakusora.shadowsandpetals.registries.builder.*;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class SAPRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ShadowsAndPetals.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ShadowsAndPetals.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ShadowsAndPetals.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ShadowsAndPetals.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ShadowsAndPetals.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, ShadowsAndPetals.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, ShadowsAndPetals.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ShadowsAndPetals.MOD_ID);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ENTITIES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        PARTICLES.register(modEventBus);
        FEATURES.register(modEventBus);
        SOUNDS.register(modEventBus);
    }

    // Block helpers

    public static RegBlockBuilder<Block> block(String name) {
        return new RegBlockBuilder<>(BLOCKS, name);
    }

    public static <B extends Block> RegBlockBuilder<B> block(String name, Function<BlockBehaviour.Properties, B> factory) {
        return new RegBlockBuilder<B>(BLOCKS, name).block(factory);
    }

    public static <T extends BlockEntity> RegBlockEntityBuilder<T> blockEntity(String name) {
        return new RegBlockEntityBuilder<>(BLOCK_ENTITIES, name);
    }

    public static BlockEntityAliasRegistry.Builder blockEntityAlias(String name) {
        return BlockEntityAliasRegistry.builder(BLOCK_ENTITIES, name);
    }

    // Item helpers

    public static RegItemBuilder<Item> item(String name) {
        return new RegItemBuilder<>(ITEMS, name);
    }

    public static <I extends Item> RegItemBuilder<I> item(String name, Function<Item.Properties, I> factory) {
        return new RegItemBuilder<I>(ITEMS, name).item(factory);
    }

    public static RegItemBuilder.BlockItemBuilder blockItem(String name) {
        return new RegItemBuilder.BlockItemBuilder(ITEMS, name);
    }

    // CreativeTab helpers

    public static RegCreativeTabBuilder creativeTab(String name) {
        return new RegCreativeTabBuilder(CREATIVE_TABS, name);
    }

    // Particle helpers

    public static RegParticleBuilder<SimpleParticleType> particle(String name) {
        return new RegParticleBuilder<>(PARTICLES, name).simple();
    }

    public static RegParticleBuilder<SimpleParticleType> particle(String name, boolean overrideLimiter) {
        return new RegParticleBuilder<>(PARTICLES, name).simple(overrideLimiter);
    }

    public static <P extends ParticleType<?>> RegParticleBuilder<P> particleType(String name, Supplier<P> factory) {
        return new RegParticleBuilder<P>(PARTICLES, name).particle(factory);
    }

    // Sound helpers

    public static RegSoundBuilder sound(String name) {
        return new RegSoundBuilder(SOUNDS, name);
    }

    // Entity helpers

    public static <E extends Entity> RegEntityBuilder<E> entity(String name, MobCategory category) {
        return new RegEntityBuilder<>(ENTITIES, name, category);
    }

    public static <E extends Mob> RegEntityBuilder.MobBuilder<E> mob(String name, MobCategory category) {
        return new RegEntityBuilder.MobBuilder<>(ENTITIES, name, category);
    }
}
