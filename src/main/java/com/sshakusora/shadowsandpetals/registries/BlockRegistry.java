package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.block.DyedBlockList;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.*;
import com.sshakusora.shadowsandpetals.compat.CompatInfo;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeFactory;
import com.sshakusora.shadowsandpetals.util.WoolUtils;
import com.sshakusora.shadowsandpetals.worldgen.SAPTreeGrowers;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

public class BlockRegistry {
    // TODO: Replace these oak placeholder textures with custom textures for each tree type.
    private static final ResourceLocation OAK_LOG_SIDE_TEXTURE = ResourceLocation.withDefaultNamespace("block/oak_log");
    private static final ResourceLocation OAK_LOG_END_TEXTURE = ResourceLocation.withDefaultNamespace("block/oak_log_top");
    private static final ResourceLocation OAK_LEAVES_TEXTURE = ResourceLocation.withDefaultNamespace("block/oak_leaves");
    private static final ResourceLocation OAK_SAPLING_TEXTURE = ResourceLocation.withDefaultNamespace("block/oak_sapling");

    public static final DeferredBlock<RotatedPillarBlock> SAKURA_LOG = treeLog(
            "sakura_log",
            "block_tree_sakura_log",
            "樱花原木"
    );
    public static final DeferredBlock<SaplingBlock> SAKURA_SAPLING = treeSapling(
            "sakura_sapling",
            SAPTreeGrowers.SAKURA,
            "block_tree_sakura_nae",
            "樱花树苗"
    );
    public static final DeferredBlock<LeavesBlock> SAKURA_LEAVES = treeLeaves(
            "sakura_leaves",
            SAKURA_SAPLING,
            "block_tree_sakura_flow",
            "樱花树叶"
    );

    public static final DeferredBlock<RotatedPillarBlock> MAPLE_LOG = treeLog(
            "maple_log",
            "block_tree_kaede_log",
            "枫树原木"
    );
    public static final DeferredBlock<SaplingBlock> MAPLE_SAPLING = treeSapling(
            "maple_sapling",
            SAPTreeGrowers.MAPLE,
            "block_tree_kaede_nae",
            "枫树树苗"
    );
    public static final DeferredBlock<LeavesBlock> MAPLE_LEAVES = treeLeaves(
            "maple_leaves",
            MAPLE_SAPLING,
            "block_tree_kaede_leaf",
            "枫树树叶"
    );

    public static final DeferredBlock<RotatedPillarBlock> GINKGO_LOG = treeLog(
            "ginkgo_log",
            "block_tree_ichoh_log",
            "银杏原木"
    );
    public static final DeferredBlock<SaplingBlock> GINKGO_SAPLING = treeSapling(
            "ginkgo_sapling",
            SAPTreeGrowers.GINKGO,
            "block_tree_ichoh_nae",
            "银杏树苗"
    );
    public static final DeferredBlock<LeavesBlock> GINKGO_LEAVES = treeLeaves(
            "ginkgo_leaves",
            GINKGO_SAPLING,
            "block_tree_ichoh_leaf",
            "银杏树叶"
    );

    public static final DeferredBlock<SaplingBlock> AUTUMN_OAK_SAPLING = treeSapling(
            "autumn_oak_sapling",
            SAPTreeGrowers.AUTUMN_OAK,
            "block_tree_oakkare_nae",
            "秋橡树树苗"
    );
    public static final DeferredBlock<LeavesBlock> AUTUMN_OAK_LEAVES = treeLeaves(
            "autumn_oak_leaves",
            AUTUMN_OAK_SAPLING,
            "block_tree_oakkare_leaf",
            "秋橡树树叶"
    );

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
            .lang("zh_cn", "矾土矿石")
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
            .lang("zh_cn", "深层矾土矿石")
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
            .blockstate((provider, pile) -> provider.ingotPileBlock(pile.get()))
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, ItemRegistry.ALUMINUM_INGOT.get(), "aluminum_ingot_from_pile"))
            .lang("zh_cn", "铝锭堆")
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
            .blockstate((provider, pile) -> provider.ingotPileBlock(pile.get()))
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.IRON_INGOT, "iron_ingot_from_pile"))
            .lang("zh_cn", "铁锭堆")
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
            .blockstate((provider, pile) -> provider.ingotPileBlock(pile.get()))
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.COPPER_INGOT, "copper_ingot_from_pile"))
            .lang("zh_cn", "铜锭堆")
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
            .blockstate((provider, pile) -> provider.ingotPileBlock(pile.get()))
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.GOLD_INGOT, "gold_ingot_from_pile"))
            .lang("zh_cn", "金锭堆")
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
            .blockstate((provider, pile) -> provider.ingotPileBlock(pile.get()))
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.NETHERITE_INGOT, "netherite_ingot_from_pile"))
            .lang("zh_cn", "下界合金锭堆")
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
            .lang("zh_cn", woodType.getZhName() + "书桌")
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
            .lang("zh_cn", woodType.getZhName() + "咖啡桌")
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
            .lang("zh_cn", woodType.getZhName() + "餐椅")
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
            .lang("zh_cn", DyedBlockList.zhName(color) + "咖啡椅")
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

    private static DeferredBlock<RotatedPillarBlock> treeLog(String id, String compatAlias, String zhName) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .alias(CompatInfo.CHINJUFU_MOD, compatAlias)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)
                        .strength(2.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN, BlockTags.OVERWORLD_NATURAL_LOGS)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .blockstate((provider, log) -> provider.logBlockWithItem(log.get(), OAK_LOG_SIDE_TEXTURE, OAK_LOG_END_TEXTURE))
                .register();
    }

    private static DeferredBlock<LeavesBlock> treeLeaves(String id, Supplier<SaplingBlock> sapling, String compatAlias, String zhName) {
        return SAPRegistries.block(id, LeavesBlock::new)
                .alias(CompatInfo.CHINJUFU_MOD, compatAlias)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                        .strength(0.2F)
                        .sound(SoundType.GRASS)
                        .noOcclusion()
                        .isValidSpawn((state, getter, pos, type) -> false)
                        .isSuffocating((state, getter, pos) -> false)
                        .isViewBlocking((state, getter, pos) -> false))
                .tags(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .blockstate((provider, leaves) -> provider.leavesBlockWithItem(leaves.get(), OAK_LEAVES_TEXTURE))
                .loot((provider, leaves) -> provider.dropLeaves(leaves.get(), sapling.get()))
                .register();
    }

    private static DeferredBlock<SaplingBlock> treeSapling(String id, TreeGrower grower, String compatAlias, String zhName) {
        return SAPRegistries.block(id, properties -> new SaplingBlock(grower, properties))
                .alias(CompatInfo.CHINJUFU_MOD, compatAlias)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)
                        .noOcclusion()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS)
                        .pushReaction(PushReaction.DESTROY))
                .tags(BlockTags.SAPLINGS)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .blockstate((provider, sapling) -> provider.saplingBlockWithItem(sapling.get(), OAK_SAPLING_TEXTURE))
                .register();
    }
}
