package com.sshakusora.shadowsandpetals.item.hammer;

import net.minecraft.world.item.UseAnim;

/**
 * 1.21.1 fallback for the custom 26.x animation enum entry.
 * The vanilla bow pose keeps the long-use interaction readable.
 */
public final class HammerUseAnimationEnumExtensions {
    private HammerUseAnimationEnumExtensions() {}

    public static UseAnim getHammerAndChisel() {
        return UseAnim.BOW;
    }
}
