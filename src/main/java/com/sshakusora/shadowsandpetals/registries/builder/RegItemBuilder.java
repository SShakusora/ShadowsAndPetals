package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeRegistry;
import com.sshakusora.shadowsandpetals.data.ModRecipeProvider;
import com.sshakusora.shadowsandpetals.data.model.ItemModelCallback;
import com.sshakusora.shadowsandpetals.data.model.ModelDatagenRegistry;
import com.sshakusora.shadowsandpetals.foundation.tooltip.ItemDescription;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipComponentRegistry;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipLangBuilder;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipModifier;
import com.sshakusora.shadowsandpetals.registries.CreativeTabContentsRegistry;
import com.sshakusora.shadowsandpetals.registries.CreativeTabType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.*;

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
    private final Map<String, String> langNames = new LinkedHashMap<>();
    private BiConsumer<ModRecipeProvider, DeferredItem<I>> recipeGenerator;
    private Supplier<? extends ItemModelCallback<I>> itemModelGenerator;
    private Function<DeferredItem<I>, Identifier> clientItemModelFactory;
    private Function<DeferredItem<I>, Identifier> customClientItemTypeFactory;
    private final List<CreativeTabType> creativeTabs = new ArrayList<>();
    private final List<Identifier> aliases = new ArrayList<>();
    private boolean hasTooltipDescription;
    private Consumer<TooltipLangBuilder> tooltipDescriptionGenerator;
    private TooltipModifier tooltipModifier;
    private BiFunction<I, ItemStack, @Nullable TooltipComponent> tooltipComponentFactory;
    private int tooltipComponentMinimumWidth;

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
        this.langNames.put(DatagenLangRegistry.DEFAULT_LOCALE, name);
        return this;
    }

    /**
     * Adds a generated language entry for the item for a specific locale.
     */
    public RegItemBuilder<I> lang(String locale, String name) {
        this.langNames.put(locale, name);
        return this;
    }

    /**
     * Opts this item into the {@link ItemDescription} tooltip system.
     * <p>
     * When set, the item will display a three-state tooltip driven by
     * localisation keys: a brief hint by default, a summary with behaviours
     * when Shift is held, and controls when Ctrl is held.
     * <p>
     * The actual tooltip text must be registered separately via
     * {@link TooltipLangBuilder}
     * during data generation.
     */
    public RegItemBuilder<I> tooltipDescription() {
        this.hasTooltipDescription = true;
        return this;
    }

    /**
     * Opts this item into the tooltip system and registers its localised text.
     * The translation-key prefix is derived automatically from the item id.
     */
    public RegItemBuilder<I> tooltipDescription(Consumer<TooltipLangBuilder> generator) {
        this.hasTooltipDescription = true;
        this.tooltipDescriptionGenerator = Objects.requireNonNull(generator);
        return this;
    }

    /**
     * Adds a dynamic modifier to this item's foundation tooltip pipeline.
     */
    public RegItemBuilder<I> tooltipModifier(TooltipModifier modifier) {
        this.tooltipModifier = Objects.requireNonNull(modifier);
        return this;
    }

    /**
     * Registers a custom component for this item's tooltip.
     */
    public RegItemBuilder<I> tooltipComponent(
            BiFunction<I, ItemStack, @Nullable TooltipComponent> factory
    ) {
        return tooltipComponent(factory, 0);
    }

    /**
     * Registers a custom component and minimum tooltip width for this item.
     */
    public RegItemBuilder<I> tooltipComponent(
            BiFunction<I, ItemStack, @Nullable TooltipComponent> factory,
            int minimumWidth
    ) {
        this.tooltipComponentFactory = Objects.requireNonNull(factory);
        this.tooltipComponentMinimumWidth = Math.max(0, minimumWidth);
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
     * Attaches an item-model datagen callback.
     */
    public RegItemBuilder<I> model(Supplier<? extends ItemModelCallback<I>> generator) {
        this.itemModelGenerator = Objects.requireNonNull(generator);
        return this;
    }

    /**
     * Attaches a client item-model mapping used by the unified model provider.
     */
    public RegItemBuilder<I> clientItem(Function<DeferredItem<I>, Identifier> modelFactory) {
        this.clientItemModelFactory = modelFactory;
        return this;
    }

    /**
     * Attaches a fixed client item-model mapping used by the unified model provider.
     */
    public RegItemBuilder<I> clientItem(Identifier modelId) {
        return clientItem(item -> modelId);
    }

    /**
     * Attaches a custom client item-model type used by the unified model provider.
     */
    public RegItemBuilder<I> customClientItem(Function<DeferredItem<I>, Identifier> modelTypeFactory) {
        this.customClientItemTypeFactory = modelTypeFactory;
        return this;
    }

    /**
     * Attaches a fixed custom client item-model type used by the unified model provider.
     */
    public RegItemBuilder<I> customClientItem(Identifier modelType) {
        return customClientItem(item -> modelType);
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
        this.aliases.add(ShadowsAndPetals.asResource(oldPath));
        return this;
    }

    /**
     * Adds a cross-namespace registry alias for this item.
     */
    public RegItemBuilder<I> alias(String oldNamespace, String oldPath) {
        this.aliases.add(Identifier.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    /**
     * Finalizes item registration and applies aliases, lang, recipe hooks, and creative-tab wiring.
     */
    @SuppressWarnings("unchecked")
    public DeferredItem<I> register() {
        DeferredItem<I> deferredItem;
        if (itemFactory == null) {
            deferredItem = (DeferredItem<I>) registry.registerSimpleItem(name, () -> properties);
        } else {
            deferredItem = registry.registerItem(name, itemFactory, () -> properties);
        }

        for (Identifier alias : aliases) {
            registry.addAlias(alias, deferredItem.getId());
        }
        DatagenLangRegistry.addFallback("item." + ShadowsAndPetals.MOD_ID + "." + deferredItem.getId().getPath(), deferredItem.getId().getPath());
        for (Map.Entry<String, String> entry : langNames.entrySet()) {
            DatagenLangRegistry.add(entry.getKey(), "item." + ShadowsAndPetals.MOD_ID + "." + deferredItem.getId().getPath(), entry.getValue());
        }
        if (recipeGenerator != null) {
            DatagenRecipeRegistry.add(deferredItem.getId(), provider -> recipeGenerator.accept(provider, deferredItem));
        }
        applyModelDatagen(deferredItem);
        for (CreativeTabType tab : creativeTabs) {
            CreativeTabContentsRegistry.add(tab, deferredItem);
        }

        if (tooltipModifier != null) {
            TooltipModifier.register(ShadowsAndPetals.asResource(name), tooltipModifier);
        }

        if (hasTooltipDescription) {
            Identifier itemId = ShadowsAndPetals.asResource(name);
            TooltipModifier.register(itemId, new ItemDescription.Modifier(() ->
                BuiltInRegistries.ITEM.getValue(itemId)));
            registerTooltipDescription();
        }

        if (tooltipComponentFactory != null) {
            TooltipComponentRegistry.register(
                    ShadowsAndPetals.asResource(name),
                    stack -> tooltipComponentFactory.apply(deferredItem.get(), stack),
                    tooltipComponentMinimumWidth);
        }

        return deferredItem;
    }

    private void registerTooltipDescription() {
        if (tooltipDescriptionGenerator == null) {
            return;
        }
        TooltipLangBuilder tooltip = TooltipLangBuilder.of(
                "item." + ShadowsAndPetals.MOD_ID + "." + name + ".tooltip");
        tooltipDescriptionGenerator.accept(tooltip);
        tooltip.register();
    }

    /**
     * Registers a plain {@link Item} using the current properties without extra hooks.
     */
    public DeferredItem<Item> simple() {
        DeferredItem<Item> deferredItem = registry.registerSimpleItem(name, () -> properties);
        ModelDatagenRegistry.addItem(deferredItem, null, null, null);
        return deferredItem;
    }

    private void applyModelDatagen(DeferredItem<I> deferredItem) {
        Identifier clientModel = clientItemModelFactory != null
                ? clientItemModelFactory.apply(deferredItem)
                : null;
        Identifier customType = customClientItemTypeFactory != null
                ? customClientItemTypeFactory.apply(deferredItem)
                : null;
        ModelDatagenRegistry.addItem(deferredItem, itemModelGenerator, clientModel, customType);
    }

    /**
     * Fluent builder for registering a {@link BlockItem} separately from the block.
     */
    public static class BlockItemBuilder {
        private final DeferredRegister.Items registry;
        private final String name;
        private Supplier<? extends Block> blockSupplier;
        private DeferredBlock<? extends Block> deferredBlock;
        private Item.Properties properties = new Item.Properties();
        private final Map<String, String> langNames = new LinkedHashMap<>();
        private Function<DeferredItem<BlockItem>, Identifier> clientItemModelFactory;
        private final List<CreativeTabType> creativeTabs = new ArrayList<>();
        private final List<Identifier> aliases = new ArrayList<>();

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
            this.langNames.put(DatagenLangRegistry.DEFAULT_LOCALE, name);
            return this;
        }

        public BlockItemBuilder lang(String locale, String name) {
            this.langNames.put(locale, name);
            return this;
        }

        /**
         * Attaches a client item-model mapping used by the unified model provider.
         */
        public BlockItemBuilder clientItem(Function<DeferredItem<BlockItem>, Identifier> modelFactory) {
            this.clientItemModelFactory = modelFactory;
            return this;
        }

        /**
         * Attaches a fixed client item-model mapping used by the unified model provider.
         */
        public BlockItemBuilder clientItem(Identifier modelId) {
            return clientItem(item -> modelId);
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
            this.aliases.add(ShadowsAndPetals.asResource(oldPath));
            return this;
        }

        public BlockItemBuilder alias(String oldNamespace, String oldPath) {
            this.aliases.add(Identifier.fromNamespaceAndPath(oldNamespace, oldPath));
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
                deferredItem = registry.register(name, key -> new BlockItem(block.get(), props.setId(ResourceKey.create(Registries.ITEM, key))));
            } else if (blockSupplier != null) {
                final var supplier = blockSupplier;
                final var props = properties;
                deferredItem = registry.register(name, key -> new BlockItem(supplier.get(), props.setId(ResourceKey.create(Registries.ITEM, key))));
            } else {
                throw new IllegalStateException("BlockItemBuilder requires a block source via .fromBlock() or .fromDeferredBlock()");
            }

            for (Identifier alias : aliases) {
                registry.addAlias(alias, deferredItem.getId());
            }
            DatagenLangRegistry.addFallback("item." + ShadowsAndPetals.MOD_ID + "." + deferredItem.getId().getPath(), deferredItem.getId().getPath());
            for (Map.Entry<String, String> entry : langNames.entrySet()) {
                DatagenLangRegistry.add(entry.getKey(), "item." + ShadowsAndPetals.MOD_ID + "." + deferredItem.getId().getPath(), entry.getValue());
            }
            Identifier modelId = clientItemModelFactory != null
                    ? clientItemModelFactory.apply(deferredItem)
                    : null;
            ModelDatagenRegistry.addItem(deferredItem, null, modelId, null);
            for (CreativeTabType tab : creativeTabs) {
                CreativeTabContentsRegistry.add(tab, deferredItem);
            }
            return deferredItem;
        }
    }
}
