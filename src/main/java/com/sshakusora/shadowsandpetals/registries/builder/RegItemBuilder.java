package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeRegistry;
import com.sshakusora.shadowsandpetals.data.ModRecipeProvider;
import com.sshakusora.shadowsandpetals.registries.CreativeTabContentsRegistry;
import com.sshakusora.shadowsandpetals.registries.CreativeTabType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fluent builder for item registration.
 *
 * @param <I> registered item type
 */
public class RegItemBuilder<I extends Item> {
    private final DeferredRegister.Items registry;
    private final String name;
    private Item.Properties properties = new Item.Properties();
    private Function<Item.Properties, I> itemFactory;
    private String langName;
    private BiConsumer<ModRecipeProvider, DeferredItem<I>> recipeGenerator;
    private final List<CreativeTabType> creativeTabs = new ArrayList<>();
    private final List<ResourceLocation> aliases = new ArrayList<>();

    public RegItemBuilder(DeferredRegister.Items registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    /**
     * Sets the exact {@link Item.Properties} instance used for registration.
     */
    public RegItemBuilder<I> properties(Item.Properties properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Builds a fresh {@link Item.Properties} through a configurator.
     */
    public RegItemBuilder<I> properties(Function<Item.Properties, Item.Properties> configurator) {
        this.properties = configurator.apply(new Item.Properties());
        return this;
    }

    /**
     * Sets the custom item factory used during registration.
     */
    public RegItemBuilder<I> item(Function<Item.Properties, I> factory) {
        this.itemFactory = factory;
        return this;
    }

    /**
     * Adds a generated language entry for the item.
     */
    public RegItemBuilder<I> lang(String name) {
        this.langName = name;
        return this;
    }

    /**
     * Attaches a recipe datagen callback.
     */
    public RegItemBuilder<I> recipe(BiConsumer<ModRecipeProvider, DeferredItem<I>> generator) {
        this.recipeGenerator = generator;
        return this;
    }

    /**
     * Adds the registered item to a creative tab.
     */
    public RegItemBuilder<I> creativeTab(CreativeTabType tab) {
        this.creativeTabs.add(tab);
        return this;
    }

    /**
     * Adds the registered item to multiple creative tabs.
     */
    public RegItemBuilder<I> creativeTabs(CreativeTabType... tabs) {
        this.creativeTabs.addAll(Arrays.asList(tabs));
        return this;
    }

    /**
     * Adds a same-namespace registry alias for this item.
     */
    public RegItemBuilder<I> alias(String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath));
        return this;
    }

    /**
     * Adds a cross-namespace registry alias for this item.
     */
    public RegItemBuilder<I> alias(String oldNamespace, String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    /**
     * Finalizes item registration and applies aliases, lang, recipe hooks, and creative-tab wiring.
     */
    @SuppressWarnings("unchecked")
    public DeferredItem<I> register() {
        DeferredItem<I> deferredItem;
        if (itemFactory == null) {
            deferredItem = (DeferredItem<I>) registry.registerSimpleItem(name, properties);
        } else {
            deferredItem = registry.registerItem(name, itemFactory, properties);
        }

        for (ResourceLocation alias : aliases) {
            registry.addAlias(alias, deferredItem.getId());
        }
        if (langName != null) {
            DatagenLangRegistry.add("item." + ShadowsAndPetals.MOD_ID + "." + deferredItem.getId().getPath(), langName);
        }
        if (recipeGenerator != null) {
            DatagenRecipeRegistry.add(deferredItem.getId(), provider -> recipeGenerator.accept(provider, deferredItem));
        }
        for (CreativeTabType tab : creativeTabs) {
            CreativeTabContentsRegistry.add(tab, deferredItem);
        }
        return deferredItem;
    }

    /**
     * Registers a plain {@link Item} using the current properties without extra hooks.
     */
    public DeferredItem<Item> simple() {
        return registry.registerSimpleItem(name, properties);
    }

    /**
     * Fluent builder for registering a {@link net.minecraft.world.item.BlockItem} separately from the block.
     */
    public static class BlockItemBuilder {
        private final DeferredRegister.Items registry;
        private final String name;
        private Supplier<? extends Block> blockSupplier;
        private DeferredBlock<? extends Block> deferredBlock;
        private Item.Properties properties = new Item.Properties();
        private String langName;
        private final List<CreativeTabType> creativeTabs = new ArrayList<>();
        private final List<ResourceLocation> aliases = new ArrayList<>();

        public BlockItemBuilder(DeferredRegister.Items registry, String name) {
            this.registry = registry;
            this.name = name;
        }

        /**
         * Uses a direct block supplier as the item source.
         */
        public BlockItemBuilder fromBlock(Supplier<? extends Block> block) {
            this.blockSupplier = block;
            return this;
        }

        /**
         * Uses a {@link DeferredBlock} as the item source.
         */
        public BlockItemBuilder fromDeferredBlock(DeferredBlock<? extends Block> block) {
            this.deferredBlock = block;
            return this;
        }

        public BlockItemBuilder properties(Item.Properties properties) {
            this.properties = properties;
            return this;
        }

        public BlockItemBuilder properties(Function<Item.Properties, Item.Properties> configurator) {
            this.properties = configurator.apply(new Item.Properties());
            return this;
        }

        public BlockItemBuilder lang(String name) {
            this.langName = name;
            return this;
        }

        public BlockItemBuilder creativeTab(CreativeTabType tab) {
            this.creativeTabs.add(tab);
            return this;
        }

        public BlockItemBuilder creativeTabs(CreativeTabType... tabs) {
            this.creativeTabs.addAll(Arrays.asList(tabs));
            return this;
        }

        public BlockItemBuilder alias(String oldPath) {
            this.aliases.add(ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath));
            return this;
        }

        public BlockItemBuilder alias(String oldNamespace, String oldPath) {
            this.aliases.add(ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath));
            return this;
        }

        /**
         * Finalizes block item registration and applies aliases, lang, and creative-tab wiring.
         */
        public DeferredItem<BlockItem> register() {
            DeferredItem<BlockItem> deferredItem;
            if (deferredBlock != null) {
                final var block = deferredBlock;
                final var props = properties;
                deferredItem = registry.register(name, key -> new BlockItem(block.get(), props));
            } else if (blockSupplier != null) {
                final var supplier = blockSupplier;
                final var props = properties;
                deferredItem = registry.register(name, key -> new BlockItem(supplier.get(), props));
            } else {
                throw new IllegalStateException("BlockItemBuilder requires a block source via .fromBlock() or .fromDeferredBlock()");
            }

            for (ResourceLocation alias : aliases) {
                registry.addAlias(alias, deferredItem.getId());
            }
            if (langName != null) {
                DatagenLangRegistry.add("item." + ShadowsAndPetals.MOD_ID + "." + deferredItem.getId().getPath(), langName);
            }
            for (CreativeTabType tab : creativeTabs) {
                CreativeTabContentsRegistry.add(tab, deferredItem);
            }
            return deferredItem;
        }
    }
}
