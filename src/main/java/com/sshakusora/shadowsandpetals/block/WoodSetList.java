package com.sshakusora.shadowsandpetals.block;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.HedgeBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VerticalSlabBlock;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import com.sshakusora.shadowsandpetals.block.nature.LeavesVerticalSlabBlock;
import com.sshakusora.shadowsandpetals.block.nature.SAPLeavesBlock;
import com.sshakusora.shadowsandpetals.data.lang.TooltipLangBuilder;
import com.sshakusora.shadowsandpetals.data.model.generator.NatureBlockModels;
import com.sshakusora.shadowsandpetals.data.model.generator.StandardBlockModels;
import com.sshakusora.shadowsandpetals.data.model.generator.WoodBlockModels;
import com.sshakusora.shadowsandpetals.registries.CreativeTabKey;
import com.sshakusora.shadowsandpetals.registries.CreativeTabOrder;
import com.sshakusora.shadowsandpetals.registries.ParticleRegistry;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import com.sshakusora.shadowsandpetals.worldgen.SAPTreeGrowers;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

public class WoodSetList extends BlockList<WoodSetList.Type, WoodSetList.WoodSet> {

    public WoodSetList() {
        super(Type.class, WoodSetList::registerWoodSet);
    }

    public WoodSet get(Type type) {
        return getByOrdinal(type.ordinal());
    }

    public enum Type {
        SAKURA("sakura", "樱花木", "樱花", SAPTreeGrowers.SAKURA, ParticleRegistry.SAKURA,
                MapColor.STONE, MapColor.DIRT, MapColor.DIRT, MapColor.COLOR_PINK),
        MAPLE("maple", "枫木", "枫树", SAPTreeGrowers.MAPLE, ParticleRegistry.MAPLE,
                MapColor.COLOR_BROWN, MapColor.WOOD, MapColor.WOOD, MapColor.COLOR_RED),
        GINKGO("ginkgo", "银杏木", "银杏", SAPTreeGrowers.GINKGO, ParticleRegistry.GINKGO,
                MapColor.STONE, MapColor.WOOD, MapColor.WOOD, MapColor.COLOR_YELLOW);

        private final String name;
        private final String woodZhName;
        private final String treeZhName;
        private final TreeGrower grower;
        private final Supplier<? extends ParticleOptions> fallingLeafParticleSupplier;
        private final MapColor barkColor;
        private final MapColor woodColor;
        private final MapColor planksColor;
        private final MapColor leavesColor;

        Type(
                String name,
                String woodZhName,
                String treeZhName,
                TreeGrower grower,
                Supplier<? extends ParticleOptions> fallingLeafParticleSupplier,
                MapColor barkColor,
                MapColor woodColor,
                MapColor planksColor,
                MapColor leavesColor
        ) {
            this.name = name;
            this.woodZhName = woodZhName;
            this.treeZhName = treeZhName;
            this.grower = grower;
            this.fallingLeafParticleSupplier = fallingLeafParticleSupplier;
            this.barkColor = barkColor;
            this.woodColor = woodColor;
            this.planksColor = planksColor;
            this.leavesColor = leavesColor;
        }
    }

    public record WoodSet(
            DeferredBlock<RotatedPillarBlock> log,
            DeferredBlock<RotatedPillarBlock> strippedLog,
            DeferredBlock<RotatedPillarBlock> wood,
            DeferredBlock<RotatedPillarBlock> strippedWood,
            DeferredBlock<Block> planks,
            DeferredBlock<WoodPostBlock> post,
            DeferredBlock<WoodPostBlock> strippedPost,
            DeferredBlock<WoodPostBlock> woodPost,
            DeferredBlock<WoodPostBlock> strippedWoodPost,
            DeferredBlock<SlabBlock> slab,
            DeferredBlock<VerticalSlabBlock> verticalSlab,
            DeferredBlock<StairBlock> stairs,
            DeferredBlock<FenceBlock> fence,
            DeferredBlock<FenceGateBlock> fenceGate,
            DeferredBlock<PressurePlateBlock> pressurePlate,
            DeferredBlock<ButtonBlock> button,
            DeferredBlock<SaplingBlock> sapling,
            DeferredBlock<LeavesBlock> leaves,
            DeferredBlock<SlabBlock> leavesSlab,
            DeferredBlock<LeavesVerticalSlabBlock> leavesVerticalSlab,
            DeferredBlock<StairBlock> leavesStairs,
            DeferredBlock<HedgeBlock> hedge
    ) {}

    private static WoodSet registerWoodSet(Type type) {
        WoodSet[] modelSet = new WoodSet[1];
        String fixZhName = type.woodZhName.endsWith("木") ? type.woodZhName.substring(0, type.woodZhName.length() - 1) : type.woodZhName;
        DeferredBlock<RotatedPillarBlock> log = treeLog(type.name + "_log", type.treeZhName + "原木", type.woodColor, type.barkColor);
        DeferredBlock<RotatedPillarBlock> strippedLog = strippedTreeLog("stripped_" + type.name + "_log", "去皮" + type.treeZhName + "原木", type.woodColor);
        DeferredBlock<RotatedPillarBlock> wood = treeWood(type.name + "_wood", log, type.woodZhName, type.barkColor);
        DeferredBlock<RotatedPillarBlock> strippedWood = strippedTreeWood("stripped_" + type.name + "_wood", strippedLog, "去皮" + type.woodZhName, type.woodColor);
        DeferredBlock<Block> planks = treePlanks(
                type.name + "_planks",
                log,
                strippedLog,
                type.woodZhName + "板",
                type.planksColor,
                () -> modelSet[0]
        );
        DeferredBlock<WoodPostBlock> post = treePost(type.name + "_post", log, fixZhName + "原木柱", type.woodColor, type.barkColor);
        DeferredBlock<WoodPostBlock> strippedPost = treeStrippedPost("stripped_" + type.name + "_post", strippedLog, "去皮" + fixZhName + "原木柱", type.woodColor);
        DeferredBlock<WoodPostBlock> woodPost = treeWoodPost(type.name + "_wood_post", wood, log, type.woodZhName + "柱", type.barkColor);
        DeferredBlock<WoodPostBlock> strippedWoodPost = treeStrippedWoodPost("stripped_" + type.name + "_wood_post", strippedWood, strippedLog, "去皮" + type.woodZhName + "柱", type.woodColor);
        DeferredBlock<SlabBlock> slab = treeSlab(type.name + "_slab", planks, type.woodZhName + "台阶", type.planksColor);
        DeferredBlock<VerticalSlabBlock> verticalSlab = treeVerticalSlab(type.name + "_vertical_slab", slab, planks, "竖直" + type.woodZhName + "台阶", type.planksColor);
        DeferredBlock<StairBlock> stairs = treeStairs(type.name + "_stairs", planks, type.woodZhName + "楼梯", type.planksColor);
        DeferredBlock<FenceBlock> fence = treeFence(type.name + "_fence", planks, type.woodZhName + "栅栏", type.planksColor);
        DeferredBlock<FenceGateBlock> fenceGate = treeFenceGate(type.name + "_fence_gate", planks, type.woodZhName + "栅栏门", type.planksColor);
        DeferredBlock<PressurePlateBlock> pressurePlate = treePressurePlate(type.name + "_pressure_plate", planks, type.woodZhName + "压力板", type.planksColor);
        DeferredBlock<ButtonBlock> button = treeButton(type.name + "_button", planks, type.woodZhName + "按钮");
        DeferredBlock<SaplingBlock> sapling = treeSapling(type.name + "_sapling", type.grower, type.treeZhName + "树苗");
        DeferredBlock<LeavesBlock> leaves = treeLeaves(type.name + "_leaves", sapling, type.treeZhName + "树叶", type.fallingLeafParticleSupplier, type.leavesColor);
        DeferredBlock<SlabBlock> leavesSlab = treeLeavesSlab(type.name + "_leaves_slab", leaves, type.treeZhName + "树叶台阶", type.leavesColor);
        DeferredBlock<LeavesVerticalSlabBlock> leavesVerticalSlab = treeLeavesVerticalSlab(type.name + "_leaves_vertical_slab", leavesSlab, leaves, "竖直" + type.treeZhName + "树叶台阶", type.leavesColor);
        DeferredBlock<StairBlock> leavesStairs = treeLeavesStairs(type.name + "_leaves_stairs", leaves, type.treeZhName + "树叶楼梯", type.leavesColor);
        DeferredBlock<HedgeBlock> hedge = treeHedge(type.name + "_hedge", leaves, type.treeZhName + "树篱", type.leavesColor);
        WoodSet result = new WoodSet(log, strippedLog, wood, strippedWood, planks, post, strippedPost, woodPost, strippedWoodPost, slab, verticalSlab, stairs, fence, fenceGate, pressurePlate, button, sapling, leaves, leavesSlab, leavesVerticalSlab, leavesStairs, hedge);
        modelSet[0] = result;
        return result;
    }

    private static DeferredBlock<RotatedPillarBlock> treeLog(String id, String zhName, MapColor topColor, MapColor sideColor) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(state -> state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y ? topColor : sideColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN, BlockTags.OVERWORLD_NATURAL_LOGS)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_LOGS)
                .lang("zh_cn", zhName)
                .register();
    }

    private static DeferredBlock<RotatedPillarBlock> strippedTreeLog(String id, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_STRIPPED_LOGS)
                .lang("zh_cn", zhName)
                .register();
    }

    private static DeferredBlock<RotatedPillarBlock> treeWood(String id, DeferredBlock<RotatedPillarBlock> sourceBlock, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_WOOD)
                .lang("zh_cn", zhName)
                .recipe((provider, wood) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, wood.get(), 3)
                        .define('W', sourceBlock.get())
                        .pattern("WW")
                        .pattern("WW")
                        .unlockedBy(provider.hasName(sourceBlock.get()), provider.hasItem(sourceBlock.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<RotatedPillarBlock> strippedTreeWood(String id, DeferredBlock<RotatedPillarBlock> sourceBlock, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, RotatedPillarBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_STRIPPED_WOOD)
                .lang("zh_cn", zhName)
                .recipe((provider, wood) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, wood.get(), 3)
                        .define('W', sourceBlock.get())
                        .pattern("WW")
                        .pattern("WW")
                        .unlockedBy(provider.hasName(sourceBlock.get()), provider.hasItem(sourceBlock.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<Block> treePlanks(
            String id,
            DeferredBlock<RotatedPillarBlock> log,
            DeferredBlock<RotatedPillarBlock> strippedLog,
            String zhName,
            MapColor mapColor,
            Supplier<WoodSet> modelSet
    ) {
        return SAPRegistries.block(id)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.PLANKS)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_PLANKS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> WoodBlockModels.woodSet(
                        context,
                        generator,
                        modelSet.get()
                ))
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

    private static DeferredBlock<WoodPostBlock> treePost(String id, DeferredBlock<RotatedPillarBlock> log, String zhName, MapColor topColor, MapColor sideColor) {
        return SAPRegistries.block(id, WoodPostBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(state -> state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y ? topColor : sideColor)
                        .noOcclusion())
                .tags(BlockTags.MINEABLE_WITH_AXE)
                .withItem()
                .tooltipDescription(WoodSetList::woodPostTooltip)
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_POSTS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> WoodBlockModels.post(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(log.get()).getPath()),
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(log.get()).getPath() + "_top")
                ))
                .loot((provider, post) -> provider.dropSelf(post.get()))
                .recipe((provider, post) -> provider.shaped(RecipeCategory.DECORATIONS, post.get(), 8)
                        .define('W', log.get())
                        .pattern("W")
                        .pattern("W")
                        .pattern("W")
                        .unlockedBy(provider.hasName(log.get()), provider.hasItem(log.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<WoodPostBlock> treeStrippedPost(String id, DeferredBlock<RotatedPillarBlock> strippedLog, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, WoodPostBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor)
                        .noOcclusion())
                .tags(BlockTags.MINEABLE_WITH_AXE)
                .withItem()
                .tooltipDescription(WoodSetList::woodPostTooltip)
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_STRIPPED_POSTS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> WoodBlockModels.post(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(strippedLog.get()).getPath()),
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(strippedLog.get()).getPath() + "_top")
                ))
                .loot((provider, post) -> provider.dropSelf(post.get()))
                .recipe((provider, post) -> provider.shaped(RecipeCategory.DECORATIONS, post.get(), 8)
                        .define('W', strippedLog.get())
                        .pattern("W")
                        .pattern("W")
                        .pattern("W")
                        .unlockedBy(provider.hasName(strippedLog.get()), provider.hasItem(strippedLog.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<WoodPostBlock> treeWoodPost(String id, DeferredBlock<RotatedPillarBlock> wood, DeferredBlock<RotatedPillarBlock> log, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, WoodPostBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor)
                        .noOcclusion())
                .tags(BlockTags.MINEABLE_WITH_AXE)
                .withItem()
                .tooltipDescription(WoodSetList::woodPostTooltip)
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_WOOD_POSTS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> WoodBlockModels.post(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(log.get()).getPath()),
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(log.get()).getPath())
                ))
                .loot((provider, post) -> provider.dropSelf(post.get()))
                .recipe((provider, post) -> provider.shaped(RecipeCategory.DECORATIONS, post.get(), 8)
                        .define('W', wood.get())
                        .pattern("W")
                        .pattern("W")
                        .pattern("W")
                        .unlockedBy(provider.hasName(wood.get()), provider.hasItem(wood.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<WoodPostBlock> treeStrippedWoodPost(String id, DeferredBlock<RotatedPillarBlock> strippedWood, DeferredBlock<RotatedPillarBlock> strippedLog, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, WoodPostBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor)
                        .noOcclusion())
                .tags(BlockTags.MINEABLE_WITH_AXE)
                .withItem()
                .tooltipDescription(WoodSetList::woodPostTooltip)
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_STRIPPED_WOOD_POSTS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> WoodBlockModels.post(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(strippedLog.get()).getPath()),
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(strippedLog.get()).getPath())
                ))
                .loot((provider, post) -> provider.dropSelf(post.get()))
                .recipe((provider, post) -> provider.shaped(RecipeCategory.DECORATIONS, post.get(), 8)
                        .define('W', strippedWood.get())
                        .pattern("W")
                        .pattern("W")
                        .pattern("W")
                        .unlockedBy(provider.hasName(strippedWood.get()), provider.hasItem(strippedWood.get()))
                        .save(provider.output()))
                .register();
    }

    private static void woodPostTooltip(TooltipLangBuilder tooltip) {
        tooltip.summary(
                        "A slender wooden _post_ that connects to posts, chains, and hanging fittings.",
                        "可连接木柱、锁链与悬挂件的细长木制_柱体_。")
                .behaviour(
                        "When placed:", "放置时:",
                        "Align along the _axis_ of the clicked face.", "沿点击面的_轴向_排列。")
                .behaviour(
                        "When joined with aligned Posts or Chains:", "与同轴木柱或锁链相连时:",
                        "Automatically form matching _support arms_.", "自动形成匹配的_支撑臂_。")
                .behaviour(
                        "When a compatible fitting is hung below:", "下方悬挂兼容连接件时:",
                        "Extend a short _chain-style connector_.", "延伸出短小的_锁链式连接件_。");
    }

    private static DeferredBlock<SlabBlock> treeSlab(String id, DeferredBlock<Block> planks, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, SlabBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.SLABS, BlockTags.WOODEN_SLABS)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_SLABS)
                .lang("zh_cn", zhName)
                .loot((provider, slab) -> provider.dropSlab(slab.get()))
                .recipe((provider, slab) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, slab.get(), 6)
                        .define('W', planks.get())
                        .pattern("WWW")
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<StairBlock> treeStairs(String id, DeferredBlock<Block> planks, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, properties -> new StairBlock(planks.get().defaultBlockState(), properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.STAIRS, BlockTags.WOODEN_STAIRS)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_STAIRS)
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

    private static DeferredBlock<FenceBlock> treeFence(String id, DeferredBlock<Block> planks, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, FenceBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.FENCES, BlockTags.WOODEN_FENCES)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_FENCES)
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

    private static DeferredBlock<FenceGateBlock> treeFenceGate(String id, DeferredBlock<Block> planks, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, properties -> new FenceGateBlock(WoodType.OAK, properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.FENCE_GATES)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_FENCE_GATES)
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

    private static DeferredBlock<PressurePlateBlock> treePressurePlate(String id, DeferredBlock<Block> planks, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, properties -> new PressurePlateBlock(BlockSetType.OAK, properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)
                        .strength(0.5F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor)
                        .noCollision())
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_PRESSURE_PLATES, BlockTags.PRESSURE_PLATES)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_PRESSURE_PLATES)
                .lang("zh_cn", zhName)
                .loot((provider, plate) -> provider.dropSelf(plate.get()))
                .recipe((provider, plate) -> provider.shaped(RecipeCategory.REDSTONE, plate.get())
                        .define('W', planks.get())
                        .pattern("WW")
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    private static DeferredBlock<ButtonBlock> treeButton(String id, DeferredBlock<Block> planks, String zhName) {
        return SAPRegistries.block(id, properties -> new ButtonBlock(BlockSetType.OAK, 30, properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)
                        .strength(0.5F)
                        .sound(SoundType.WOOD)
                        .mapColor(MapColor.NONE)
                        .noCollision())
                .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.BUTTONS, BlockTags.WOODEN_BUTTONS)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_BUTTONS)
                .lang("zh_cn", zhName)
                .loot((provider, button) -> provider.dropSelf(button.get()))
                .recipe((provider, button) -> provider.shapeless(RecipeCategory.REDSTONE, button.get())
                        .requires(planks.get())
                        .unlockedBy(provider.hasName(planks.get()), provider.hasItem(planks.get()))
                        .save(provider.output()))
                .register();
    }

    public static DeferredBlock<LeavesBlock> treeLeaves(
            String id,
            DeferredBlock<SaplingBlock> sapling,
            String zhName,
            Supplier<? extends ParticleOptions> fallingLeafParticleSupplier,
            MapColor mapColor
    ) {
        return SAPRegistries.<LeavesBlock>block(id, properties -> new SAPLeavesBlock(0.01F, properties, fallingLeafParticleSupplier))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                        .strength(0.2F)
                        .sound(SoundType.GRASS)
                        .mapColor(mapColor)
                        .noOcclusion()
                        .isValidSpawn((state, getter, pos, type1) -> false)
                        .isSuffocating((state, getter, pos) -> false)
                        .isViewBlocking((state, getter, pos) -> false))
                .tags(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_LEAVES)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> NatureBlockModels.leaves(
                        context, generator, ShadowsAndPetals.asResource("block/" + id)))
                .loot((provider, leaves) -> provider.dropLeaves(leaves.get(), sapling.get()))
                .register();
    }

    private static DeferredBlock<VerticalSlabBlock> treeVerticalSlab(
            String id,
            DeferredBlock<SlabBlock> slab,
            DeferredBlock<Block> planks,
            String zhName,
            MapColor mapColor
    ) {
        return SAPRegistries.block(id, VerticalSlabBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)
                        .mapColor(mapColor))
                .tags(BlockTags.MINEABLE_WITH_AXE)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_VERTICAL_SLABS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> StandardBlockModels.verticalSlab(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(planks.get()).getPath()),
                        planks.get(),
                        false
                ))
                .loot((provider, verticalSlab) -> provider.dropSlab(verticalSlab.get()))
                .recipe((provider, verticalSlab) -> {
                    provider.shaped(RecipeCategory.BUILDING_BLOCKS, verticalSlab.get(), 3)
                            .define('S', slab.get())
                            .pattern("S")
                            .pattern("S")
                            .pattern("S")
                            .unlockedBy(provider.hasName(slab.get()), provider.hasItem(slab.get()))
                            .save(provider.output());
                    provider.shapeless(RecipeCategory.BUILDING_BLOCKS, slab.get())
                            .requires(verticalSlab.get())
                            .unlockedBy(provider.hasName(verticalSlab.get()), provider.hasItem(verticalSlab.get()))
                            .save(provider.output(), provider.id(id + "_revert").toString());
                })
                .register();
    }

    public static DeferredBlock<SlabBlock> treeLeavesSlab(String id, DeferredBlock<LeavesBlock> leaves, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, SlabBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                        .strength(0.2F)
                        .sound(SoundType.GRASS)
                        .mapColor(mapColor)
                        .noOcclusion()
                        .isValidSpawn((state, getter, pos, type) -> false)
                        .isSuffocating((state, getter, pos) -> false)
                        .isViewBlocking((state, getter, pos) -> false))
                .tags(BlockTags.MINEABLE_WITH_HOE, BlockTags.SLABS)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_LEAVES_SLABS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> NatureBlockModels.leavesSlab(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(leaves.get()).getPath() + "_0")
                ))
                .loot((provider, slab) -> provider.dropSlab(slab.get()))
                .recipe((provider, slab) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, slab.get(), 6)
                        .define('L', leaves.get())
                        .pattern("LLL")
                        .unlockedBy(provider.hasName(leaves.get()), provider.hasItem(leaves.get()))
                        .save(provider.output()))
                .register();
    }

    public static DeferredBlock<LeavesVerticalSlabBlock> treeLeavesVerticalSlab(
            String id,
            DeferredBlock<SlabBlock> leavesSlab,
            DeferredBlock<LeavesBlock> leaves,
            String zhName,
            MapColor mapColor
    ) {
        return SAPRegistries.block(id, LeavesVerticalSlabBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                        .strength(0.2F)
                        .sound(SoundType.GRASS)
                        .mapColor(mapColor)
                        .noOcclusion()
                        .isValidSpawn((state, getter, pos, type) -> false)
                        .isSuffocating((state, getter, pos) -> false)
                        .isViewBlocking((state, getter, pos) -> false))
                .tags(BlockTags.MINEABLE_WITH_HOE)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_LEAVES_VERTICAL_SLABS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> StandardBlockModels.verticalSlab(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(leaves.get()).getPath() + "_0"),
                        generator.blockModelId(leaves.get()).withSuffix("_0"),
                        true
                ))
                .loot((provider, verticalSlab) -> provider.dropSlab(verticalSlab.get()))
                .recipe((provider, verticalSlab) -> {
                    provider.shaped(RecipeCategory.BUILDING_BLOCKS, verticalSlab.get(), 3)
                            .define('S', leavesSlab.get())
                            .pattern("S")
                            .pattern("S")
                            .pattern("S")
                            .unlockedBy(provider.hasName(leavesSlab.get()), provider.hasItem(leavesSlab.get()))
                            .save(provider.output());
                    provider.shapeless(RecipeCategory.BUILDING_BLOCKS, leavesSlab.get())
                            .requires(verticalSlab.get())
                            .unlockedBy(provider.hasName(verticalSlab.get()), provider.hasItem(verticalSlab.get()))
                            .save(provider.output(), provider.id(id + "_revert").toString());
                })
                .register();
    }

    public static DeferredBlock<StairBlock> treeLeavesStairs(String id, DeferredBlock<LeavesBlock> leaves, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, properties -> new StairBlock(leaves.get().defaultBlockState(), properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                        .strength(0.2F)
                        .sound(SoundType.GRASS)
                        .mapColor(mapColor)
                        .noOcclusion()
                        .isValidSpawn((state, getter, pos, type) -> false)
                        .isSuffocating((state, getter, pos) -> false)
                        .isViewBlocking((state, getter, pos) -> false))
                .tags(BlockTags.MINEABLE_WITH_HOE, BlockTags.STAIRS)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_LEAVES_STAIRS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> NatureBlockModels.leavesStairs(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(leaves.get()).getPath() + "_0")
                ))
                .loot((provider, stairs) -> provider.dropSelf(stairs.get()))
                .recipe((provider, stairs) -> provider.shaped(RecipeCategory.BUILDING_BLOCKS, stairs.get(), 4)
                        .define('L', leaves.get())
                        .pattern("L  ")
                        .pattern("LL ")
                        .pattern("LLL")
                        .unlockedBy(provider.hasName(leaves.get()), provider.hasItem(leaves.get()))
                        .save(provider.output()))
                .register();
    }

    public static DeferredBlock<HedgeBlock> treeHedge(String id, DeferredBlock<LeavesBlock> leaves, String zhName, MapColor mapColor) {
        return SAPRegistries.block(id, HedgeBlock::new)
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                        .strength(0.2F)
                        .sound(SoundType.GRASS)
                        .mapColor(mapColor)
                        .noOcclusion()
                        .isValidSpawn((state, getter, pos, type1) -> false)
                        .isSuffocating((state, getter, pos) -> false)
                        .isViewBlocking((state, getter, pos) -> false))
                .tags(BlockTags.MINEABLE_WITH_HOE)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_HEDGES)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> NatureBlockModels.hedge(
                        context,
                        generator,
                        ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(leaves.get()).getPath() + "_0")
                ))
                .loot((provider, hedge) -> provider.dropSelf(hedge.get()))
                .recipe((provider, hedge) -> provider.shaped(RecipeCategory.DECORATIONS, hedge.get(), 6)
                        .define('L', leaves.get())
                        .pattern("LLL")
                        .pattern("LLL")
                        .unlockedBy(provider.hasName(leaves.get()), provider.hasItem(leaves.get()))
                        .save(provider.output()))
                .register();
    }

    public static DeferredBlock<SaplingBlock> treeSapling(String id, TreeGrower grower, String zhName) {
        return SAPRegistries.block(id, properties -> new SaplingBlock(grower, properties))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)
                        .noOcclusion()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS)
                        .mapColor(MapColor.PLANT)
                        .pushReaction(PushReaction.DESTROY))
                .tags(BlockTags.SAPLINGS)
                .withItem()
                .creativeTab(CreativeTabKey.NATURE, CreativeTabOrder.NATURE_SAPLINGS)
                .lang("zh_cn", zhName)
                .blockstate(() -> (context, generator) -> NatureBlockModels.sapling(
                        context, generator, ShadowsAndPetals.asResource("block/" + id)))
                .itemModel(() -> (context, generator) -> NatureBlockModels.saplingItem(
                        context, generator, ShadowsAndPetals.asResource("block/" + id)))
                .register();
    }
}
