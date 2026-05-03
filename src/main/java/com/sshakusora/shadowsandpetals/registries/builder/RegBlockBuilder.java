package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenBlockStateRegistry;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.data.ModBlockStateProvider;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RegBlockBuilder<B extends Block> {
    private final DeferredRegister.Blocks registry;
    private final String name;
    private BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
    private Function<BlockBehaviour.Properties, B> blockFactory;
    private boolean withItem;
    private Item.Properties itemProperties;
    private Function<Block, ? extends BlockItem> itemFactory;
    private String langName;
    private BiConsumer<ModBlockStateProvider, DeferredBlock<B>> blockStateGenerator;
    private final List<ResourceLocation> aliases = new ArrayList<>();

    public RegBlockBuilder(DeferredRegister.Blocks registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    public RegBlockBuilder<B> properties(BlockBehaviour.Properties properties) {
        this.properties = properties;
        return this;
    }

    public RegBlockBuilder<B> properties(Function<BlockBehaviour.Properties, BlockBehaviour.Properties> configurator) {
        this.properties = configurator.apply(BlockBehaviour.Properties.of());
        return this;
    }

    public RegBlockBuilder<B> block(Function<BlockBehaviour.Properties, B> factory) {
        this.blockFactory = factory;
        return this;
    }

    public RegBlockBuilder<B> withItem() {
        this.withItem = true;
        this.itemProperties = new Item.Properties();
        return this;
    }

    public RegBlockBuilder<B> withItem(Item.Properties properties) {
        this.withItem = true;
        this.itemProperties = properties;
        return this;
    }

    public RegBlockBuilder<B> withItem(Function<Item.Properties, Item.Properties> configurator) {
        this.withItem = true;
        this.itemProperties = configurator.apply(new Item.Properties());
        return this;
    }

    public RegBlockBuilder<B> withCustomItem(Function<Block, ? extends BlockItem> factory) {
        this.withItem = true;
        this.itemFactory = factory;
        return this;
    }

    public RegBlockBuilder<B> lang(String name) {
        this.langName = name;
        return this;
    }

    public RegBlockBuilder<B> blockstate(BiConsumer<ModBlockStateProvider, DeferredBlock<B>> generator) {
        this.blockStateGenerator = generator;
        return this;
    }

    public RegBlockBuilder<B> alias(String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath));
        return this;
    }

    public RegBlockBuilder<B> alias(String oldNamespace, String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    @SuppressWarnings("unchecked")
    public DeferredBlock<B> register() {
        DeferredBlock<B> deferredBlock;
        if (blockFactory == null) {
            DeferredBlock<Block> simpleBlock = registry.registerSimpleBlock(name, properties);
            deferredBlock = (DeferredBlock<B>) simpleBlock;
        } else {
            deferredBlock = registry.registerBlock(name, blockFactory, properties);
        }

        postRegister(deferredBlock);

        if (withItem) {
            registerBlockItem(deferredBlock);
        }
        return deferredBlock;
    }

    private void postRegister(DeferredBlock<? extends Block> block) {
        applyAliases(block.getId());
        applyLang(block.getId().getPath());
        applyBlockStateUnchecked(block);
    }

    private void applyAliases(ResourceLocation targetId) {
        for (ResourceLocation alias : aliases) {
            registry.addAlias(alias, targetId);
        }
    }

    private void applyLang(String path) {
        if (langName == null) {
            return;
        }

        DatagenLangRegistry.add("block." + ShadowsAndPetals.MOD_ID + "." + path, langName);
        if (withItem) {
            DatagenLangRegistry.add("item." + ShadowsAndPetals.MOD_ID + "." + path, langName);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyBlockStateUnchecked(DeferredBlock<? extends Block> block) {
        if (blockStateGenerator == null) {
            return;
        }

        DeferredBlock<B> typedBlock = (DeferredBlock<B>) block;
        DatagenBlockStateRegistry.add(block.getId(), provider -> blockStateGenerator.accept(provider, typedBlock));
    }

    private void registerBlockItem(DeferredBlock<? extends Block> block) {
        DeferredRegister.Items items = SAPRegistries.ITEMS;
        if (itemFactory != null) {
            items.register(name, key -> itemFactory.apply(block.get()));
        } else {
            final Item.Properties props = itemProperties;
            items.register(name, key -> new BlockItem(block.get(), props));
        }
    }

    public DeferredBlock<Block> simple() {
        DeferredBlock<Block> block = registry.registerSimpleBlock(name, properties);
        postRegister(block);
        return block;
    }

    public DeferredBlock<Block> simpleWithItem() {
        DeferredBlock<Block> block = registry.registerSimpleBlock(name, properties);
        this.withItem = true;
        postRegister(block);
        final Item.Properties props = itemProperties != null ? itemProperties : new Item.Properties();
        SAPRegistries.ITEMS.register(name, key -> new BlockItem(block.get(), props));
        return block;
    }
}
