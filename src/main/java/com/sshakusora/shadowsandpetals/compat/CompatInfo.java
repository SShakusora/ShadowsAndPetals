package com.sshakusora.shadowsandpetals.compat;

import com.sshakusora.shadowsandpetals.block.WoodBlockList.WoodType;
import com.sshakusora.shadowsandpetals.block.decoration.IngotPileBlock;
import com.sshakusora.shadowsandpetals.registries.builder.RegBlockBuilder;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

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

    public static RegBlockBuilder<IngotPileBlock> ingotPileStateAlias(RegBlockBuilder<IngotPileBlock> builder, String legacyPath) {
        return builder.stateAliasProperties(CHINJUFU_MOD, legacyPath,
                legacy -> legacy
                        .property(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                        .property(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM)
                        .property(BlockStateProperties.WATERLOGGED, false),
                (legacyState, targetState) -> targetState
                        .setValue(IngotPileBlock.HORIZONTAL_AXIS, legacyState.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis())
                        .setValue(BlockStateProperties.SLAB_TYPE, legacyState.getValue(BlockStateProperties.SLAB_TYPE))
                        .setValue(BlockStateProperties.WATERLOGGED, legacyState.getValue(BlockStateProperties.WATERLOGGED)));
    }
}
