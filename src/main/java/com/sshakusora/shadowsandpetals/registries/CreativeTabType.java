package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public enum CreativeTabType {
    MAIN("main", "Shadows & Petals", "织影落花", () -> ItemRegistry.HAMMER.get()),
    NATURE("nature", "Shadows & Petals: Nature", "织影落花：自然",
            () -> BlockRegistry.MAPLE_SET.sapling());

    private final String name;
    @Nullable
    private final String langName;
    @Nullable
    private final String zhCnLangName;
    private final Supplier<ItemLike> iconSupplier;
    private DeferredHolder<CreativeModeTab, CreativeModeTab> holder;

    CreativeTabType(String name, @Nullable String langName, @Nullable String zhCnLangName, Supplier<ItemLike> iconSupplier) {
        this.name = name;
        this.langName = langName;
        this.zhCnLangName = zhCnLangName;
        this.iconSupplier = iconSupplier;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getLangName() {
        return langName;
    }

    @Nullable
    public String getZhCnLangName() {
        return zhCnLangName;
    }

    public Supplier<ItemLike> getIcon() {
        return iconSupplier;
    }

    public DeferredHolder<CreativeModeTab, CreativeModeTab> getHolder() {
        if (holder == null) {
            throw new IllegalStateException("Creative tab '" + name + "' has not been registered yet");
        }
        return holder;
    }

    void bind(DeferredHolder<CreativeModeTab, CreativeModeTab> holder) {
        if (this.holder != null) {
            throw new IllegalStateException("Creative tab '" + name + "' is already bound");
        }
        this.holder = holder;
    }
}
