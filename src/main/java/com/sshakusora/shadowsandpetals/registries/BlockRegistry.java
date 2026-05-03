package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.block.DyedBlockList;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.CafeChairBlock;
import com.sshakusora.shadowsandpetals.compat.CompatInfo;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockRegistry {
    // TODO 需要对应方块类
    public static final WoodBlockList<Block> MODULAR_DESKS = new WoodBlockList<>(woodType -> SAPRegistries.
            block(woodType.getName() + "_modular_desk", Block::new)
            .alias(CompatInfo.CM, CompatInfo.getWoodBlockAlias(woodType, "block_unitdesk"))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, desk) -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, desk.get())
                    .define('W', woodType.getSlab())
                    .pattern("WWW")
                    .pattern("W W")
                    .pattern("W W")
                    .unlockedBy(provider.hasName(woodType.getPlanks()), provider.hasItem(woodType.getPlanks()))
                    .save(provider.output()))
            .register());

    // TODO 需要对应方块类
    public static final WoodBlockList<Block> CAFE_TABLES = new WoodBlockList<>(woodType -> SAPRegistries.
            block(woodType.getName() + "_cafe_table", Block::new)
            .alias(CompatInfo.CM, CompatInfo.getWoodBlockAlias(woodType, "block_cafetable"))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, desk) -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, desk.get())
                    .define('W', woodType.getSlab())
                    .define('S', Items.STICK)
                    .pattern("WWW")
                    .pattern(" S ")
                    .pattern("SSS")
                    .unlockedBy(provider.hasName(woodType.getPlanks()), provider.hasItem(woodType.getPlanks()))
                    .save(provider.output()))
            .register());

    public static final DyedBlockList<CafeChairBlock> CAFE_CHAIRS = new DyedBlockList<>(color -> SAPRegistries
            .block(color.getName() + "_cafe_chair", CafeChairBlock::new)
            .alias(CompatInfo.CM, CompatInfo.getDyedBlockAlias(color, "block_cafechair"))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, chair) -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, chair.get())
                    .define('W', getWool(color))
                    .define('S', Items.STICK)
                    .pattern(" W ")
                    .pattern(" S ")
                    .pattern("SSS")
                    .unlockedBy(provider.hasName(getWool(color)), provider.hasItem(getWool(color)))
                    .save(provider.output()))
            .register());

    public static void init() {}

    private static Block getWool(DyeColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_WOOL;
            case ORANGE -> Blocks.ORANGE_WOOL;
            case MAGENTA -> Blocks.MAGENTA_WOOL;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
            case YELLOW -> Blocks.YELLOW_WOOL;
            case LIME -> Blocks.LIME_WOOL;
            case PINK -> Blocks.PINK_WOOL;
            case GRAY -> Blocks.GRAY_WOOL;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL;
            case CYAN -> Blocks.CYAN_WOOL;
            case PURPLE -> Blocks.PURPLE_WOOL;
            case BLUE -> Blocks.BLUE_WOOL;
            case BROWN -> Blocks.BROWN_WOOL;
            case GREEN -> Blocks.GREEN_WOOL;
            case RED -> Blocks.RED_WOOL;
            case BLACK -> Blocks.BLACK_WOOL;
        };
    }
}
