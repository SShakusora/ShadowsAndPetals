package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

public enum CreativeTabType {
    MAIN("main", "Shadows & Petals");

    private final String name;
    @Nullable
    private final String langName;
    private DeferredHolder<CreativeModeTab, CreativeModeTab> holder;

    CreativeTabType(String name, @Nullable String langName) {
        this.name = name;
        this.langName = langName;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getLangName() {
        return langName;
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
