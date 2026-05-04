package com.sshakusora.shadowsandpetals.compat;

import com.sshakusora.shadowsandpetals.block.WoodBlockList.WoodType;
import net.minecraft.world.item.DyeColor;

public class CompatInfo {
    public static final String CHINJUFU_MOD = "chinjufumod";

    public static String getDyedBlockAlias(DyeColor color, String prefix) {
        String colorName = switch (color) {
            case LIGHT_BLUE -> "lightb";
            case LIGHT_GRAY -> "lightg";
            default -> color.getName();
        };
        return prefix + "_" + colorName;
    }

    public static String getWoodBlockAlias1(WoodType woodType, String prefix) {
        String woodName = switch (woodType) {
            case OAK -> "";
            case DARK_OAK -> "darkoak";
            default -> woodType.getName();
        };
        return woodName.isEmpty() ? prefix : prefix + "_" + woodName;
    }

    public static String getWoodBlockAlias2(WoodType woodType, String prefix) {
        String woodName = switch (woodType) {
            case OAK -> "";
            case SPRUCE -> "s";
            case BIRCH -> "b";
            case JUNGLE -> "j";
            case ACACIA -> "a";
            case DARK_OAK -> "d";
            default -> woodType.getName();
        };
        return woodName.isEmpty() ? prefix : prefix + "_" + woodName;
    }
}
