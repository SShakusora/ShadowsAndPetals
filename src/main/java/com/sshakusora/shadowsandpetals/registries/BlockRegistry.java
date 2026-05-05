package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.block.DyedBlockList;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.*;
import com.sshakusora.shadowsandpetals.compat.CompatInfo;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeFactory;
import com.sshakusora.shadowsandpetals.util.WoolUtils;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.registries.DeferredBlock;

public class BlockRegistry {
    public static final DeferredBlock<DropExperienceBlock> BAUXITE_ORE = SAPRegistries
            .block("bauxite_ore", props -> new DropExperienceBlock(UniformInt.of(1, 3), props))
            .alias(CompatInfo.CHINJUFU_MOD, "block_bauxite_ore")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .loot((provider, ore) -> provider.dropOre(ore.get(), ItemRegistry.RAW_ALUMINUM.get()))
            .register();

    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = SAPRegistries
            .block("deepslate_bauxite_ore", props -> new DropExperienceBlock(UniformInt.of(1, 3), props))
            .alias(CompatInfo.CHINJUFU_MOD, "block_bauxite_ore_deep")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .loot((provider, ore) -> provider.dropOre(ore.get(), ItemRegistry.RAW_ALUMINUM.get()))
            .register();

    public static final DeferredBlock<IngotPileBlock> ALUMINUM_INGOT_PILE = CompatInfo.ingotPileStateAlias(SAPRegistries
            .block("aluminum_ingot_pile", IngotPileBlock::new), "block_alumi_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, ItemRegistry.ALUMINUM_INGOT.get(), "aluminum_ingot_from_pile"))
            .register();

    public static final DeferredBlock<IngotPileBlock> IRON_INGOT_PILE = CompatInfo.ingotPileStateAlias(SAPRegistries
            .block("iron_ingot_pile", IngotPileBlock::new), "block_steel_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.IRON_INGOT, "iron_ingot_from_pile"))
            .register();

    public static final DeferredBlock<IngotPileBlock> COPPER_INGOT_PILE = CompatInfo.ingotPileStateAlias(SAPRegistries
            .block("copper_ingot_pile", IngotPileBlock::new), "block_copper_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.COPPER_INGOT, "copper_ingot_from_pile"))
            .register();

    public static final DeferredBlock<IngotPileBlock> GOLD_INGOT_PILE = CompatInfo.ingotPileStateAlias(SAPRegistries
            .block("gold_ingot_pile", IngotPileBlock::new), "block_gold_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.GOLD_INGOT, "gold_ingot_from_pile"))
            .register();

    public static final DeferredBlock<IngotPileBlock> NETHERITE_INGOT_PILE = CompatInfo.ingotPileStateAlias(SAPRegistries
            .block("netherite_ingot_pile", IngotPileBlock::new), "block_netherite_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.NETHERITE_INGOT, "netherite_ingot_from_pile"))
            .register();

    public static final WoodBlockList<ModularDeskBlock> MODULAR_DESKS = new WoodBlockList<>(woodType -> SAPRegistries.
            block(woodType.getName() + "_modular_desk", ModularDeskBlock::new)
            .alias(CompatInfo.CHINJUFU_MOD, CompatInfo.getWoodBlockAlias1(woodType, "block_unitdesk"))
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

    public static final WoodBlockList<CafeTableBlock> CAFE_TABLES = new WoodBlockList<>(woodType -> SAPRegistries.
            block(woodType.getName() + "_cafe_table", CafeTableBlock::new)
            .alias(CompatInfo.CHINJUFU_MOD, CompatInfo.getWoodBlockAlias1(woodType, "block_cafetable"))
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

    public static final WoodBlockList<DiningChairBlock> DINING_CHAIRS = new WoodBlockList<>(woodType -> SAPRegistries
            .block(woodType.getName() + "_dining_chair", DiningChairBlock::new)
            .stateAliasProperties(CompatInfo.CHINJUFU_MOD, CompatInfo.getWoodBlockAlias2(woodType, "block_diningchair"), legacy -> legacy
                            .property(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                            .property(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                            .property(BlockStateProperties.WATERLOGGED, false),
                    (legacyState, targetState) -> legacyState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                            ? targetState
                            .setValue(DiningChairBlock.FACING, legacyState.getValue(BlockStateProperties.HORIZONTAL_FACING))
                            .setValue(DiningChairBlock.WATERLOGGED, legacyState.getValue(BlockStateProperties.WATERLOGGED))
                            : legacyState.getValue(BlockStateProperties.WATERLOGGED)
                            ? Blocks.WATER.defaultBlockState()
                            : Blocks.AIR.defaultBlockState())
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .recipe((provider, chair) -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, chair.get())
                    .define('W', woodType.getSlab())
                    .define('S', Items.STICK)
                    .pattern("W  ")
                    .pattern("WWW")
                    .pattern("S S")
                    .unlockedBy(provider.hasName(woodType.getPlanks()), provider.hasItem(woodType.getPlanks()))
                    .save(provider.output()))
            .register());

    public static final DyedBlockList<CafeChairBlock> CAFE_CHAIRS = new DyedBlockList<>(color -> SAPRegistries
            .block(color.getName() + "_cafe_chair", CafeChairBlock::new)
            .alias(CompatInfo.CHINJUFU_MOD, CompatInfo.getDyedBlockAlias(color, "block_cafechair"))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, chair) -> provider.simpleBlockWithItem(
                    chair.get(),
                    provider.models().getExistingFile(provider.modLoc("block/cafe_chair/" + color.getName()))
            ))
            .recipe((provider, chair) -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, chair.get())
                    .define('W', WoolUtils.getWool(color))
                    .define('S', Items.STICK)
                    .pattern(" W ")
                    .pattern(" S ")
                    .pattern("SSS")
                    .unlockedBy(provider.hasName(WoolUtils.getWool(color)), provider.hasItem(WoolUtils.getWool(color)))
                    .save(provider.output()))
            .register());

    public static void init() {}
}
