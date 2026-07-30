package com.sshakusora.shadowsandpetals.item.harrow;

import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

public final class HarrowUseAnimationEnumExtensions {
    public static final EnumProxy<ItemUseAnimation> SHADOWSANDPETALS_HARROW_DIGGING = new EnumProxy<>(
            ItemUseAnimation.class,
            -1,
            "shadowsandpetals:harrow_digging",
            true
    );

    private HarrowUseAnimationEnumExtensions() {
    }

    public static ItemUseAnimation getHarrowDigging() {
        return SHADOWSANDPETALS_HARROW_DIGGING.getValue();
    }
}
