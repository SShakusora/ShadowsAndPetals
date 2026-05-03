package com.sshakusora.shadowsandpetals.compat;

import com.sshakusora.shadowsandpetals.block.WoodBlockList.WoodType;
import net.minecraft.world.item.DyeColor;

public class CompatInfo {
    public static String CM = "chinjufumod";

    public static String getDyedBlockAlias(DyeColor color, String prefix) {
        String colorName = color.equals(DyeColor.LIGHT_BLUE) ? "lightb" :
                           color.equals(DyeColor.LIGHT_GRAY) ? "lightg" :
                           color.getName();
        return prefix + "_" + colorName;
    }

    public static String getWoodBlockAlias(WoodType woodType, String prefix) {
        String woodName = switch (woodType) {
            case OAK -> "";
            case DARK_OAK -> "darkoak";
            default -> woodType.getName();
        };
        return woodName.isEmpty() ? prefix : prefix + "_" + woodName;
    }
}
