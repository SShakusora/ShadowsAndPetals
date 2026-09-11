package com.sshakusora.shadowsandpetals.data.model;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Compatibility registry for model callbacks authored against 26.x.
 * 1.21.1 datagen providers use the legacy NeoForge model API, so callbacks are
 * intentionally retained as optional no-op hooks.
 */
public final class ModelDatagenRegistry {
    private static final Map<ResourceLocation, RegisteredBlock<?>> BLOCKS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, RegisteredItem<?>> ITEMS = new LinkedHashMap<>();

    private ModelDatagenRegistry() {}

    public static <B extends Block> void addBlock(
            DeferredBlock<B> block,
            @Nullable Supplier<? extends BlockModelCallback<B>> callback
    ) {
        BLOCKS.put(block.getId(), new RegisteredBlock<>(new BlockModelContext<>(block.getId(), block), callback));
    }

    public static <I extends Item> void addItem(
            DeferredItem<I> item,
            @Nullable Supplier<? extends ItemModelCallback<I>> callback,
            @Nullable ResourceLocation clientModel,
            @Nullable ResourceLocation customClientType
    ) {
        ITEMS.put(item.getId(), new RegisteredItem<>(new ItemModelContext<>(item.getId(), item), callback, clientModel, customClientType));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void generateBlocks(SAPBlockModelGenerator generator) {
        BLOCKS.values().forEach(entry -> {
            if (entry.callback() == null) {
                return;
            }
            BlockModelCallback callback = entry.callback().get();
            if (callback != null) {
                callback.generate(entry.context(), generator);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void generateItemModels(SAPItemModelGenerator generator) {
        ITEMS.values().forEach(entry -> {
            if (entry.callback() == null) {
                return;
            }
            ItemModelCallback callback = entry.callback().get();
            if (callback != null) {
                callback.generate(entry.context(), generator);
            }
        });
    }

    public static void finalizeClientItems(SAPItemModelGenerator generator) {
        ITEMS.values().forEach(entry -> generator.finalizeClientItem(
                entry.context().get(), entry.clientModel(), entry.customClientType()));
    }

    public static Stream<? extends Holder<Block>> knownBlocks() {
        return BLOCKS.values().stream().map(entry -> entry.context().get().builtInRegistryHolder());
    }

    public static Stream<? extends Holder<Item>> knownItems() {
        return ITEMS.values().stream().map(entry -> entry.context().get().builtInRegistryHolder());
    }

    private record RegisteredBlock<B extends Block>(
            BlockModelContext<B> context,
            @Nullable Supplier<? extends BlockModelCallback<B>> callback) {}

    private record RegisteredItem<I extends Item>(
            ItemModelContext<I> context,
            @Nullable Supplier<? extends ItemModelCallback<I>> callback,
            @Nullable ResourceLocation clientModel,
            @Nullable ResourceLocation customClientType) {}
}
