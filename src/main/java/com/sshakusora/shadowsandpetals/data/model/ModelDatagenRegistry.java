package com.sshakusora.shadowsandpetals.data.model;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ModelDatagenRegistry {
    private static final Map<Identifier, RegisteredBlock<?>> BLOCKS = new LinkedHashMap<>();
    private static final Map<Identifier, RegisteredItem<?>> ITEMS = new LinkedHashMap<>();

    private ModelDatagenRegistry() {
    }

    public static <B extends Block> void addBlock(
            DeferredBlock<B> block,
            @Nullable Supplier<? extends BlockModelCallback<B>> callback
    ) {
        BLOCKS.put(block.getId(), new RegisteredBlock<>(new BlockModelContext<>(block.getId(), block), callback));
    }

    public static <I extends Item> void addItem(
            DeferredItem<I> item,
            @Nullable Supplier<? extends ItemModelCallback<I>> callback,
            @Nullable Identifier clientModel,
            @Nullable Identifier customClientType
    ) {
        if (clientModel != null && customClientType != null) {
            throw new IllegalArgumentException("An item cannot have both a vanilla and custom client model: " + item.getId());
        }
        ITEMS.put(item.getId(), new RegisteredItem<>(
                new ItemModelContext<>(item.getId(), item),
                callback,
                clientModel,
                customClientType
        ));
    }

    public static void generateBlocks(SAPBlockModelGenerator generator) {
        BLOCKS.values().forEach(entry -> generateBlock(entry, generator));
    }

    public static void generateItemModels(SAPItemModelGenerator generator) {
        ITEMS.values().forEach(entry -> generateItem(entry, generator));
    }

    public static void finalizeClientItems(SAPItemModelGenerator generator) {
        ITEMS.values().forEach(entry -> finalizeClientItem(entry, generator));
    }

    public static Stream<? extends Holder<Block>> knownBlocks() {
        return BLOCKS.values().stream().map(entry -> entry.context().get().builtInRegistryHolder());
    }

    public static Stream<? extends Holder<Item>> knownItems() {
        return ITEMS.values().stream().map(entry -> entry.context().get().builtInRegistryHolder());
    }

    private static <B extends Block> void generateBlock(RegisteredBlock<B> entry, SAPBlockModelGenerator generator) {
        if (entry.callback() != null) {
            Objects.requireNonNull(entry.callback().get()).generate(entry.context(), generator);
        }
    }

    private static <I extends Item> void generateItem(RegisteredItem<I> entry, SAPItemModelGenerator generator) {
        if (entry.callback() != null) {
            Objects.requireNonNull(entry.callback().get()).generate(entry.context(), generator);
        }
    }

    private static <I extends Item> void finalizeClientItem(RegisteredItem<I> entry, SAPItemModelGenerator generator) {
        generator.finalizeClientItem(
                entry.context().get(),
                entry.clientModel(),
                entry.customClientType()
        );
    }

    private record RegisteredBlock<B extends Block>(
            BlockModelContext<B> context,
            @Nullable Supplier<? extends BlockModelCallback<B>> callback
    ) {
    }

    private record RegisteredItem<I extends Item>(
            ItemModelContext<I> context,
            @Nullable Supplier<? extends ItemModelCallback<I>> callback,
            @Nullable Identifier clientModel,
            @Nullable Identifier customClientType
    ) {
    }
}
