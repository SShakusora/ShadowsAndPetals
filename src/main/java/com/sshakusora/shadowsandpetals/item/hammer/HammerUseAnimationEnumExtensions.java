package com.sshakusora.shadowsandpetals.item.hammer;

import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

public final class HammerUseAnimationEnumExtensions {
    public static final EnumProxy<ItemUseAnimation> SHADOWSANDPETALS_HAMMER_AND_CHISEL = new EnumProxy<>(
            ItemUseAnimation.class,
            -1,
            "shadowsandpetals:hammer_and_chisel",
            true
    );

    private HammerUseAnimationEnumExtensions() {
    }

    public static ItemUseAnimation getHammerAndChisel() {
        return SHADOWSANDPETALS_HAMMER_AND_CHISEL.getValue();
    }
}
