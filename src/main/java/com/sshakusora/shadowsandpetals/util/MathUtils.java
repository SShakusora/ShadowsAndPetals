package com.sshakusora.shadowsandpetals.util;

import net.minecraft.util.Mth;

public final class MathUtils {
    private MathUtils() {
    }

    public static float easeOutCubic(float value) {
        float clampedValue = Mth.clamp(value, 0.0F, 1.0F);
        float inverse = 1.0F - clampedValue;
        return 1.0F - inverse * inverse * inverse;
    }
}
