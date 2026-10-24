package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.registries.builder.RegCreativeTabBuilder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CreativeTabRegistry {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = register(CreativeTabType.MAIN, null);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NATURE = register(CreativeTabType.NATURE, null);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BOXES = register(
            CreativeTabType.BOXES,
            () -> BlockRegistry.BOX.get()
    );

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> register(CreativeTabType type, @Nullable Supplier<ItemLike> icon) {
        RegCreativeTabBuilder builder = SAPRegistries
                .creativeTab(type.getName())
                .lang(type.getLangName())
                .lang("zh_cn", type.getZhCnLangName())
                .addItems(CreativeTabContentsRegistry.generator(type));
        if (icon != null) {
            builder.icon(icon);
        }
        DeferredHolder<CreativeModeTab, CreativeModeTab> holder = builder.register();
        type.bind(holder);
        return holder;
    }

    public static void init() {}
}
