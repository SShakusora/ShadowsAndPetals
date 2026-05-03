package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeRegistry;
import com.sshakusora.shadowsandpetals.data.ModRecipeProvider;
import com.sshakusora.shadowsandpetals.registries.CreativeTabContentsRegistry;
import com.sshakusora.shadowsandpetals.registries.CreativeTabType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    public RegItemBuilder<I> properties(Item.Properties properties) {
        this.properties = properties;
        return this;
    }

    public RegItemBuilder<I> properties(Function<Item.Properties, Item.Properties> configurator) {
        this.properties = configurator.apply(new Item.Properties());
        return this;
    }

    public RegItemBuilder<I> item(Function<Item.Properties, I> factory) {
        this.itemFactory = factory;
        return this;
    }

    public RegItemBuilder<I> lang(String name) {
        this.langName = name;
        return this;
    }

    public RegItemBuilder<I> recipe(BiConsumer<ModRecipeProvider, DeferredItem<I>> generator) {
        this.recipeGenerator = generator;
        return this;
    }

    public RegItemBuilder<I> creativeTab(CreativeTabType tab) {
        this.creativeTabs.add(tab);
        return this;
    }

    public RegItemBuilder<I> creativeTabs(CreativeTabType... tabs) {
        for (CreativeTabType tab : tabs) {
            this.creativeTabs.add(tab);
        }
        return this;
    }

    public RegItemBuilder<I> alias(String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath));
        return this;
    }

    public RegItemBuilder<I> alias(String oldNamespace, String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

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
            CreativeTabContentsRegistry.add(tab, deferredItem::get);
        }
        return deferredItem;
    }

    public DeferredItem<Item> simple() {
        return registry.registerSimpleItem(name, properties);
    }

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

        public BlockItemBuilder fromBlock(Supplier<? extends Block> block) {
            this.blockSupplier = block;
            return this;
        }

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
            for (CreativeTabType tab : tabs) {
                this.creativeTabs.add(tab);
            }
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

        public DeferredItem<net.minecraft.world.item.BlockItem> register() {
            DeferredItem<net.minecraft.world.item.BlockItem> deferredItem;
            if (deferredBlock != null) {
                final var block = deferredBlock;
                final var props = properties;
                deferredItem = registry.register(name, key -> new net.minecraft.world.item.BlockItem(block.get(), props));
            } else if (blockSupplier != null) {
                final var supplier = blockSupplier;
                final var props = properties;
                deferredItem = registry.register(name, key -> new net.minecraft.world.item.BlockItem(supplier.get(), props));
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
                CreativeTabContentsRegistry.add(tab, deferredItem::get);
            }
            return deferredItem;
        }
    }
}
