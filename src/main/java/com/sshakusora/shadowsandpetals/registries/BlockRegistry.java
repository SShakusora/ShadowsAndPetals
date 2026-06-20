package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.DyedBlockList;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.block.decoration.*;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.client.ct.CTTextureType;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryTooltipComponent;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeFactory;
import com.sshakusora.shadowsandpetals.item.HammerItem;
import com.sshakusora.shadowsandpetals.util.WoolUtils;
import com.sshakusora.shadowsandpetals.worldgen.SAPTreeGrowers;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;

public class BlockRegistry {

    public static final WoodSetList WOOD_SETS = new WoodSetList();

    public static final WoodSetList.WoodSet SAKURA_SET = WOOD_SETS.get(WoodSetList.Type.SAKURA);
    public static final WoodSetList.WoodSet MAPLE_SET = WOOD_SETS.get(WoodSetList.Type.MAPLE);
    public static final WoodSetList.WoodSet GINKGO_SET = WOOD_SETS.get(WoodSetList.Type.GINKGO);

    public static final DeferredBlock<SaplingBlock> AUTUMN_OAK_SAPLING = WoodSetList.treeSapling(
            "autumn_oak_sapling",
            SAPTreeGrowers.AUTUMN_OAK,
            "秋橡树树苗"
    );
    public static final DeferredBlock<LeavesBlock> AUTUMN_OAK_LEAVES = WoodSetList.treeLeaves(
            "autumn_oak_leaves",
            AUTUMN_OAK_SAPLING,
            "秋橡树树叶",
            () -> ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, ARGB.color(255, 176, 106, 45))
    );
    public static final DeferredBlock<HedgeBlock> AUTUMN_OAK_HEDGE = WoodSetList.treeHedge(
            "autumn_oak_hedge",
            AUTUMN_OAK_LEAVES,
            "秋橡树树篱"
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

    public static final DeferredBlock<Block> RAW_CONCRETE = SAPRegistries
            .block("raw_concrete")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "An unfinished architectural block with _connected textures_.",
                            "具有_连接纹理_的素面建筑方块。")
                    .behaviour(
                            "When placed beside itself:", "与同类方块相邻放置时:",
                            "Join into a _continuous concrete surface_.", "连接成_连续的混凝土表面_。"))
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, block) -> provider.cubeAllBlockWithItem(block.get()))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .connectedTexture(CTTextureType.OMNIDIRECTIONAL)
            .recipe((provider, block) -> {
                provider.shaped(RecipeCategory.BUILDING_BLOCKS, block.get(), 8)
                        .define('P', ItemTags.PLANKS)
                        .define('C', Tags.Items.CONCRETE_POWDERS)
                        .pattern("PPP")
                        .pattern("PCP")
                        .pattern("PPP")
                        .unlockedBy("has_concrete_powder", provider.hasTag(Tags.Items.CONCRETE_POWDERS))
                        .save(provider.output());
                provider.stonecutter(RecipeCategory.BUILDING_BLOCKS, block.get(), 1, Blocks.WHITE_CONCRETE);
            })
            .lang("zh_cn", "清水混凝土")
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
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A modular _hearth_ that burns fuel and leaves useful ash.",
                            "能够燃烧燃料并留下有用灰烬的组合式_围炉_。")
                    .behaviour(
                            "When fuel burns out:", "燃料耗尽时:",
                            "Leave behind ash that can be collected as _Bone Meal_.", "留下可收集为_骨粉_的灰烬。")
                    .action(
                            "Drop Fuel into the Basin:", "将燃料丢入炉膛:",
                            "_Load_ the hearth with fuel.", "为围炉_添加_燃料。")
                    .action(
                            "Use Flint and Steel or a Fire Charge:", "使用打火石或火焰弹:",
                            "_Ignite_ the loaded fuel.", "_点燃_已添加的燃料。")
                    .action(
                            "Right-click the Ash:", "右键点击灰烬:",
                            "Collect it as _Bone Meal_.", "将其收集为_骨粉_。"))
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, irori) -> provider.iroriBlock(irori.get()))
            .loot((provider, irori) -> provider.dropSelf(irori.get()))
            .recipe((provider, irori) -> provider.shaped(RecipeCategory.DECORATIONS, irori.get())
                    .define('L', ItemTags.LOGS)
                    .define('B', Items.STONE_BRICKS)
                    .define('G', Items.GRAVEL)
                    .pattern("LLL")
                    .pattern("BGB")
                    .pattern("BBB")
                    .unlockedBy(provider.hasName(Items.STONE_BRICKS), provider.hasItem(Items.STONE_BRICKS))
                    .save(provider.output()))
            .lang("zh_cn", "日式围炉")
            .register();

    public static final DeferredBlock<BedroomLampBlock> BEDROOM_LAMP = SAPRegistries
            .block("bedroom_lamp", BedroomLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(BedroomLampBlock.LIT) ? 15 : 0))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A warm decorative _bedside lamp_.",
                            "散发温暖光线的装饰性_卧室台灯_。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, lamp) -> provider.bedroomLampBlock(lamp.get()))
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .clientItem(ShadowsAndPetals.asResource("block/bedroom_lamp/off"))
            .lang("zh_cn", "卧室台灯")
            .register();

    public static final DeferredBlock<WallLampBlock> WALL_LAMP = SAPRegistries
            .block("wall_lamp", WallLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(WallLampBlock.LIT) ? 15 : 0))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A compact lamp mounted on a _sturdy wall_.",
                            "安装在_坚固墙面_上的小型灯具。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, lamp) -> provider.wallLampBlock(lamp.get()))
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .clientItem(ShadowsAndPetals.asResource("block/wall_lamp/off"))
            .lang("zh_cn", "壁灯")
            .register();

    public static final DeferredBlock<EmergencyLampBlock> EMERGENCY_LAMP = SAPRegistries
            .block("emergency_lamp", EmergencyLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(EmergencyLampBlock.LIT) ? 15 : 0))
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A rugged lamp that attaches to _any sturdy face_.",
                            "可安装在_任意坚固表面_上的耐用防爆灯。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, lamp) -> provider.emergencyLampBlock(lamp.get()))
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .clientItem(ShadowsAndPetals.asResource("block/emergency_lamp/off"))
            .lang("zh_cn", "防爆灯")
            .register();

    public static final DeferredBlock<DeskLampBlock> DESK_LAMP = SAPRegistries
            .block("desk_lamp", DeskLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(DeskLampBlock.LIT) ? 15 : 0))
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A focused lamp for a _sturdy tabletop_.",
                            "放置在_坚固台面_上的聚光台灯。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, lamp) -> provider.deskLampBlock(lamp.get()))
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .clientItem(ShadowsAndPetals.asResource("block/desk_lamp/off"))
            .lang("zh_cn", "台灯")
            .register();

    public static final DyedBlockList<RoofTileBlock> ROOF_TILES = new DyedBlockList<>(color -> SAPRegistries
            .block(color.getName() + "_roof_tile", RoofTileBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE_TILES)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabType.MAIN)
            .blockstate((provider, block) -> provider.horizontalFacingCubeAllBlockWithItem(
                    block.get(),
                    provider.modLoc("block/roof_tile/" + color.getName())
            ))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> {
                provider.shaped(RecipeCategory.BUILDING_BLOCKS, block.get(), 8)
                        .define('S', Items.STONE_BRICKS)
                        .define('D', color.getTag())
                        .pattern("SSS")
                        .pattern("SDS")
                        .pattern("SSS")
                        .unlockedBy(provider.hasName(Items.STONE_BRICKS), provider.hasItem(Items.STONE_BRICKS))
                        .save(provider.output());
            })
            .lang("zh_cn", DyedBlockList.zhName(color) + "瓦")
            .register());

    public static final WoodBlockList<VanityBlock> VANITIES = new WoodBlockList<>(woodType -> SAPRegistries
            .block(woodType.getName() + "_vanity", VanityBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(woodType.getPlanks())
                    .strength(2.5F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary("A decorative _vanity_ with a _usable drawer_.", "带有_抽屉_的装饰性梳妆台。")
                    .behaviour(
                            "When the drawer is right-clicked:", "右键点击抽屉时:",
                            "Open its _9-slot storage_ space.", "打开其_9 格储物空间_。")
                    .behaviour(
                            "When the front is obstructed:", "前方被阻挡时:",
                            "The drawer _cannot open_.", "抽屉_无法打开_。"))
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
            .register());

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
            .tooltipDescription(tooltip -> tooltip
                    .summary("A soft seat that can be _recolored_.", "一把可以_重新染色_的柔软座椅。")
                    .behaviour(
                            "When right-clicked;", "右键点击时:",
                            "_Sit_ on the chair.", "_坐_在椅子上。")
                    .behaviour(
                            "When landed on:", "落在上面时:",
                            "_Cushion_ the fall and bounce upward.", "_缓冲_坠落并向上弹起。")
                    .action(
                            "Right-click with Dye:", "手持染料右键点击:",
                            "_Recolor_ the chair.", "为椅子_重新染色_。"))
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

    public static final DeferredBlock<SamonBlock> SAMON = SAPRegistries
            .block("samon", SamonBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(0.7F)
                    .sound(SoundType.STONE))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .blockstate((provider, block) -> provider.samonBlock(block.get()))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .lang("zh_cn", "砂纹")
            .register();

    public static final DeferredBlock<ShishiOdoshiBlock> SHISHI_ODOSHI = SAPRegistries
            .block("shishi_odoshi", ShishiOdoshiBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.BAMBOO)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A bamboo water feature that fills, tips, and _knocks_.",
                            "蓄水后倾倒并发出_敲击声_的竹制添水。")
                    .behaviour(
                            "When supplied by a Pipe above:", "由上方竹管供液时:",
                            "Fill gradually, then tip and _strike_ in a repeating cycle.", "逐渐蓄满，随后倾倒并循环_敲击_。"))
            .creativeTab(CreativeTabType.NATURE)
            .blockstate((provider, block) -> provider.shishiOdoshiBlock(block.get()))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> provider.shaped(RecipeCategory.DECORATIONS, block.get())
                    .define('B', Items.BAMBOO)
                    .define('S', Items.COBBLESTONE)
                    .pattern("BBB")
                    .pattern("SSS")
                    .unlockedBy(provider.hasName(Items.BAMBOO), provider.hasItem(Items.BAMBOO))
                    .save(provider.output()))
            .lang("zh_cn", "添水")
            .register();

    public static final DeferredBlock<ShishiOdoshiPipeBlock> SHISHI_ODOSHI_PIPE = SAPRegistries
            .block("shishi_odoshi_pipe", ShishiOdoshiPipeBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(1.0F, 1.5F)
                    .sound(SoundType.BAMBOO)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A bamboo _spout_ that supplies a Shishi-Odoshi.",
                            "为添水供液的竹制_出水管_。")
                    .behaviour(
                            "When attached to a compatible fluid source:", "连接兼容的流体源时:",
                            "Feed fluid into a _Shishi-Odoshi directly below_.", "向_正下方的添水_持续供液。"))
            .creativeTab(CreativeTabType.NATURE)
            .blockstate((provider, block) -> provider.shishiOdoshiPipeBlock(block.get()))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> provider.shaped(RecipeCategory.DECORATIONS, block.get())
                    .define('B', Items.BAMBOO)
                    .pattern("B")
                    .pattern("B")
                    .unlockedBy(provider.hasName(Items.BAMBOO), provider.hasItem(Items.BAMBOO))
                    .save(provider.output()))
            .lang("zh_cn", "添水竹管")
            .register();

    public static final DeferredBlock<RockeryBlock> ROCKERY_1x1x1 = registerRockery(1, 1, 1);
    public static final DeferredBlock<RockeryBlock> ROCKERY_1x1x2 = registerRockery(1, 1, 2);
    public static final DeferredBlock<RockeryBlock> ROCKERY_1x2x1 = registerRockery(1, 2, 1);
    public static final DeferredBlock<RockeryBlock> ROCKERY_1x2x2 = registerRockery(1, 2, 2);
    public static final DeferredBlock<RockeryBlock> ROCKERY_1x3x1 = registerRockery(1, 3, 1);

    private static DeferredBlock<RockeryBlock> registerRockery(int w, int h, int d) {
        RockeryDimensions dims = new RockeryDimensions(w, h, d);
        DeferredBlock<RockeryBlock> result = SAPRegistries
                .block("rockery_" + w + "_" + h + "_" + d,
                        props -> new RockeryBlock(dims, props))
                .properties(p -> BlockBehaviour.Properties.of()
                        .strength(1.5F, 6.0F)
                        .sound(SoundType.STONE)
                        .noOcclusion()
                        .requiresCorrectToolForDrops())
                .withItem()
                .tooltipComponent(
                        (rockery, stack) -> new RockeryTooltipComponent(rockery, dims),
                        100)
                .creativeTab(CreativeTabType.NATURE)
                .tags(BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate((provider, block) -> provider.rockeryBlock(block.get(), dims))
                .clientItem(ShadowsAndPetals.asResource("block/rock/1_1_1"))
                .loot((provider, block) -> provider.addTable(block.get(), provider.noDropTable()))
                .lang("en_us", "rockery")
                .lang("zh_cn", "石山")
                .register();

        HammerItem.registerRockery(result, dims);
        return result;
    }

    public static void init() {}
}
