package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.DyedBlockList;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.block.decoration.CafeChairBlock;
import com.sshakusora.shadowsandpetals.block.decoration.IngotPileBlock;
import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeFactory;
import com.sshakusora.shadowsandpetals.util.WoolUtils;
import com.sshakusora.shadowsandpetals.worldgen.SAPTreeGrowers;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

public class BlockRegistry {

    public static final WoodSetList WOOD_SETS = new WoodSetList();

    private static final WoodSetList.WoodSet SAKURA_SET = WOOD_SETS.get(WoodSetList.Type.SAKURA);
    private static final WoodSetList.WoodSet MAPLE_SET = WOOD_SETS.get(WoodSetList.Type.MAPLE);
    private static final WoodSetList.WoodSet GINKGO_SET = WOOD_SETS.get(WoodSetList.Type.GINKGO);

    public static final DeferredBlock<RotatedPillarBlock> SAKURA_LOG = SAKURA_SET.log();
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SAKURA_LOG = SAKURA_SET.strippedLog();
    public static final DeferredBlock<RotatedPillarBlock> SAKURA_WOOD = SAKURA_SET.wood();
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SAKURA_WOOD = SAKURA_SET.strippedWood();
    public static final DeferredBlock<Block> SAKURA_PLANKS = SAKURA_SET.planks();
    public static final DeferredBlock<WoodPostBlock> SAKURA_POST = SAKURA_SET.post();
    public static final DeferredBlock<WoodPostBlock> STRIPPED_SAKURA_POST = SAKURA_SET.strippedPost();
    public static final DeferredBlock<WoodPostBlock> SAKURA_WOOD_POST = SAKURA_SET.woodPost();
    public static final DeferredBlock<WoodPostBlock> STRIPPED_SAKURA_WOOD_POST = SAKURA_SET.strippedWoodPost();
    public static final DeferredBlock<SlabBlock> SAKURA_SLAB = SAKURA_SET.slab();
    public static final DeferredBlock<StairBlock> SAKURA_STAIRS = SAKURA_SET.stairs();
    public static final DeferredBlock<FenceBlock> SAKURA_FENCE = SAKURA_SET.fence();
    public static final DeferredBlock<FenceGateBlock> SAKURA_FENCE_GATE = SAKURA_SET.fenceGate();
    public static final DeferredBlock<PressurePlateBlock> SAKURA_PRESSURE_PLATE = SAKURA_SET.pressurePlate();
    public static final DeferredBlock<ButtonBlock> SAKURA_BUTTON = SAKURA_SET.button();
    public static final DeferredBlock<SaplingBlock> SAKURA_SAPLING = SAKURA_SET.sapling();
    public static final DeferredBlock<LeavesBlock> SAKURA_LEAVES = SAKURA_SET.leaves();

    public static final DeferredBlock<RotatedPillarBlock> MAPLE_LOG = MAPLE_SET.log();
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MAPLE_LOG = MAPLE_SET.strippedLog();
    public static final DeferredBlock<RotatedPillarBlock> MAPLE_WOOD = MAPLE_SET.wood();
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MAPLE_WOOD = MAPLE_SET.strippedWood();
    public static final DeferredBlock<Block> MAPLE_PLANKS = MAPLE_SET.planks();
    public static final DeferredBlock<WoodPostBlock> MAPLE_POST = MAPLE_SET.post();
    public static final DeferredBlock<WoodPostBlock> STRIPPED_MAPLE_POST = MAPLE_SET.strippedPost();
    public static final DeferredBlock<WoodPostBlock> MAPLE_WOOD_POST = MAPLE_SET.woodPost();
    public static final DeferredBlock<WoodPostBlock> STRIPPED_MAPLE_WOOD_POST = MAPLE_SET.strippedWoodPost();
    public static final DeferredBlock<SlabBlock> MAPLE_SLAB = MAPLE_SET.slab();
    public static final DeferredBlock<StairBlock> MAPLE_STAIRS = MAPLE_SET.stairs();
    public static final DeferredBlock<FenceBlock> MAPLE_FENCE = MAPLE_SET.fence();
    public static final DeferredBlock<FenceGateBlock> MAPLE_FENCE_GATE = MAPLE_SET.fenceGate();
    public static final DeferredBlock<PressurePlateBlock> MAPLE_PRESSURE_PLATE = MAPLE_SET.pressurePlate();
    public static final DeferredBlock<ButtonBlock> MAPLE_BUTTON = MAPLE_SET.button();
    public static final DeferredBlock<SaplingBlock> MAPLE_SAPLING = MAPLE_SET.sapling();
    public static final DeferredBlock<LeavesBlock> MAPLE_LEAVES = MAPLE_SET.leaves();

    public static final DeferredBlock<RotatedPillarBlock> GINKGO_LOG = GINKGO_SET.log();
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_GINKGO_LOG = GINKGO_SET.strippedLog();
    public static final DeferredBlock<RotatedPillarBlock> GINKGO_WOOD = GINKGO_SET.wood();
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_GINKGO_WOOD = GINKGO_SET.strippedWood();
    public static final DeferredBlock<Block> GINKGO_PLANKS = GINKGO_SET.planks();
    public static final DeferredBlock<WoodPostBlock> GINKGO_POST = GINKGO_SET.post();
    public static final DeferredBlock<WoodPostBlock> STRIPPED_GINKGO_POST = GINKGO_SET.strippedPost();
    public static final DeferredBlock<WoodPostBlock> GINKGO_WOOD_POST = GINKGO_SET.woodPost();
    public static final DeferredBlock<WoodPostBlock> STRIPPED_GINKGO_WOOD_POST = GINKGO_SET.strippedWoodPost();
    public static final DeferredBlock<SlabBlock> GINKGO_SLAB = GINKGO_SET.slab();
    public static final DeferredBlock<StairBlock> GINKGO_STAIRS = GINKGO_SET.stairs();
    public static final DeferredBlock<FenceBlock> GINKGO_FENCE = GINKGO_SET.fence();
    public static final DeferredBlock<FenceGateBlock> GINKGO_FENCE_GATE = GINKGO_SET.fenceGate();
    public static final DeferredBlock<PressurePlateBlock> GINKGO_PRESSURE_PLATE = GINKGO_SET.pressurePlate();
    public static final DeferredBlock<ButtonBlock> GINKGO_BUTTON = GINKGO_SET.button();
    public static final DeferredBlock<SaplingBlock> GINKGO_SAPLING = GINKGO_SET.sapling();
    public static final DeferredBlock<LeavesBlock> GINKGO_LEAVES = GINKGO_SET.leaves();

    public static final DeferredBlock<SaplingBlock> AUTUMN_OAK_SAPLING = WoodSetList.treeSapling(
            "autumn_oak_sapling",
            SAPTreeGrowers.AUTUMN_OAK,
            "秋橡树树苗"
    );
    public static final DeferredBlock<LeavesBlock> AUTUMN_OAK_LEAVES = WoodSetList.treeLeaves(
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
}
