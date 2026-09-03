package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.builder.RegBlockBuilder;
import com.sshakusora.shadowsandpetals.block.*;
import com.sshakusora.shadowsandpetals.block.agriculture.OrangeTreeBlock;
import com.sshakusora.shadowsandpetals.block.decoration.*;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.block.nature.LeavesVerticalSlabBlock;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.block.nature.SandExcavationBlock;
import com.sshakusora.shadowsandpetals.client.ct.CTTextureType;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryTooltipComponent;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeFactory;
import com.sshakusora.shadowsandpetals.data.model.generator.*;
import com.sshakusora.shadowsandpetals.item.RecessedLampBlockItem;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeTooltipModifier;
import com.sshakusora.shadowsandpetals.item.hammer.HammerItem;
import com.sshakusora.shadowsandpetals.recipe.WindChimeDyeRecipe;
import com.sshakusora.shadowsandpetals.util.WoolUtils;
import com.sshakusora.shadowsandpetals.worldgen.SAPTreeGrowers;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

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
            () -> ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, ARGB.color(255, 176, 106, 45)),
            MapColor.COLOR_ORANGE
    );
    public static final DeferredBlock<SlabBlock> AUTUMN_OAK_LEAVES_SLAB = WoodSetList.treeLeavesSlab(
            "autumn_oak_leaves_slab",
            AUTUMN_OAK_LEAVES,
            "秋橡树树叶台阶",
            MapColor.COLOR_ORANGE
    );
    public static final DeferredBlock<LeavesVerticalSlabBlock> AUTUMN_OAK_LEAVES_VERTICAL_SLAB = WoodSetList.treeLeavesVerticalSlab(
            "autumn_oak_leaves_vertical_slab",
            AUTUMN_OAK_LEAVES_SLAB,
            AUTUMN_OAK_LEAVES,
            "竖直秋橡树树叶台阶",
            MapColor.COLOR_ORANGE
    );
    public static final DeferredBlock<StairBlock> AUTUMN_OAK_LEAVES_STAIRS = WoodSetList.treeLeavesStairs(
            "autumn_oak_leaves_stairs",
            AUTUMN_OAK_LEAVES,
            "秋橡树树叶楼梯",
            MapColor.COLOR_ORANGE
    );
    public static final DeferredBlock<HedgeBlock> AUTUMN_OAK_HEDGE = WoodSetList.treeHedge(
            "autumn_oak_hedge",
            AUTUMN_OAK_LEAVES,
            "秋橡树树篱",
            MapColor.COLOR_ORANGE
    );

    public static final DeferredBlock<DropExperienceBlock> BAUXITE_ORE = SAPRegistries
            .block("bauxite_ore", props -> new DropExperienceBlock(UniformInt.of(1, 3), props))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> (context, generator) -> StandardBlockModels.cubeAll(
                    context, generator, generator.modLoc("block/bauxite_ore/bauxite_ore")))
            .loot((provider, ore) -> provider.dropOre(ore.get(), ItemRegistry.RAW_BAUXITE.get()))
            .lang(DatagenLangRegistry.ZH_CN, "矾土矿石")
            .register();

    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = SAPRegistries
            .block("deepslate_bauxite_ore", props -> new DropExperienceBlock(UniformInt.of(1, 3), props))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> (context, generator) -> StandardBlockModels.cubeAll(
                    context, generator, generator.modLoc("block/bauxite_ore/deepslate_bauxite_ore")))
            .loot((provider, ore) -> provider.dropOre(ore.get(), ItemRegistry.RAW_BAUXITE.get()))
            .lang(DatagenLangRegistry.ZH_CN, "深层矾土矿石")
            .register();

    public static final DeferredBlock<Block> RAW_BAUXITE_BLOCK = SAPRegistries
            .block("raw_bauxite_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.DIRT)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> StandardBlockModels::cubeAll)
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> DatagenRecipeFactory.storageBlock(provider, block, ItemRegistry.RAW_BAUXITE.get(), "raw_bauxite_from_block"))
            .lang(DatagenLangRegistry.ZH_CN, "粗矾土块")
            .register();

    public static final DeferredBlock<Block> ALUMINUM_BLOCK = SAPRegistries
            .block("aluminum_block")
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> StandardBlockModels::cubeAll)
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> DatagenRecipeFactory.storageBlock(provider, block, ItemRegistry.ALUMINUM_INGOT.get(), "aluminum_ingot_from_block"))
            .lang(DatagenLangRegistry.ZH_CN, "铝块")
            .register();

    public static final DeferredBlock<WindChimeBlock> WIND_CHIME = SAPRegistries
            .block("wind_chime", WindChimeBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .sound(SoundType.GLASS)
                    .mapColor(MapColor.NONE)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTagRegistry.WOOD_POST_HANGING_CONNECTIONS)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A hanging ornament with independently dyeable _ribbon_ and _vane_.",
                            "悬绳与短册可_分别染色_的悬挂饰物。")
                    .behaviour(
                            "When crafted with dye:", "与染料合成时：",
                            "Change the _ribbon_ or _vane_ color.", "改变_悬绳_或_短册_颜色。")
                    .behaviour(
                            "Over time:", "经过一段时间后：",
                            "Play soft ambient chimes.", "发出轻柔的环境风铃声。")
                    .action(
                            "Right-click", "右键点击",
                            "_Ring_ the wind chime.", "_敲响_风铃。"))
            .tooltipModifier(new WindChimeTooltipModifier())
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> WindChimeModels::block)
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, windChime) -> {
                provider.shaped(RecipeCategory.DECORATIONS, windChime.get())
                        .define('S', Items.STRING)
                        .define('G', Items.GLASS_PANE)
                        .define('A', Items.AMETHYST_SHARD)
                        .define('P', Items.PAPER)
                        .pattern(" S ")
                        .pattern("GAG")
                        .pattern(" P ")
                        .unlockedBy(provider.hasName(Items.AMETHYST_SHARD), provider.hasItem(Items.AMETHYST_SHARD))
                        .save(provider.output());

                provider.output().accept(
                        ResourceKey.create(Registries.RECIPE, provider.id("wind_chime_ribbon_dyeing")),
                        new WindChimeDyeRecipe(WindChimeDyeRecipe.Target.RIBBON),
                        null
                );
                provider.output().accept(
                        ResourceKey.create(Registries.RECIPE, provider.id("wind_chime_vane_dyeing")),
                        new WindChimeDyeRecipe(WindChimeDyeRecipe.Target.VANE),
                        null
                );
                provider.output().accept(
                        ResourceKey.create(Registries.RECIPE, provider.id("wind_chime_dual_dyeing")),
                        new WindChimeDyeRecipe(WindChimeDyeRecipe.Target.BOTH),
                        null
                );
            })
            .itemModel(() -> WindChimeModels::item)
            .customClientItem(ShadowsAndPetals.asResource("wind_chime"))
            .lang(DatagenLangRegistry.ZH_CN, "风铃")
            .register();

    public static final DeferredBlock<CopperTeapotBlock> COPPER_TEAPOT = SAPRegistries
            .block("copper_teapot", CopperTeapotBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTagRegistry.REQUIRES_IRORI_GRILL)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A _decorative copper teapot_ that can hold fluids.",
                            "可盛装液体的_铜制装饰茶壶_。")
                    .behaviour(
                            "Right-click to open:", "右键打开：",
                            "Open the teapot screen.", "打开茶壶界面。")
                    .behaviour(
                            "When placed on an irori:", "放置在围炉上时：",
                            "Lift to sit on the _irori grate_.", "抬高并摆放在_围炉炉架_上。"))
            .blockstate(() -> DecorationBlockModels::copperTeapot)
            .recipe((provider, block) -> {
                provider.shaped(RecipeCategory.DECORATIONS, block.get())
                        .define('C', Items.COPPER_INGOT)
                        .pattern("C C")
                        .pattern("C C")
                        .pattern(" C ")
                        .unlockedBy("has_copper_ingot", provider.hasItem(Items.COPPER_INGOT))
                        .save(provider.output());
            })
            .clientItem(ShadowsAndPetals.asResource("item/teapot/copper"))
            .lang(DatagenLangRegistry.ZH_CN, "铜茶壶")
            .register();

    public static final DeferredBlock<RawConcreteBlock> RAW_CONCRETE = SAPRegistries
            .block("raw_concrete", RawConcreteBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.CLAY)
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
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> (context, generator) -> StandardBlockModels.cubeAll(
                    context,
                    generator,
                    ShadowsAndPetals.asResource("block/raw_concrete/base")))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .connectedTextures(
                    ShadowsAndPetals.asResource("block/raw_concrete/base"),
                    List.of(
                            ShadowsAndPetals.asResource("block/raw_concrete/connected_bleed"),
                            ShadowsAndPetals.asResource("block/raw_concrete/connected_hole_bleed"),
                            ShadowsAndPetals.asResource("block/raw_concrete/connected_dense_hole_bleed")),
                    RawConcreteBlock::selectTextureIndex,
                    CTTextureType.OMNIDIRECTIONAL, 1)
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
            .lang(DatagenLangRegistry.ZH_CN, "清水混凝土")
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
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::ingotPile)
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, ItemRegistry.ALUMINUM_INGOT.get(), "aluminum_ingot_from_pile"))
            .lang(DatagenLangRegistry.ZH_CN, "铝锭堆")
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
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::ingotPile)
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.IRON_INGOT, "iron_ingot_from_pile"))
            .lang(DatagenLangRegistry.ZH_CN, "铁锭堆")
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
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::ingotPile)
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.COPPER_INGOT, "copper_ingot_from_pile"))
            .lang(DatagenLangRegistry.ZH_CN, "铜锭堆")
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
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::ingotPile)
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.GOLD_INGOT, "gold_ingot_from_pile"))
            .lang(DatagenLangRegistry.ZH_CN, "金锭堆")
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
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::ingotPile)
            .loot((provider, pile) -> provider.dropSlab(pile.get()))
            .recipe((provider, pile) -> DatagenRecipeFactory.ingotPile(provider, pile, Items.NETHERITE_INGOT, "netherite_ingot_from_pile"))
            .lang(DatagenLangRegistry.ZH_CN, "下界合金锭堆")
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
                            "A modular _hearth_ for burning fuel, cooking food, and keeping watch.",
                            "能够燃烧燃料、烹饪食物并守护周围的组合式_围炉_。")
                    .behaviour(
                            "While burning:", "燃烧时：",
                            "Cook campfire and smoking recipes, repel nearby _Phantoms_, and suppress their spawning.",
                            "烹饪篝火与烟熏配方，驱散附近的_幻翼_并抑制其生成。")
                    .behaviour(
                            "When fuel burns out:", "燃料耗尽时：",
                            "Leave behind ash that can be collected as _Bone Meal_.", "留下可收集为_骨粉_的灰烬。")
                    .action(
                            "Drop Fuel into the Basin:", "将燃料丢入炉膛：",
                            "_Load_ the hearth with fuel.", "为围炉_添加_燃料。")
                    .action(
                            "Use Flint and Steel or a Fire Charge:", "使用打火石或火焰弹：",
                            "_Ignite_ the loaded fuel.", "_点燃_已添加的燃料。")
                    .action(
                            "Right-click the Center with Cookable Food:", "手持可烹饪食物右键中心：",
                            "Place one item on the _grill_.", "将一份食物放上_炉架_。")
                    .action(
                            "Empty-hand Right-click the Food:", "空手右键炉架上的食物：",
                            "Take it from the _grill_.", "从_炉架_上取回食物。")
                    .action(
                            "Right-click the Ash:", "右键点击灰烬：",
                            "Collect it as _Bone Meal_.", "将其收集为_骨粉_。")
                    .action(
                            "Shift Right-click:", "Shift+右键：",
                            "Open the master hearth _menu_.", "打开主围炉_界面_。"))
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::irori)
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
            .lang(DatagenLangRegistry.ZH_CN, "日式围炉")
            .register();

    public static final DeferredBlock<BedroomLampBlock> BEDROOM_LAMP = SAPRegistries
            .block("bedroom_lamp", BedroomLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(BedroomLampBlock.LIT) ? 10 : 0))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A warm decorative _bedside lamp_.",
                            "散发温暖光线的装饰性_卧室台灯_。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::bedroomLamp)
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .recipe((provider, lamp) -> provider.shapeless(RecipeCategory.DECORATIONS, lamp.get(), 2)
                    .requires(provider.ingredient(Tags.Items.DUSTS_REDSTONE), 2)
                    .requires(Items.GLOWSTONE)
                    .requires(Tags.Items.INGOTS_IRON)
                    .unlockedBy(provider.hasName(Items.GLOWSTONE), provider.hasItem(Items.GLOWSTONE))
                    .save(provider.output()))
            .clientItem(ShadowsAndPetals.asResource("block/bedroom_lamp/off"))
            .lang(DatagenLangRegistry.ZH_CN, "卧室台灯")
            .register();

    public static final DeferredBlock<WallLampBlock> WALL_LAMP = SAPRegistries
            .block("wall_lamp", WallLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(WallLampBlock.LIT) ? 10 : 0))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A compact lamp that mounts to a _sturdy wall face_.",
                            "安装在_坚固墙面_上的小型灯具。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::wallLamp)
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .recipe((provider, lamp) -> provider.shapeless(RecipeCategory.DECORATIONS, lamp.get(), 2)
                    .requires(provider.ingredient(Tags.Items.DUSTS_REDSTONE), 2)
                    .requires(Items.GLOWSTONE)
                    .requires(ItemRegistry.ALUMINUM_INGOT.get())
                    .unlockedBy(provider.hasName(Items.GLOWSTONE), provider.hasItem(Items.GLOWSTONE))
                    .save(provider.output()))
            .clientItem(ShadowsAndPetals.asResource("block/wall_lamp/off"))
            .lang(DatagenLangRegistry.ZH_CN, "壁灯")
            .register();

    public static final DeferredBlock<EmergencyLampBlock> EMERGENCY_LAMP = SAPRegistries
            .block("emergency_lamp", EmergencyLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(EmergencyLampBlock.LIT) ? 10 : 0))
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A rugged lamp that attaches to _any sturdy face_.",
                            "可安装在_任意坚固面_上的耐用防爆灯。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::emergencyLamp)
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .recipe((provider, lamp) -> {
                provider.shapeless(RecipeCategory.DECORATIONS, lamp.get(), 2)
                        .requires(provider.ingredient(Tags.Items.DUSTS_REDSTONE), 2)
                        .requires(Items.WHITE_STAINED_GLASS)
                        .requires(Items.GLOWSTONE)
                        .requires(Tags.Items.INGOTS_IRON)
                        .unlockedBy(provider.hasName(Items.GLOWSTONE), provider.hasItem(Items.GLOWSTONE))
                        .save(provider.output());

                provider.shapeless(RecipeCategory.DECORATIONS, lamp.get(), 2)
                        .requires(provider.ingredient(Tags.Items.DUSTS_REDSTONE), 2)
                        .requires(Items.WHITE_STAINED_GLASS)
                        .requires(Items.GLOWSTONE)
                        .requires(ItemRegistry.ALUMINUM_INGOT.get())
                        .unlockedBy(provider.hasName(Items.GLOWSTONE), provider.hasItem(Items.GLOWSTONE))
                        .save(provider.output(), provider.id("emergency_lamp_from_aluminum").toString());
            })
            .clientItem(ShadowsAndPetals.asResource("block/emergency_lamp/off"))
            .lang(DatagenLangRegistry.ZH_CN, "防爆灯")
            .register();

    public static final DeferredBlock<RecessedLampBlock> RECESSED_LAMP = SAPRegistries
            .block("recessed_lamp", RecessedLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(RecessedLampBlock.LIT) ? 10 : 0))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withCustomItem(RecessedLampBlockItem::new)
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A compact light recessed into a _floor or ceiling_.",
                            "嵌入_地面或天花板_的小型灯具。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::recessedLamp)
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .recipe((provider, lamp) -> {
                provider.shaped(RecipeCategory.DECORATIONS, lamp.get(), 4)
                        .define('I', Tags.Items.INGOTS_IRON)
                        .define('L', Items.GLOWSTONE)
                        .define('G', Tags.Items.GLASS_PANES_COLORLESS)
                        .pattern("GIG")
                        .pattern("ILI")
                        .pattern("GIG")
                        .unlockedBy("has_iron_ingot", provider.hasTag(Tags.Items.INGOTS_IRON))
                        .save(provider.output());

                provider.shaped(RecipeCategory.DECORATIONS, lamp.get(), 4)
                        .define('I', ItemRegistry.ALUMINUM_INGOT.get())
                        .define('L', Items.GLOWSTONE)
                        .define('G', Tags.Items.GLASS_PANES_COLORLESS)
                        .pattern("GIG")
                        .pattern("ILI")
                        .pattern("GIG")
                        .unlockedBy("has_aluminum_ingot", provider.hasItem(ItemRegistry.ALUMINUM_INGOT.get()))
                        .save(provider.output(), provider.id("recessed_lamp_from_aluminum").toString());
            })
            .clientItem(ShadowsAndPetals.asResource("block/recessed_lamp/up_off"))
            .lang(DatagenLangRegistry.ZH_CN, "嵌灯")
            .register();

    public static final DeferredBlock<RecessedLampCompositeBlock> RECESSED_LAMP_COMPOSITE = SAPRegistries
            .block("recessed_lamp_composite", RecessedLampCompositeBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.METAL)
                    .dynamicShape()
                    .lightLevel(state -> state.getValue(RecessedLampBlock.LIT) ? 10 : 0))
            .blockstate(() -> DecorationBlockModels::recessedLampComposite)
            .loot((provider, block) -> provider.addTable(block.get(), provider.noDropTable()))
            .lang(DatagenLangRegistry.DEFAULT_LOCALE, "Recessed Lamp")
            .lang(DatagenLangRegistry.ZH_CN, "嵌灯")
            .register();

    public static final DeferredBlock<DeskLampBlock> DESK_LAMP = SAPRegistries
            .block("desk_lamp", DeskLampBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(DeskLampBlock.LIT) ? 10 : 0))
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A focused lamp for a _sturdy tabletop_.",
                            "放置在_坚固台面_上的聚光台灯。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "Toggle the light _on or off_.", "切换灯的_开关状态_。"))
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::deskLamp)
            .loot((provider, lamp) -> provider.dropSelf(lamp.get()))
            .recipe((provider, lamp) -> provider.shapeless(RecipeCategory.DECORATIONS, lamp.get(), 2)
                    .requires(provider.ingredient(Tags.Items.DUSTS_REDSTONE), 2)
                    .requires(Items.GLOWSTONE)
                    .requires(ItemRegistry.ALUMINUM_INGOT.get(), 2)
                    .unlockedBy(provider.hasName(Items.GLOWSTONE), provider.hasItem(Items.GLOWSTONE))
                    .save(provider.output()))
            .clientItem(ShadowsAndPetals.asResource("block/desk_lamp/off"))
            .lang(DatagenLangRegistry.ZH_CN, "台灯")
            .register();

    public static final DeferredBlock<BonsaiBlock> BONSAI = SAPRegistries
            .block("bonsai", BonsaiBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.STONE)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A _decorative pot_ for growing trees.",
                            "可栽种树木的_装饰花盆_。")
                    .behaviour(
                            "Use a Sapling on an empty pot:", "对空花盆使用树苗：",
                            "Create a miniature tree matching the sapling's _wood and foliage_.",
                            "栽种具有对应_树干与树叶_的微型盆景。")
                    .behaviour(
                            "Use a Dead Bush on an empty pot:", "对空花盆使用枯死的灌木：",
                            "Create a leafless _dead-tree bonsai_.",
                            "栽种一株无叶的_枯木盆景_。")
                    .action(
                            "Empty-hand Right-click:", "已种植时空手右键：",
                            "_Cycle_ through four bonsai shapes.",
                            "切换四种_盆景造型_。")
                    .action(
                            "Sneak + Empty-hand Right-click:", "潜行时空手右键：",
                            "Rotate the pot by _22.5°_.",
                            "将花盆旋转_22.5°_。")
                    .action(
                            "Use Shears on a living bonsai:", "对活盆景使用剪刀：",
                            "Remove its leaves and turn it into a _dead tree_.",
                            "剪去树叶，使其变为_枯木盆景_。")
                    .action(
                            "Use Shears on a dead bonsai:", "对枯木盆景使用剪刀：",
                            "Recover the planted item and return to an _empty pot_.",
                            "取回种植物，并恢复为_空花盆_。"))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> provider.shaped(RecipeCategory.DECORATIONS, block.get(), 3)
                    .define('A', ItemRegistry.ALUMINUM_INGOT.get())
                    .define('D', ItemTags.DIRT)
                    .pattern("ADA")
                    .pattern(" A ")
                    .unlockedBy("has_aluminum_ingot", provider.hasItem(ItemRegistry.ALUMINUM_INGOT.get()))
                    .save(provider.output()))
            .blockstate(() -> BonsaiBlockModels::block)
            .lang(DatagenLangRegistry.ZH_CN, "盆栽")
            .register();

    public static final DyedBlockList<RoofTileBlock> ROOF_TILES = new DyedBlockList<>(color -> SAPRegistries
            .block(color.getName() + "_roof_tile", RoofTileBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE_TILES)
                    .mapColor(color)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> (context, generator) -> RoofTileModels.base(
                    context,
                    generator,
                    generator.modLoc("block/roof_tile/" + color.getName())
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
            .lang(DatagenLangRegistry.ZH_CN, DyedBlockList.zhName(color) + "瓦")
            .register());

    public static final DyedBlockList<RoofTileSlabBlock> ROOF_TILE_SLABS = new DyedBlockList<>(color -> SAPRegistries
            .block(color.getName() + "_roof_tile_slab", RoofTileSlabBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE_TILES)
                    .mapColor(color)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.SLABS)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> (context, generator) -> RoofTileModels.shapes(context, generator, color))
            .loot((provider, block) -> provider.dropSlab(block.get()))
            .recipe((provider, block) -> {
                provider.slabFromBase(RecipeCategory.BUILDING_BLOCKS, block.get(), ROOF_TILES.get(color).get());
                provider.stonecutter(RecipeCategory.BUILDING_BLOCKS, block.get(), 2, ROOF_TILES.get(color).get());
            })
            .lang(DatagenLangRegistry.ZH_CN, DyedBlockList.zhName(color) + "瓦台阶")
            .register());

    public static final DyedBlockList<RoofTileVerticalSlabBlock> ROOF_TILE_VERTICAL_SLABS = new DyedBlockList<>(color -> SAPRegistries
            .block(color.getName() + "_roof_tile_vertical_slab", RoofTileVerticalSlabBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE_TILES)
                    .mapColor(color)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .clientItem(block -> ShadowsAndPetals.asResource("block/" + block.getId().getPath()))
            .loot((provider, block) -> provider.dropSlab(block.get()))
            .recipe((provider, block) -> {
                var slab = ROOF_TILE_SLABS.get(color).get();
                provider.shaped(RecipeCategory.BUILDING_BLOCKS, block.get(), 3)
                        .define('S', slab)
                        .pattern("S")
                        .pattern("S")
                        .pattern("S")
                        .unlockedBy(provider.hasName(slab), provider.hasItem(slab))
                        .save(provider.output());
                provider.shapeless(RecipeCategory.BUILDING_BLOCKS, slab)
                        .requires(block.get())
                        .unlockedBy(provider.hasName(block.get()), provider.hasItem(block.get()))
                        .save(provider.output(), provider.id(color.getName() + "_roof_tile_vertical_slab_revert").toString());
                provider.stonecutter(RecipeCategory.BUILDING_BLOCKS, block.get(), 2, ROOF_TILES.get(color).get());
            })
            .lang(DatagenLangRegistry.ZH_CN, "竖直" + DyedBlockList.zhName(color) + "瓦台阶")
            .register());

    public static final DyedBlockList<StairBlock> ROOF_TILE_STAIRS = new DyedBlockList<>(color -> SAPRegistries
            .block(color.getName() + "_roof_tile_stairs", properties -> new StairBlock(ROOF_TILES.get(color).get().defaultBlockState(), properties))
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE_TILES)
                    .mapColor(color)
                    .requiresCorrectToolForDrops())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.STAIRS)
            .withItem()
            .creativeTab(CreativeTabKey.MAIN)
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> {
                provider.stairsFromBase(block.get(), ROOF_TILES.get(color).get());
                provider.stonecutter(RecipeCategory.BUILDING_BLOCKS, block.get(), 1, ROOF_TILES.get(color).get());
            })
            .lang(DatagenLangRegistry.ZH_CN, DyedBlockList.zhName(color) + "瓦楼梯")
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
            .creativeTab(CreativeTabKey.MAIN)
            .blockstate(() -> DecorationBlockModels::vanity)
            .clientItem(ShadowsAndPetals.asResource("item/vanity/" + woodType.getName()))
            .recipe((provider, vanity) -> provider.shaped(RecipeCategory.DECORATIONS, vanity.get())
                    .define('S', woodType.getSlab())
                    .define('G', Items.GLASS_PANE)
                    .pattern("S  ")
                    .pattern("G  ")
                    .pattern("SSS")
                    .unlockedBy(provider.hasName(woodType.getPlanks()), provider.hasItem(woodType.getPlanks()))
                    .save(provider.output()))
            .lang(DatagenLangRegistry.ZH_CN, woodType.getZhName() + "梳妆台")
            .register());

    public static final DeferredBlock<WindowPaneBlock> RED_LACQUERED_WINDOW_PANE = SAPRegistries
            .block("red_lacquered_window_pane", WindowPaneBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .sound(SoundType.WOOD)
                    .mapColor(DyeColor.RED)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_WINDOW_PANES)
            .blockstate(() -> WindowPaneModels::redLacquered)
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .lang(DatagenLangRegistry.ZH_CN, "红漆窗格")
            .register();

    public static final WoodBlockList<WindowPaneBlock> WINDOW_PANES = new WoodBlockList<>(woodType ->
            registerWindowPane(
                    woodType.getName() + "_window_pane",
                    woodType,
                    woodType.getName(),
                    woodType.getZhName() + "窗格"
            ));

    public static final DeferredBlock<WoodPillarBlock> RED_LACQUERED_WOOD_PILLAR = SAPRegistries
            .block("red_lacquered_wood_pillar", WoodPillarBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .mapColor(DyeColor.RED))
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_STRIPPED_PILLARS)
            .blockstate(() -> (context, generator) -> AxisAlignedPillarBlockModels.withItem(
                    context,
                    generator,
                    ShadowsAndPetals.asResource("block/wood_pillar/red_lacquered_wood_pillar")
            ))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> provider.shapeless(RecipeCategory.DECORATIONS, block.get())
                    .requires(provider.ingredient(ItemTagRegistry.STRIPPED_WOOD_PILLARS))
                    .requires(Items.RED_DYE)
                    .unlockedBy(
                            provider.hasName(ItemTagRegistry.STRIPPED_WOOD_PILLARS),
                            provider.hasTag(ItemTagRegistry.STRIPPED_WOOD_PILLARS)
                    )
                    .save(provider.output()))
            .lang(DatagenLangRegistry.ZH_CN, "红漆木圆柱")
            .register();

    public static final WoodBlockList<WoodPillarBlock> STRIPPED_WOOD_PILLARS = new WoodBlockList<>(woodType -> SAPRegistries
            .block("stripped_" + woodType.getName() + "_wood_pillar", WoodPillarBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(woodType.getStrippedLog())
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD))
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_STRIPPED_PILLARS)
            .blockstate(() -> (context, generator) -> WoodPillarBlockModels.strippedWoodPillar(
                    context,
                    generator,
                    woodType
            ))
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .lang(DatagenLangRegistry.ZH_CN, "去皮" + woodType.getZhName() + "木圆柱")
            .register());

//    public static final WoodBlockList<ModularDeskBlock> MODULAR_DESKS = new WoodBlockList<>(woodType -> SAPRegistries.
//            block(woodType.getName() + "_modular_desk", ModularDeskBlock::new)
//            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
//                    .strength(2.0F, 3.0F)
//                    .sound(SoundType.WOOD)
//                    .noOcclusion())
//            .withItem()
//            .creativeTab(CreativeTabType.MAIN)
//            .lang(DatagenLangRegistry.ZH_CN, woodType.getZhName() + "书桌")
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
//            .lang(DatagenLangRegistry.ZH_CN, woodType.getZhName() + "咖啡桌")
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
//            .lang(DatagenLangRegistry.ZH_CN, woodType.getZhName() + "餐椅")
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
                    .mapColor(color)
                    .noOcclusion())
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary("A soft seat that can be _recolored_.", "一把可以_重新染色_的柔软座椅。")
                    .behaviour(
                            "When right-clicked:", "右键点击时:",
                            "_Sit_ on the chair.", "_坐_在椅子上。")
                    .behaviour(
                            "When landed on:", "落在上面时:",
                            "_Cushion_ the fall and bounce upward.", "_缓冲_坠落并向上弹起。")
                    .action(
                            "Right-click with Dye:", "手持染料右键点击:",
                            "_Recolor_ the chair.", "为椅子_重新染色_。"))
            .creativeTab(CreativeTabKey.MAIN)
            .lang(DatagenLangRegistry.ZH_CN, DyedBlockList.zhName(color) + "咖啡椅")
            .blockstate(() -> (context, generator) -> StandardBlockModels.simpleBlockWithItem(
                    context,
                    generator,
                    generator.modLoc("block/cafe_chair/" + color.getName())
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
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.SAND))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .withItem()
            .blockstate(() -> DecorationBlockModels::samon)
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .lang(DatagenLangRegistry.ZH_CN, "砂纹")
            .register();

    public static final DeferredBlock<SandExcavationBlock> SAND_EXCAVATION = SAPRegistries
            .block("sand_excavation", SandExcavationBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)
                    .strength(0.5F)
                    .sound(SoundType.SAND))
            .tags(BlockTags.MINEABLE_WITH_SHOVEL)
            .blockstate(() -> NatureBlockModels::sandExcavation)
            .loot((provider, block) -> provider.addTable(
                    block.get(),
                    LootTable.lootTable().withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .add(LootItem.lootTableItem(Items.SAND)))
            ))
            .lang(DatagenLangRegistry.DEFAULT_LOCALE, "Sand Excavation")
            .lang(DatagenLangRegistry.ZH_CN, "挖掘中的沙子")
            .register();

    public static final DeferredBlock<ShishiOdoshiBlock> SHISHI_ODOSHI = SAPRegistries
            .block("shishi_odoshi", ShishiOdoshiBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.BAMBOO)
                    .mapColor(MapColor.STONE)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A bamboo water feature that fills from above, tips, and _knocks_.",
                            "从上方蓄液、倾倒并发出_敲击声_的竹制添水。")
                    .behaviour(
                            "When supplied by a Pipe above:", "由上方竹管供液时:",
                            "Fill with the pipe's fluid, then pour and _strike_ in a repeating cycle.", "蓄入竹管提供的流体，随后倾倒并循环_敲击_。"))
            .creativeTab(CreativeTabKey.NATURE)
            .blockstate(() -> DecorationBlockModels::shishiOdoshi)
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> provider.shaped(RecipeCategory.DECORATIONS, block.get())
                    .define('B', Items.BAMBOO)
                    .define('S', Items.COBBLESTONE)
                    .pattern("BBB")
                    .pattern("SSS")
                    .unlockedBy(provider.hasName(Items.BAMBOO), provider.hasItem(Items.BAMBOO))
                    .save(provider.output()))
            .lang(DatagenLangRegistry.ZH_CN, "添水")
            .register();

    public static final DeferredBlock<ShishiOdoshiPipeBlock> SHISHI_ODOSHI_PIPE = SAPRegistries
            .block("shishi_odoshi_pipe", ShishiOdoshiPipeBlock::new)
            .properties(properties -> BlockBehaviour.Properties.of()
                    .strength(1.0F, 1.5F)
                    .sound(SoundType.BAMBOO)
                    .mapColor(MapColor.PLANT)
                    .noOcclusion())
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .withItem()
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A bamboo _spout_ that pours fluid from a source behind it.",
                            "从背后流体源引出液体的竹制_出水管_。")
                    .behaviour(
                            "When attached to a compatible fluid source:", "连接兼容的流体源时:",
                            "Feed fluid into a _Shishi-Odoshi directly below_.", "向_正下方的添水_持续供液。"))
            .creativeTab(CreativeTabKey.NATURE)
            .blockstate(() -> DecorationBlockModels::shishiOdoshiPipe)
            .loot((provider, block) -> provider.dropSelf(block.get()))
            .recipe((provider, block) -> provider.shaped(RecipeCategory.DECORATIONS, block.get())
                    .define('B', Items.BAMBOO)
                    .pattern("B")
                    .pattern("B")
                    .unlockedBy(provider.hasName(Items.BAMBOO), provider.hasItem(Items.BAMBOO))
                    .save(provider.output()))
            .lang(DatagenLangRegistry.ZH_CN, "添水竹管")
            .register();

    public static final DyedBlockList<CurtainBlock> CURTAINS = new DyedBlockList<>(color -> {
        RegBlockBuilder<CurtainBlock> builder = SAPRegistries
                .block(color.getName() + "_curtain", CurtainBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)
                        .strength(1.0F)
                        .sound(SoundType.WOOL)
                        .mapColor(color)
                        .noOcclusion())
                .tags(BlockTags.WOOL, BlockTags.MINEABLE_WITH_AXE)
                .withItem()
                .creativeTab(CreativeTabKey.MAIN)
                .blockstate(() -> CurtainModels::block);
        // Deprecated: the 'shadowsandpetals:curtain' id only existed in 26.1.2 test worlds,
        // and 26.1.2 legacy content is deprecated. Restore the stateAliasProperties migration
        // below if the old id ever needs to survive in saved worlds.
        // builder.stateAliasProperties("curtain", legacy -> legacy
        //                 .property(CurtainBlock.FACING, Direction.NORTH)
        //                 .property(CurtainBlock.HALF, DoubleBlockHalf.LOWER)
        //                 .property(CurtainBlock.SIDE, CurtainBlock.Side.LEFT)
        //                 .property(CurtainBlock.OPEN, false)
        //                 .property(CurtainBlock.POWERED, false)
        //                 .property(CurtainBlock.ANIMATING, false),
        //         (legacyState, targetState) -> targetState
        //                 .setValue(CurtainBlock.FACING, legacyState.getValue(CurtainBlock.FACING))
        //                 .setValue(CurtainBlock.HALF, legacyState.getValue(CurtainBlock.HALF))
        //                 .setValue(CurtainBlock.SIDE, legacyState.getValue(CurtainBlock.SIDE))
        //                 .setValue(CurtainBlock.OPEN, legacyState.getValue(CurtainBlock.OPEN))
        //                 .setValue(CurtainBlock.POWERED, legacyState.getValue(CurtainBlock.POWERED))
        //                 .setValue(CurtainBlock.ANIMATING, false));
        return builder
                .clientItem(block -> ShadowsAndPetals.asResource(
                        "block/curtain/" + color.getName() + "_curtain"))
                .loot((provider, block) -> provider.dropSelfLowerHalfOnly(block.get()))
                .lang(DatagenLangRegistry.ZH_CN, DyedBlockList.zhName(color) + "窗帘")
                .register();
    });

    public static final DeferredBlock<OrangeTreeBlock> ORANGE_TREE = SAPRegistries
            .block("orange_tree", OrangeTreeBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY))
            .blockstate(() -> NatureBlockModels::orangeTree)
            .loot((provider, tree) -> provider.addTable(
                    tree.get(),
                    LootTable.lootTable().withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .add(LootItem.lootTableItem(ItemRegistry.ORANGE_SEED.get())))
            ))
            .lang(DatagenLangRegistry.ZH_CN, "蜜柑树")
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
                        .mapColor(MapColor.STONE)
                        .noOcclusion()
                        .requiresCorrectToolForDrops())
                .withItem()
                .tooltipComponent(
                        (rockery, stack) -> new RockeryTooltipComponent(rockery, dims),
                        100)
                .creativeTab(CreativeTabKey.NATURE)
                .tags(BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate(() -> (context, generator) -> NatureBlockModels.rockery(context, generator, dims))
                .clientItem(ShadowsAndPetals.asResource("block/rock/1x1x1/0_0_0"))
                .loot((provider, block) -> provider.addTable(block.get(), provider.noDropTable()))
                .lang(DatagenLangRegistry.DEFAULT_LOCALE, "rockery")
                .lang(DatagenLangRegistry.ZH_CN, "石山")
                .register();

        HammerItem.registerRockery(result, dims);
        return result;
    }

    private static DeferredBlock<WindowPaneBlock> registerWindowPane(
            String id,
            WoodBlockList.WoodType woodType,
            String modelName,
            String zhName
    ) {
        return SAPRegistries
                .block(id, WindowPaneBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(woodType.getPlanks())
                        .sound(SoundType.WOOD)
                        .noOcclusion())
                .tags(BlockTags.MINEABLE_WITH_AXE)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_WINDOW_PANES)
                .blockstate(() -> (context, generator) -> WindowPaneModels.block(
                        context,
                        generator,
                        modelName,
                        woodType.getPlanks()
                ))
                .loot((provider, block) -> provider.dropSelf(block.get()))
                .lang(DatagenLangRegistry.ZH_CN, zhName)
                .register();
    }

    public static void init() {}
}
