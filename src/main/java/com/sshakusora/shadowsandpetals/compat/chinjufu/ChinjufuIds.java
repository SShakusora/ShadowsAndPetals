package com.sshakusora.shadowsandpetals.compat.chinjufu;

import com.sshakusora.shadowsandpetals.block.WoodBlockList.WoodType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * Naming rules for ids used by ChinjufuMod 1.20.2.
 *
 * <p>The old mod uses several abbreviated color and wood names. Keeping those rules here makes
 * the registry declarations readable while leaving the actual compatibility mapping explicit.</p>
 */
public final class ChinjufuIds {
    public static final String MOD_ID = "chinjufumod";

    private ChinjufuIds() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String dyedBlockAlias(DyeColor color, String prefix) {
        String colorName = switch (color) {
            case LIGHT_BLUE -> "lightb";
            case LIGHT_GRAY -> "lightg";
            default -> color.getName();
        };
        return prefix + "_" + colorName;
    }

    public static String woodBlockAlias1(WoodType woodType, String prefix) {
        String woodName = switch (woodType) {
            case OAK -> "";
            case DARK_OAK -> "darkoak";
            default -> woodType.getName();
        };
        return woodName.isEmpty() ? prefix : prefix + "_" + woodName;
    }

    public static String woodBlockAlias2(WoodType woodType, String prefix) {
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

    public static String tansuBlockPath(WoodType woodType) {
        String suffix = switch (woodType) {
            case ACACIA -> "acacia";
            case BIRCH -> "birch";
            case DARK_OAK -> "doak";
            case GINKGO -> "ichoh";
            case JUNGLE -> "jungle";
            case MAPLE -> "kaede";
            case OAK -> "oak";
            case SAKURA -> "sakura";
            case SPRUCE -> "spruce";
            default -> null;
        };
        return suffix == null ? null : "block_tansu_" + suffix;
    }
}
