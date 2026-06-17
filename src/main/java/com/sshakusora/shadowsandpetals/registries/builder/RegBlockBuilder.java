package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.ct.CTRegistry;
import com.sshakusora.shadowsandpetals.client.ct.CTTextureType;
import com.sshakusora.shadowsandpetals.data.*;
import com.sshakusora.shadowsandpetals.foundation.tooltip.ItemDescription;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipModifier;
import com.sshakusora.shadowsandpetals.legacy.BlockStateAliasRegistry;
import com.sshakusora.shadowsandpetals.legacy.LegacyCompatIds;
import com.sshakusora.shadowsandpetals.legacy.LegacyStateBlock;
import com.sshakusora.shadowsandpetals.registries.BlockTagRegistry;
import com.sshakusora.shadowsandpetals.registries.CreativeTabContentsRegistry;
import com.sshakusora.shadowsandpetals.registries.CreativeTabType;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.function.*;

/**
 * Fluent builder for block registration.
 * <p>
 * This builder wraps a {@link DeferredRegister.Blocks} entry and optionally wires the matching
 * block item, language entry, datagen hooks, creative tab contents, registry aliases, and
 * legacy block-state migration rules.
 *
 * @param <B> registered block type
 */
public class RegBlockBuilder<B extends Block> {
    private final DeferredRegister.Blocks registry;
    private final String name;
    private Supplier<BlockBehaviour.Properties> propertiesFactory = BlockBehaviour.Properties::of;
    private Function<BlockBehaviour.Properties, B> blockFactory;
    private boolean withItem;
    private Item.Properties itemProperties;
    private Function<Block, ? extends BlockItem> itemFactory;
    private final Map<String, String> langNames = new LinkedHashMap<>();
    private BiConsumer<ModBlockStateProvider, DeferredBlock<B>> blockStateGenerator;
    private BiConsumer<ModBlockLootProvider, DeferredBlock<B>> blockLootGenerator;
    private BiConsumer<ModRecipeProvider, DeferredBlock<B>> recipeGenerator;
    private BiConsumer<ModItemModelProvider, DeferredBlock<B>> itemModelGenerator;
    private Function<DeferredBlock<B>, Identifier> clientItemModelFactory;
    private Function<DeferredBlock<B>, Identifier> ctBaseTextureFactory;
    private Function<DeferredBlock<B>, Identifier> ctConnectedTextureFactory;
    private CTTextureType ctTextureType;
    private int ctPadding;
    private final List<CreativeTabType> creativeTabs = new ArrayList<>();
    private final List<Identifier> aliases = new ArrayList<>();
    private final List<StateAliasSpec<?>> stateAliases = new ArrayList<>();
    private final List<TagKey<Block>> blockTags = new ArrayList<>();
    private boolean hasTooltipDescription;

    public RegBlockBuilder(DeferredRegister.Blocks registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    /**
    * Sets the exact {@link BlockBehaviour.Properties} instance used for registration.
     */
    public RegBlockBuilder<B> properties(BlockBehaviour.Properties properties) {
        this.propertiesFactory = () -> properties;
        return this;
    }

    /**
     * Builds a fresh {@link BlockBehaviour.Properties} via a configurator.
     */
    public RegBlockBuilder<B> properties(Function<BlockBehaviour.Properties, BlockBehaviour.Properties> configurator) {
        this.propertiesFactory = () -> configurator.apply(BlockBehaviour.Properties.of());
        return this;
    }

    /**
     * Overrides the block factory used during registration.
     */
    public RegBlockBuilder<B> block(Function<BlockBehaviour.Properties, B> factory) {
        this.blockFactory = factory;
        return this;
    }

    /**
     * Registers a default {@link BlockItem} for the block.
     */
    public RegBlockBuilder<B> withItem() {
        this.withItem = true;
        this.itemProperties = new Item.Properties();
        return this;
    }

    /**
     * Registers a default {@link BlockItem} using the supplied item properties.
     */
    public RegBlockBuilder<B> withItem(Item.Properties properties) {
        this.withItem = true;
        this.itemProperties = properties;
        return this;
    }

    /**
     * Registers a default {@link BlockItem} after configuring a fresh {@link Item.Properties}.
     */
    public RegBlockBuilder<B> withItem(Function<Item.Properties, Item.Properties> configurator) {
        this.withItem = true;
        this.itemProperties = configurator.apply(new Item.Properties());
        return this;
    }

    /**
     * Registers a custom {@link BlockItem} implementation for this block.
     */
    public RegBlockBuilder<B> withCustomItem(Function<Block, ? extends BlockItem> factory) {
        this.withItem = true;
        this.itemFactory = factory;
        return this;
    }

    /**
     * Adds a generated language entry for the block and, when present, its block item.
     */
    public RegBlockBuilder<B> lang(String name) {
        this.langNames.put(DatagenLangRegistry.DEFAULT_LOCALE, name);
        return this;
    }

    /**
     * Adds a generated language entry for the block and, when present, its block item for a specific locale.
     */
    public RegBlockBuilder<B> lang(String locale, String name) {
        this.langNames.put(locale, name);
        return this;
    }

    /**
     * Opts this block item into the {@link ItemDescription} tooltip system.
     * <p>
     * When set, the block's item will display a three-state tooltip driven by
     * localisation keys: a brief hint by default, a summary with behaviours
     * when Shift is held, and controls when Ctrl is held.
     * <p>
     * The actual tooltip text must be registered separately via
     * {@link com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipLangBuilder}
     * during data generation.
     */
    public RegBlockBuilder<B> tooltipDescription() {
        this.hasTooltipDescription = true;
        return this;
    }

    /**
     * Attaches a blockstate datagen callback.
     */
    public RegBlockBuilder<B> blockstate(BiConsumer<ModBlockStateProvider, DeferredBlock<B>> generator) {
        this.blockStateGenerator = generator;
        return this;
    }

    /**
     * Attaches a loot-table datagen callback.
     */
    public RegBlockBuilder<B> loot(BiConsumer<ModBlockLootProvider, DeferredBlock<B>> generator) {
        this.blockLootGenerator = generator;
        return this;
    }

    /**
     * Attaches a recipe datagen callback.
     */
    public RegBlockBuilder<B> recipe(BiConsumer<ModRecipeProvider, DeferredBlock<B>> generator) {
        this.recipeGenerator = generator;
        return this;
    }

    /**
     * Attaches an item-model datagen callback for the block item.
     */
    public RegBlockBuilder<B> itemModel(BiConsumer<ModItemModelProvider, DeferredBlock<B>> generator) {
        this.itemModelGenerator = generator;
        return this;
    }

    /**
     * Attaches a client item-model mapping used by {@link ModClientItemProvider}.
     */
    public RegBlockBuilder<B> clientItem(Function<DeferredBlock<B>, Identifier> modelFactory) {
        this.clientItemModelFactory = modelFactory;
        return this;
    }

    /**
     * Attaches a fixed client item-model mapping used by {@link ModClientItemProvider}.
     */
    public RegBlockBuilder<B> clientItem(Identifier modelId) {
        return clientItem(block -> modelId);
    }

    /**
     * Registers connected textures using the default texture names:
     * {@code block/<id>} and {@code block/<id>_connected_bleed}.
     */
    public RegBlockBuilder<B> connectedTexture(CTTextureType type) {
        return connectedTexture(
                block -> ShadowsAndPetals.asResource("block/" + block.getId().getPath()),
                block -> ShadowsAndPetals.asResource("block/" + block.getId().getPath() + "_connected_bleed"),
                type,
                1
        );
    }

    /**
     * Registers connected textures with fixed texture ids.
     */
    public RegBlockBuilder<B> connectedTexture(Identifier baseTexture, Identifier connectedTexture, CTTextureType type) {
        return connectedTexture(block -> baseTexture, block -> connectedTexture, type, 0);
    }

    /**
     * Registers connected textures with fixed texture ids and an inner tile padding.
     */
    public RegBlockBuilder<B> connectedTexture(Identifier baseTexture, Identifier connectedTexture, CTTextureType type, int padding) {
        return connectedTexture(block -> baseTexture, block -> connectedTexture, type, padding);
    }

    /**
     * Registers connected textures with texture ids derived from the registered block.
     */
    public RegBlockBuilder<B> connectedTexture(
            Function<DeferredBlock<B>, Identifier> baseTextureFactory,
            Function<DeferredBlock<B>, Identifier> connectedTextureFactory,
            CTTextureType type
    ) {
        return connectedTexture(baseTextureFactory, connectedTextureFactory, type, 0);
    }

    /**
     * Registers connected textures with texture ids derived from the registered block and an inner tile padding.
     */
    public RegBlockBuilder<B> connectedTexture(
            Function<DeferredBlock<B>, Identifier> baseTextureFactory,
            Function<DeferredBlock<B>, Identifier> connectedTextureFactory,
            CTTextureType type,
            int padding
    ) {
        this.ctBaseTextureFactory = Objects.requireNonNull(baseTextureFactory, "baseTextureFactory");
        this.ctConnectedTextureFactory = Objects.requireNonNull(connectedTextureFactory, "connectedTextureFactory");
        this.ctTextureType = Objects.requireNonNull(type, "type");
        this.ctPadding = Math.max(0, padding);
        return this;
    }

    /**
     * Adds the registered block item to a creative tab.
     */
    public RegBlockBuilder<B> creativeTab(CreativeTabType tab) {
        this.creativeTabs.add(tab);
        return this;
    }

    /**
     * Adds the registered block item to multiple creative tabs.
     */
    public RegBlockBuilder<B> creativeTabs(CreativeTabType... tabs) {
        Collections.addAll(this.creativeTabs, tabs);
        return this;
    }

    /**
     * Adds a block tag for datagen (e.g. {@link net.minecraft.tags.BlockTags#MINEABLE_WITH_PICKAXE}).
     */
    public RegBlockBuilder<B> tag(TagKey<Block> tag) {
        this.blockTags.add(tag);
        return this;
    }

    /**
     * Adds multiple block tags for datagen.
     */
    @SafeVarargs
    public final RegBlockBuilder<B> tags(TagKey<Block>... tags) {
        Collections.addAll(this.blockTags, tags);
        return this;
    }

    /**
     * Adds a same-namespace registry alias for save compatibility or renames.
     */
    public RegBlockBuilder<B> alias(String oldPath) {
        this.aliases.add(ShadowsAndPetals.asResource(oldPath));
        return this;
    }

    /**
     * Adds a cross-namespace registry alias for save compatibility or mod migrations.
     */
    public RegBlockBuilder<B> alias(String oldNamespace, String oldPath) {
        this.aliases.add(Identifier.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    /**
     * Registers a legacy block-state alias using a dedicated legacy block implementation.
     * <p>
     * Use this when old saves need to deserialize a removed block id that still carries legacy
     * properties. The legacy state is later converted to this builder's target state.
     */
    public <L extends Block> RegBlockBuilder<B> stateAlias(
            String oldPath,
            Function<BlockBehaviour.Properties, L> legacyFactory,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {
        return stateAlias(ShadowsAndPetals.MOD_ID, oldPath, legacyFactory, converter);
    }

    /**
     * Registers a cross-namespace legacy block-state alias using a dedicated legacy block implementation.
     */
    public <L extends Block> RegBlockBuilder<B> stateAlias(
            String oldNamespace,
            String oldPath,
            Function<BlockBehaviour.Properties, L> legacyFactory,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {
        this.stateAliases.add(new StateAliasSpec<>(
                Identifier.fromNamespaceAndPath(oldNamespace, oldPath),
                legacyFactory,
                converter
        ));
        return this;
    }

    /**
     * Registers a same-namespace legacy block-state alias whose properties are declared inline.
     * <p>
     * This is the preferred API when old saves only need a lightweight compatibility block with
     * dynamically declared properties.
     */
    public RegBlockBuilder<B> stateAliasProperties(
            String oldPath,
            Consumer<LegacyStateBlock.Builder> legacyStateBuilder,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {
        return stateAliasProperties(ShadowsAndPetals.MOD_ID, oldPath, legacyStateBuilder, converter);
    }

    /**
     * Registers a cross-namespace legacy block-state alias whose properties are declared inline.
     */
    public RegBlockBuilder<B> stateAliasProperties(
            String oldNamespace,
            String oldPath,
            Consumer<LegacyStateBlock.Builder> legacyStateBuilder,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {
        LegacyStateBlock.Builder builder = LegacyStateBlock.builder();
        legacyStateBuilder.accept(builder);
        return stateAlias(oldNamespace, oldPath, LegacyStateBlock.factory(builder.build()), converter);
    }

    /**
     * Finalizes registration and applies all configured side effects.
     */
    @SuppressWarnings("unchecked")
    public DeferredBlock<B> register() {
        DeferredBlock<B> deferredBlock;
        if (blockFactory == null) {
            DeferredBlock<Block> simpleBlock = registry.registerSimpleBlock(name, propertiesFactory);
            deferredBlock = (DeferredBlock<B>) simpleBlock;
        } else {
            deferredBlock = registry.registerBlock(name, blockFactory, propertiesFactory);
        }

        registerStateAliases(deferredBlock);
        postRegister(deferredBlock);
        registerBlockTags(deferredBlock);

        if (withItem) {
            registerBlockItem(deferredBlock);
            applyItemModelUnchecked(deferredBlock);
            applyClientItemUnchecked(deferredBlock);
        }

        if (hasTooltipDescription && withItem) {
            Identifier itemId = ShadowsAndPetals.asResource(name);
            TooltipModifier.register(itemId, new ItemDescription.Modifier(() ->
                BuiltInRegistries.ITEM.getValue(itemId)));
        }

        registerCreativeTabs(deferredBlock);
        return deferredBlock;
    }

    private void postRegister(DeferredBlock<? extends Block> block) {
        applyAliases(block.getId());
        applyLang(block.getId().getPath());
        applyBlockStateUnchecked(block);
        applyBlockLootUnchecked(block);
        applyRecipeUnchecked(block);
        applyConnectedTextureUnchecked(block);
    }

    private void applyAliases(Identifier targetId) {
        for (Identifier alias : aliases) {
            registry.addAlias(alias, targetId);
        }
    }

    private void registerBlockTags(DeferredBlock<B> block) {
        for (TagKey<Block> tag : blockTags) {
            BlockTagRegistry.add(tag, block);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyConnectedTextureUnchecked(DeferredBlock<? extends Block> block) {
        if (ctTextureType == null) {
            return;
        }

        DeferredBlock<B> typedBlock = (DeferredBlock<B>) block;
        CTRegistry.register(
                block.getId(),
                ctBaseTextureFactory.apply(typedBlock),
                ctConnectedTextureFactory.apply(typedBlock),
                ctTextureType,
                ctPadding);
    }

    private void registerStateAliases(DeferredBlock<B> targetBlock) {
        for (int i = 0; i < stateAliases.size(); i++) {
            registerStateAlias(targetBlock, stateAliases.get(i), i);
        }
    }

    private <L extends Block> void registerStateAlias(DeferredBlock<B> targetBlock, StateAliasSpec<L> aliasSpec, int index) {
        String compatName = buildCompatAliasName(aliasSpec.aliasId(), index);
        DeferredBlock<L> compatBlock = registry.registerBlock(compatName, aliasSpec.factory(), propertiesFactory);
        registry.addAlias(aliasSpec.aliasId(), compatBlock.getId());
        BlockStateAliasRegistry.add(compatBlock, () -> targetBlock.get().defaultBlockState(), aliasSpec.converter());
        DatagenBlockLootRegistry.add(compatBlock.getId(), provider -> provider.addTable(compatBlock.get(), provider.noDropTable()));
    }

    private String buildCompatAliasName(Identifier aliasId, int index) {
        return LegacyCompatIds.blockName(name, aliasId, index);
    }

    private void applyLang(String path) {
        DatagenLangRegistry.addFallback("block." + ShadowsAndPetals.MOD_ID + "." + path, path);
        for (Map.Entry<String, String> entry : langNames.entrySet()) {
            DatagenLangRegistry.add(entry.getKey(), "block." + ShadowsAndPetals.MOD_ID + "." + path, entry.getValue());
        }
        if (withItem) {
            DatagenLangRegistry.addFallback("item." + ShadowsAndPetals.MOD_ID + "." + path, path);
            for (Map.Entry<String, String> entry : langNames.entrySet()) {
                DatagenLangRegistry.add(entry.getKey(), "item." + ShadowsAndPetals.MOD_ID + "." + path, entry.getValue());
            }
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

    @SuppressWarnings("unchecked")
    private void applyBlockLootUnchecked(DeferredBlock<? extends Block> block) {
        DeferredBlock<B> typedBlock = (DeferredBlock<B>) block;
        if (blockLootGenerator != null) {
            DatagenBlockLootRegistry.add(block.getId(), provider -> blockLootGenerator.accept(provider, typedBlock));
            return;
        }

        if (withItem) {
            DatagenBlockLootRegistry.add(block.getId(), provider -> provider.dropSelf(typedBlock.get()));
        }
    }

    @SuppressWarnings("unchecked")
    private void applyRecipeUnchecked(DeferredBlock<? extends Block> block) {
        if (recipeGenerator == null) {
            return;
        }

        DeferredBlock<B> typedBlock = (DeferredBlock<B>) block;
        DatagenRecipeRegistry.add(block.getId(), provider -> recipeGenerator.accept(provider, typedBlock));
    }

    @SuppressWarnings("unchecked")
    private void applyItemModelUnchecked(DeferredBlock<? extends Block> block) {
        if (itemModelGenerator == null) {
            return;
        }

        DeferredBlock<B> typedBlock = (DeferredBlock<B>) block;
        DatagenItemModelRegistry.add(block.getId(), provider -> itemModelGenerator.accept(provider, typedBlock));
    }

    @SuppressWarnings("unchecked")
    private void applyClientItemUnchecked(DeferredBlock<? extends Block> block) {
        DeferredBlock<B> typedBlock = (DeferredBlock<B>) block;
        Identifier modelId = clientItemModelFactory != null
                ? clientItemModelFactory.apply(typedBlock)
                : ShadowsAndPetals.asResource("item/" + block.getId().getPath());
        DatagenClientItemRegistry.add(block.getId(), modelId);
    }

    private void registerBlockItem(DeferredBlock<? extends Block> block) {
        DeferredRegister.Items items = SAPRegistries.ITEMS;
        if (itemFactory != null) {
            items.register(name, key -> itemFactory.apply(block.get()));
        } else {
            final Item.Properties props = itemProperties;
            items.register(name, key -> new BlockItem(block.get(), props.setId(ResourceKey.create(Registries.ITEM, key))));
        }
    }

    private void registerCreativeTabs(DeferredBlock<? extends Block> block) {
        if (creativeTabs.isEmpty()) {
            return;
        }

        if (!withItem) {
            throw new IllegalStateException("Block '" + name + "' cannot be added to a creative tab without an item");
        }

        for (CreativeTabType tab : creativeTabs) {
            CreativeTabContentsRegistry.add(tab, block::get);
        }
    }

    /**
     * Registers a plain {@link Block} without item, datagen, or alias side effects beyond the block entry itself.
     */
    public DeferredBlock<Block> simple() {
        DeferredBlock<Block> block = registry.registerSimpleBlock(name, propertiesFactory);
        postRegister(block);
        return block;
    }

    /**
     * Registers a plain {@link Block} and a default {@link BlockItem} using the current item properties.
     */
    public DeferredBlock<Block> simpleWithItem() {
        DeferredBlock<Block> block = registry.registerSimpleBlock(name, propertiesFactory);
        this.withItem = true;
        postRegister(block);
        final Item.Properties props = itemProperties != null ? itemProperties : new Item.Properties();
        SAPRegistries.ITEMS.register(name, key -> new BlockItem(block.get(), props.setId(ResourceKey.create(Registries.ITEM, key))));
        applyClientItemUnchecked(block);
        return block;
    }

    private record StateAliasSpec<L extends Block>(
            Identifier aliasId,
            Function<BlockBehaviour.Properties, L> factory,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {}
}
