package com.sshakusora.shadowsandpetals.item.harrow;

import net.minecraft.world.item.UseAnim;

/**
 * 1.21.1 fallback for the custom 26.x animation enum entry.
 */
public final class HarrowUseAnimationEnumExtensions {
    private HarrowUseAnimationEnumExtensions() {}

    public static UseAnim getHarrowDigging() {
        return UseAnim.BRUSH;
    }
}
