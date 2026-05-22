package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.DyedBlockList;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.CafeChairBlock;
import com.sshakusora.shadowsandpetals.block.decoration.IngotPileBlock;
import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import com.sshakusora.shadowsandpetals.block.nature.SAPLeavesBlock;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeFactory;
import com.sshakusora.shadowsandpetals.util.WoolUtils;
import com.sshakusora.shadowsandpetals.worldgen.SAPTreeGrowers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

public class BlockRegistry {

    public static final DeferredBlock<RotatedPillarBlock> SAKURA_LOG = treeLog(
            "sakura_log",
            "樱花原木"
    );
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SAKURA_LOG = strippedTreeLog(
            "stripped_sakura_log",
            "去皮樱花原木"
    );
    public static final DeferredBlock<RotatedPillarBlock> SAKURA_WOOD = treeWood(
            "sakura_wood",
            SAKURA_LOG,
            "樱花木"
    );
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SAKURA_WOOD = strippedTreeWood(
            "stripped_sakura_wood",
            STRIPPED_SAKURA_LOG,
            "去皮樱花木"
    );
    public static final DeferredBlock<Block> SAKURA_PLANKS = treePlanks(
            "sakura_planks",
            SAKURA_LOG,
            STRIPPED_SAKURA_LOG,
            "樱花木板"
    );
    public static final DeferredBlock<SlabBlock> SAKURA_SLAB = treeSlab(
            "sakura_slab",
            SAKURA_PLANKS,
            "樱花木台阶"
    );
    public static final DeferredBlock<StairBlock> SAKURA_STAIRS = treeStairs(
            "sakura_stairs",
            SAKURA_PLANKS,
            "樱花木楼梯"
    );
    public static final DeferredBlock<FenceBlock> SAKURA_FENCE = treeFence(
            "sakura_fence",
            SAKURA_PLANKS,
            "樱花木栅栏"
    );
    public static final DeferredBlock<FenceGateBlock> SAKURA_FENCE_GATE = treeFenceGate(
            "sakura_fence_gate",
            SAKURA_PLANKS,
            "樱花木栅栏门"
    );
    public static final DeferredBlock<PressurePlateBlock> SAKURA_PRESSURE_PLATE = treePressurePlate(
            "sakura_pressure_plate",
            SAKURA_PLANKS,
            "樱花木压力板"
    );
    public static final DeferredBlock<ButtonBlock> SAKURA_BUTTON = treeButton(
            "sakura_button",
            SAKURA_PLANKS,
            "樱花木按钮"
    );
    public static final DeferredBlock<SaplingBlock> SAKURA_SAPLING = treeSapling(
            "sakura_sapling",
            SAPTreeGrowers.SAKURA,
            "樱花树苗"
    );
    public static final DeferredBlock<LeavesBlock> SAKURA_LEAVES = treeLeaves(
            "sakura_leaves",
            SAKURA_SAPLING,
            "樱花树叶"
    );

    public static final DeferredBlock<RotatedPillarBlock> MAPLE_LOG = treeLog(
            "maple_log",
            "枫树原木"
    );
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MAPLE_LOG = strippedTreeLog(
            "stripped_maple_log",
            "去皮枫树原木"
    );
    public static final DeferredBlock<RotatedPillarBlock> MAPLE_WOOD = treeWood(
            "maple_wood",
            MAPLE_LOG,
            "枫木"
    );
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MAPLE_WOOD = strippedTreeWood(
            "stripped_maple_wood",
            STRIPPED_MAPLE_LOG,
            "去皮枫木"
    );
    public static final DeferredBlock<Block> MAPLE_PLANKS = treePlanks(
            "maple_planks",
            MAPLE_LOG,
            STRIPPED_MAPLE_LOG,
            "枫木板"
    );
    public static final DeferredBlock<SlabBlock> MAPLE_SLAB = treeSlab(
            "maple_slab",
            MAPLE_PLANKS,
            "枫木台阶"
    );
    public static final DeferredBlock<StairBlock> MAPLE_STAIRS = treeStairs(
            "maple_stairs",
            MAPLE_PLANKS,
            "枫木楼梯"
    );
    public static final DeferredBlock<FenceBlock> MAPLE_FENCE = treeFence(
            "maple_fence",
            MAPLE_PLANKS,
            "枫木栅栏"
    );
    public static final DeferredBlock<FenceGateBlock> MAPLE_FENCE_GATE = treeFenceGate(
            "maple_fence_gate",
            MAPLE_PLANKS,
            "枫木栅栏门"
    );
    public static final DeferredBlock<PressurePlateBlock> MAPLE_PRESSURE_PLATE = treePressurePlate(
            "maple_pressure_plate",
            MAPLE_PLANKS,
            "枫木压力板"
    );
    public static final DeferredBlock<ButtonBlock> MAPLE_BUTTON = treeButton(
            "maple_button",
            MAPLE_PLANKS,
            "枫木按钮"
    );
    public static final DeferredBlock<SaplingBlock> MAPLE_SAPLING = treeSapling(
            "maple_sapling",
            SAPTreeGrowers.MAPLE,
            "枫树树苗"
    );
    public static final DeferredBlock<LeavesBlock> MAPLE_LEAVES = treeLeaves(
            "maple_leaves",
            MAPLE_SAPLING,
            "枫树树叶"
    );

    public static final DeferredBlock<RotatedPillarBlock> GINKGO_LOG = treeLog(
            "ginkgo_log",
            "银杏原木"
    );
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_GINKGO_LOG = strippedTreeLog(
            "stripped_ginkgo_log",
            "去皮银杏原木"
    );
    public static final DeferredBlock<RotatedPillarBlock> GINKGO_WOOD = treeWood(
            "ginkgo_wood",
            GINKGO_LOG,
            "银杏木"
    );
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_GINKGO_WOOD = strippedTreeWood(
            "stripped_ginkgo_wood",
            STRIPPED_GINKGO_LOG,
            "去皮银杏木"
    );
    public static final DeferredBlock<Block> GINKGO_PLANKS = treePlanks(
            "ginkgo_planks",
            GINKGO_LOG,
            STRIPPED_GINKGO_LOG,
            "银杏木板"
    );
    public static final DeferredBlock<SlabBlock> GINKGO_SLAB = treeSlab(
            "ginkgo_slab",
            GINKGO_PLANKS,
            "银杏木台阶"
    );
    public static final DeferredBlock<StairBlock> GINKGO_STAIRS = treeStairs(
            "ginkgo_stairs",
            GINKGO_PLANKS,
            "银杏木楼梯"
    );
    public static final DeferredBlock<FenceBlock> GINKGO_FENCE = treeFence(
            "ginkgo_fence",
            GINKGO_PLANKS,
            "银杏木栅栏"
    );
    public static final DeferredBlock<FenceGateBlock> GINKGO_FENCE_GATE = treeFenceGate(
            "ginkgo_fence_gate",
            GINKGO_PLANKS,
            "银杏木栅栏门"
    );
    public static final DeferredBlock<PressurePlateBlock> GINKGO_PRESSURE_PLATE = treePressurePlate(
            "ginkgo_pressure_plate",
            GINKGO_PLANKS,
            "银杏木压力板"
    );
    public static final DeferredBlock<ButtonBlock> GINKGO_BUTTON = treeButton(
            "ginkgo_button",
            GINKGO_PLANKS,
            "银杏木按钮"
    );
    public static final DeferredBlock<SaplingBlock> GINKGO_SAPLING = treeSapling(
            "ginkgo_sapling",
            SAPTreeGrowers.GINKGO,
            "银杏树苗"
    );
    public static final DeferredBlock<LeavesBlock> GINKGO_LEAVES = treeLeaves(
            "ginkgo_leaves",
            GINKGO_SAPLING,
            "银杏树叶"
    );

    public static final DeferredBlock<SaplingBlock> AUTUMN_OAK_SAPLING = treeSapling(
            "autumn_oak_sapling",
            SAPTreeGrowers.AUTUMN_OAK,
            "秋橡树树苗"
    );
    public static final DeferredBlock<LeavesBlock> AUTUMN_OAK_LEAVES = treeLeaves(
            "autumn_oak_leaves",
            AUTUMN_OAK_SAPLING,
            "秋橡树树叶"
    );

    public static final DeferredBlock<DropExperienceBlock> BAUXITE_ORE = SAPRegistries
            .block("bauxite_ore", props -> new DropExperienceBlock(UniformInt.of(1, 3), props))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, ore) -> provider.cubeAllBlockWithItem(ore.get(), provider.modLoc("block/bauxite_ore/bauxite_ore")))
            .loot((provider, ore) -> provider.dropOre(ore.get(), ItemRegistry.RAW_BAUXITE.get()))
            .lang("zh_cn", "矾土矿石")
            .register();

    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = SAPRegistries
            .block("deepslate_bauxite_ore", props -> new DropExperienceBlock(UniformInt.of(1, 3), props))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, ore) -> provider.cubeAllBlockWithItem(ore.get(), provider.modLoc("block/bauxite_ore/deepslate_bauxite_ore")))
            .loot((provider, ore) -> provider.dropOre(ore.get(), ItemRegistry.RAW_BAUXITE.get()))
            .lang("zh_cn", "深层矾土矿石")
            .register();

    public static final DeferredBlock<Block> RAW_BAUXITE_BLOCK = SAPRegistries
            .block("raw_bauxite_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, block) -> provider.cubeAllBlockWithItem(block.get()))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> DatagenRecipeFactory.storageBlock(provider, block, ItemRegistry.RAW_BAUXITE.get(), "raw_bauxite_from_block"))
            .lang("zh_cn", "粗矾土块")
            .register();

    public static final DeferredBlock<Block> ALUMINUM_BLOCK = SAPRegistries
            .block("aluminum_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, block) -> provider.cubeAllBlockWithItem(block.get()))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> DatagenRecipeFactory.storageBlock(provider, block, ItemRegistry.ALUMINUM_INGOT.get(), "aluminum_ingot_from_block"))
            .lang("zh_cn", "铝块")
            .register();

    public static final DeferredBlock<IngotPileBlock> ALUMINUM_INGOT_PILE = SAPRegistries
            .block("aluminum_ingot_pile", IngotPileBlock::new)
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

    public static final DeferredBlock<IngotPileBlock> IRON_INGOT_PILE = SAPRegistries
            .block("iron_ingot_pile", IngotPileBlock::new)
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

    public static final DeferredBlock<IngotPileBlock> COPPER_INGOT_PILE = SAPRegistries
            .block("copper_ingot_pile", IngotPileBlock::new)
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

    public static final DeferredBlock<IngotPileBlock> GOLD_INGOT_PILE = SAPRegistries
            .block("gold_ingot_pile", IngotPileBlock::new)
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

    public static final DeferredBlock<IngotPileBlock> NETHERITE_INGOT_PILE = SAPRegistries
            .block("netherite_ingot_pile", IngotPileBlock::new)
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

    public static final DeferredBlock<IroriBlock> IRORI = SAPRegistries
            .block("irori", IroriBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, irori) -> provider.iroriBlock(irori.get()))
            .loot((provider, irori) -> provider.dropSelf(irori.get()))
            .lang("zh_cn", "日式围炉")
            .register();

    public static final WoodBlockList<VanityBlock> VANITIES = new WoodBlockList<>(woodType -> SAPRegistries
            .block(woodType.getName() + "_vanity", VanityBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(woodType.getPlanks())
                    .strength(2.5F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, vanity) -> provider.vanityBlock(vanity.get()))
            .clientItem(ShadowsAndPetals.asResource("item/vanity/" + woodType.getName()))
            .recipe((provider, vanity) -> provider.shaped(RecipeCategory.DECORATIONS, vanity.get())
                    .define('S', woodType.getSlab())
                    .define('G', Items.GLASS_PANE)
                    .pattern("S  ")
                    .pattern("G  ")
                    .pattern("SSS")
                    .unlockedBy(provider.hasName(woodType.getPlanks()), provider.hasItem(woodType.getPlanks()))
                    .save(provider.output()))
            .lang("zh_cn", woodType.getZhName() + "梳妆台")
            .register()
    );

//    public static final WoodBlockList<ModularDeskBlock> MODULAR_DESKS = new WoodBlockList<>(woodType -> SAPRegistries.
//            block(woodType.getName() + "_modular_desk", ModularDeskBlock::new)
//            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
//                    .strength(2.0F, 3.0F)
//                    .sound(SoundType.WOOD)
//                    .noOcclusion())
//            .withItem()
//            .creativeTab(CreativeTabType.MAIN)
//            .lang("zh_cn", woodType.getZhName() + "书桌")
//            .recipe((provider, desk) -> provider.shaped(RecipeCategory.DECORATIONS, desk.get())
//                    .define('W', woodType.getSlab())
//                    .pattern("WWW")
//                    .pattern("W W")
//                    .pattern("W W")
//                    .unlockedBy(provider.hasName(woodType.getPlanks()), provider.hasItem(woodType.getPlanks()))
//                    .save(provider.output()))
//            .register());
//
//    public static final WoodBlockList<CafeTableBlock> CAFE_TABLES = new WoodBlockList<>(woodType -> SAPRegistries.
//            block(woodType.getName() + "_cafe_table", CafeTableBlock::new)
//            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
//                    .strength(2.0F, 3.0F)
//                    .sound(SoundType.WOOD)
//                    .noOcclusion())
//            .withItem()
//            .creativeTab(CreativeTabType.MAIN)
//            .lang("zh_cn", woodType.getZhName() + "咖啡桌")
//            .recipe((provider, desk) -> provider.shaped(RecipeCategory.DECORATIONS, desk.get())
//                    .define('W', woodType.getSlab())
//                    .define('S', Items.STICK)
//                    .pattern("WWW")
//                    .pattern(" S ")
//                    .pattern("SSS")
//                    .unlockedBy(provider.hasName(woodType.getPlanks()), provider.hasItem(woodType.getPlanks()))
//                    .save(provider.output()))
//            .register());
//
//    public static final WoodBlockList<DiningChairBlock> DINING_CHAIRS = new WoodBlockList<>(woodType -> SAPRegistries
//            .block(woodType.getName() + "_dining_chair", DiningChairBlock::new)
//            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
//                    .strength(2.0F, 3.0F)
//                    .sound(SoundType.WOOD)
//                    .noOcclusion())
//            .withItem()
//            .creativeTab(CreativeTabType.MAIN)
//            .lang("zh_cn", woodType.getZhName() + "餐椅")
//            .recipe((provider, chair) -> provider.shaped(RecipeCategory.DECORATIONS, chair.get())
//                    .define('W', woodType.getSlab())
//                    .define('S', Items.STICK)
//                    .pattern("W  ")
//                    .pattern("WWW")
//                    .pattern("S S")
//                    .unlockedBy(provider.hasName(woodType.getPlanks()), provider.hasItem(woodType.getPlanks()))
//                    .save(provider.output()))
//            .register());

    public static final DyedBlockList<CafeChairBlock> CAFE_CHAIRS = new DyedBlockList<>(color -> SAPRegistries
            .block(color.getName() + "_cafe_chair", CafeChairBlock::new)
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
            .recipe((provider, chair) -> provider.shaped(RecipeCategory.DECORATIONS, chair.get())
                    .define('W', WoolUtils.getWool(color))
                    .define('S', Items.STICK)
                    .pattern(" W ")
                    .pattern(" S ")
                    .pattern("SSS")
                    .unlockedBy(provider.hasName(WoolUtils.getWool(color)), provider.hasItem(WoolUtils.getWool(color)))
                    .save(provider.output()))
            .register());

    public static void init() {}

    private static DeferredBlock<RotatedPillarBlock> treeLog(String id, String zhName) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)
                        .strength(2.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN, BlockTags.OVERWORLD_NATURAL_LOGS)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .register();
    }

    private static DeferredBlock<RotatedPillarBlock> strippedTreeLog(String id, String zhName) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)
                        .strength(2.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .register();
    }

    private static DeferredBlock<RotatedPillarBlock> treeWood(String id, Supplier<? extends Block> sourceBlock, String zhName) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)
                        .strength(2.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .recipe((provider, wood) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, wood.get(), 3)
                        .define('W', sourceBlock.get())
                        .pattern("WW")
                        .pattern("WW")
                        .unlockedBy(provider.hasName(sourceBlock.get()), provider.hasItem(sourceBlock.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<RotatedPillarBlock> strippedTreeWood(String id, Supplier<? extends Block> sourceBlock, String zhName) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)
                        .strength(2.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .recipe((provider, wood) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, wood.get(), 3)
                        .define('W', sourceBlock.get())
                        .pattern("WW")
                        .pattern("WW")
                        .unlockedBy(provider.hasName(sourceBlock.get()), provider.hasItem(sourceBlock.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<Block> treePlanks(String id, Supplier<? extends Block> log, Supplier<? extends Block> strippedLog, String zhName) {
        return SAPRegistries.block(id)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.PLANKS)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .loot((provider, block) -> provider.dropSelf(block.get()))
                .recipe((provider, block) -> {
                    provider.shapeless(RecipeCategory.BUILDING_BLOCKS, block.get(), 4)
                            .requires(log.get())
                            .unlockedBy(provider.hasName(log.get()), provider.hasItem(log.get()))
                            .save(provider.output(), provider.id(id + "_from_" + BuiltInRegistries.BLOCK.getKey(log.get()).getPath()).toString());
                    provider.shapeless(RecipeCategory.BUILDING_BLOCKS, block.get(), 4)
                            .requires(strippedLog.get())
                            .unlockedBy(provider.hasName(strippedLog.get()), provider.hasItem(strippedLog.get()))
                            .save(provider.output(), provider.id(id + "_from_" + BuiltInRegistries.BLOCK.getKey(strippedLog.get()).getPath()).toString());
                })
                .register();
    }

    private static DeferredBlock<SlabBlock> treeSlab(String id, Supplier<? extends Block> planks, String zhName) {
        return SAPRegistries.block(id, SlabBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.SLABS, BlockTags.WOODEN_SLABS)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .loot((provider, slab) -> provider.dropSlab(slab.get()))
                .recipe((provider, slab) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, slab.get(), 6)
                        .define('W', planks.get())
                        .pattern("WWW")
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<StairBlock> treeStairs(String id, Supplier<? extends Block> planks, String zhName) {
        return SAPRegistries.block(id, properties -> new StairBlock(planks.get().defaultBlockState(), properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.STAIRS, BlockTags.WOODEN_STAIRS)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .loot((provider, stairs) -> provider.dropSelf(stairs.get()))
                .recipe((provider, stairs) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, stairs.get(), 4)
                        .define('W', planks.get())
                        .pattern("W  ")
                        .pattern("WW ")
                        .pattern("WWW")
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<FenceBlock> treeFence(String id, Supplier<? extends Block> planks, String zhName) {
        return SAPRegistries.block(id, FenceBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.FENCES, BlockTags.WOODEN_FENCES)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .loot((provider, fence) -> provider.dropSelf(fence.get()))
                .recipe((provider, fence) -> provider.shaped(RecipeCategory.DECORATIONS, fence.get(), 3)
                        .define('W', planks.get())
                        .define('S', Items.STICK)
                        .pattern("WSW")
                        .pattern("WSW")
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<FenceGateBlock> treeFenceGate(String id, Supplier<? extends Block> planks, String zhName) {
        return SAPRegistries.block(id, properties -> new FenceGateBlock(net.minecraft.world.level.block.state.properties.WoodType.OAK, properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.FENCE_GATES)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .loot((provider, gate) -> provider.dropSelf(gate.get()))
                .recipe((provider, gate) -> provider.shaped(RecipeCategory.REDSTONE, gate.get())
                        .define('W', planks.get())
                        .define('S', Items.STICK)
                        .pattern("SWS")
                        .pattern("SWS")
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<PressurePlateBlock> treePressurePlate(String id, Supplier<? extends Block> planks, String zhName) {
        return SAPRegistries.block(id, properties -> new PressurePlateBlock(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)
                        .strength(0.5F)
                        .sound(SoundType.WOOD)
                        .noCollision())
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_PRESSURE_PLATES, BlockTags.PRESSURE_PLATES)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .loot((provider, plate) -> provider.dropSelf(plate.get()))
                .recipe((provider, plate) -> provider.shaped(RecipeCategory.REDSTONE, plate.get())
                        .define('W', planks.get())
                        .pattern("WW")
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<ButtonBlock> treeButton(String id, Supplier<? extends Block> planks, String zhName) {
        return SAPRegistries.block(id, properties -> new ButtonBlock(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, 30, properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)
                        .strength(0.5F)
                        .sound(SoundType.WOOD)
                        .noCollision())
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.BUTTONS, BlockTags.WOODEN_BUTTONS)
                .withItem()
                .creativeTab(CreativeTabType.NATURE)
                .lang("zh_cn", zhName)
                .loot((provider, button) -> provider.dropSelf(button.get()))
                .recipe((provider, button) -> provider.shapeless(RecipeCategory.REDSTONE, button.get())
                        .requires(planks.get())
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<LeavesBlock> treeLeaves(String id, Supplier<SaplingBlock> sapling, String zhName) {
        return SAPRegistries.<LeavesBlock>block(id, properties -> new SAPLeavesBlock(0.01F, properties))
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
                .blockstate((provider, leaves) -> provider.leavesBlockWithItem(leaves.get(), ShadowsAndPetals.asResource("block/" + id)))
                .loot((provider, leaves) -> provider.dropLeaves(leaves.get(), sapling.get()))
                .register();
    }

    private static DeferredBlock<SaplingBlock> treeSapling(String id, TreeGrower grower, String zhName) {
        return SAPRegistries.block(id, properties -> new SaplingBlock(grower, properties))
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
                .blockstate((provider, sapling) -> provider.saplingBlock(sapling.get(), ShadowsAndPetals.asResource("block/" + id)))
                .itemModel((provider, sapling) -> provider.generatedBlockItem(sapling.get(), ShadowsAndPetals.asResource("block/" + id)))
                .register();
    }
}
