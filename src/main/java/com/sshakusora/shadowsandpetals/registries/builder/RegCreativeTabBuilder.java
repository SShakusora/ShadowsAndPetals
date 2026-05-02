package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegCreativeTabBuilder {
    private final DeferredRegister<CreativeModeTab> registry;
    private final String name;
    private Component title;
    private Supplier<ItemLike> iconSupplier = () -> Items.BARRIER;
    private final List<ItemStack> items = new ArrayList<>();
    private final List<Consumer<CreativeModeTab.Output>> simpleDisplayGenerators = new ArrayList<>();
    private CreativeModeTab.DisplayItemsGenerator fullGenerator;
    private final List<ResourceLocation> aliases = new ArrayList<>();

    public RegCreativeTabBuilder(DeferredRegister<CreativeModeTab> registry, String name) {
        this.registry = registry;
        this.name = name;
        this.title = Component.translatable("itemGroup." + ShadowsAndPetals.MOD_ID + "." + name);
    }

    public RegCreativeTabBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public RegCreativeTabBuilder title(String translationKey) {
        this.title = Component.translatable(translationKey);
        return this;
    }

    public RegCreativeTabBuilder icon(Supplier<ItemLike> iconSupplier) {
        this.iconSupplier = iconSupplier;
        return this;
    }

    public RegCreativeTabBuilder icon(ItemLike icon) {
        this.iconSupplier = () -> icon;
        return this;
    }

    public RegCreativeTabBuilder addItem(ItemLike item) {
        this.items.add(new ItemStack(item));
        return this;
    }

    public RegCreativeTabBuilder addItem(ItemLike item, int count) {
        this.items.add(new ItemStack(item, count));
        return this;
    }

    public RegCreativeTabBuilder addItems(Iterable<ItemLike> items) {
        for (ItemLike item : items) {
            this.items.add(new ItemStack(item));
        }
        return this;
    }

    public RegCreativeTabBuilder addItems(Consumer<CreativeModeTab.Output> generator) {
        this.simpleDisplayGenerators.add(generator);
        return this;
    }

    public RegCreativeTabBuilder displayItems(CreativeModeTab.DisplayItemsGenerator generator) {
        this.fullGenerator = generator;
        return this;
    }

    public RegCreativeTabBuilder alias(String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath));
        return this;
    }

    public RegCreativeTabBuilder alias(String oldNamespace, String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    public DeferredHolder<CreativeModeTab, CreativeModeTab> register() {
        DeferredHolder<CreativeModeTab, CreativeModeTab> tab = registry.register(name, key -> {
            if (fullGenerator != null) {
                return CreativeModeTab.builder()
                        .title(title)
                        .icon(() -> new ItemStack(iconSupplier.get()))
                        .displayItems(fullGenerator)
                        .build();
            }

            List<ItemStack> localItems = new ArrayList<>(this.items);
            List<Consumer<CreativeModeTab.Output>> localGenerators = new ArrayList<>(this.simpleDisplayGenerators);

            return CreativeModeTab.builder()
                    .title(title)
                    .icon(() -> new ItemStack(iconSupplier.get()))
                    .displayItems((params, output) -> {
                        for (ItemStack stack : localItems) {
                            if (!stack.isEmpty() && stack.getItem().isEnabled(params.enabledFeatures())) {
                                output.accept(stack);
                            }
                        }
                        for (Consumer<CreativeModeTab.Output> generator : localGenerators) {
                            generator.accept(output);
                        }
                    })
                    .build();
        });

        for (ResourceLocation alias : aliases) {
            registry.addAlias(alias, tab.getId());
        }
        return tab;
    }
}
