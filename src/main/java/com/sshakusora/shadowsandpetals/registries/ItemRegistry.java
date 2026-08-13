package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.item.hammer.HammerItem;
import com.sshakusora.shadowsandpetals.item.harrow.HarrowItem;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredItem;

public class ItemRegistry {
    public static final DeferredItem<BucketItem> TEA_BUCKET = SAPRegistries.item(
                    "tea_bucket",
                    properties -> new BucketItem(FluidRegistry.TEA.get(), properties)
            )
            .properties(properties -> properties.craftRemainder(Items.BUCKET).stacksTo(1))
            .customClientItem(ShadowsAndPetals.asResource("tea_bucket"))
            .lang(DatagenLangRegistry.ZH_CN, "茶桶")
            .creativeTab(CreativeTabKey.MAIN)
            .register();

    public static final DeferredItem<BlockItem> ORANGE_SEED = SAPRegistries.item(
                    "orange_seed",
                    properties -> new BlockItem(BlockRegistry.ORANGE_TREE.get(), properties)
            )
            .model(() -> (context, generator) -> generator.generatedItem(context.get()))
            .lang(DatagenLangRegistry.ZH_CN, "蜜柑种子")
            .creativeTab(CreativeTabKey.AGRICULTURE)
            .register();

    public static final DeferredItem<Item> ORANGE = SAPRegistries.item("orange")
            .properties(properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.3F)
                    .build()))
            .model(() -> (context, generator) -> generator.generatedItem(context.get()))
            .recipe((provider, orange) -> {
                provider.shapeless(RecipeCategory.FOOD, ORANGE_SEED.get())
                        .requires(orange.get())
                        .unlockedBy(provider.hasName(orange.get()), provider.hasItem(orange.get()))
                        .save(provider.output());
            })
            .lang(DatagenLangRegistry.ZH_CN, "蜜柑")
            .creativeTab(CreativeTabKey.AGRICULTURE)
            .register();

    public static final DeferredItem<Item> RAW_BAUXITE = SAPRegistries.item("raw_bauxite")
            .model(() -> (context, generator) -> generator.generatedItem(context.get()))
            .lang(DatagenLangRegistry.ZH_CN, "粗矾土")
            .creativeTab(CreativeTabKey.MAIN)
            .register();

    public static final DeferredItem<Item> ALUMINUM_INGOT = SAPRegistries.item("aluminum_ingot")
            .model(() -> (context, generator) -> generator.generatedItem(context.get()))
            .recipe((provider, ingot) -> {
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(RAW_BAUXITE.get()), RecipeCategory.MISC, CookingBookCategory.MISC, ingot.get(), 0.7F, 200)
                        .unlockedBy(provider.hasName(RAW_BAUXITE.get()), provider.hasItem(RAW_BAUXITE.get()))
                        .save(provider.output(), provider.id("aluminum_ingot_from_smelting").toString());
                SimpleCookingRecipeBuilder.blasting(Ingredient.of(RAW_BAUXITE.get()), RecipeCategory.MISC, CookingBookCategory.MISC, ingot.get(), 0.7F, 100)
                        .unlockedBy(provider.hasName(RAW_BAUXITE.get()), provider.hasItem(RAW_BAUXITE.get()))
                        .save(provider.output(), provider.id("aluminum_ingot_from_blasting").toString());
            })
            .lang(DatagenLangRegistry.ZH_CN, "铝锭")
            .creativeTab(CreativeTabKey.MAIN)
            .register();

    public static final DeferredItem<Item> CLAM = SAPRegistries.item("clam")
            .model(() -> (context, generator) -> generator.generatedItem(context.get()))
            .lang(DatagenLangRegistry.ZH_CN, "蛤蜊")
            .creativeTab(CreativeTabKey.AGRICULTURE)
            .register();

    public static final DeferredItem<Item> CHISEL = SAPRegistries.item("chisel")
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A stoneworking tool that enables _rockery carving_.",
                            "用于_石山雕刻_的石工工具。")
                    .action(
                            "Hold in Off Hand", "放在副手",
                            "Hold right-click with a Hammer on _Stone_ to carve matching rockeries.", "主手持锤子对_石头_长按右键，雕刻匹配形状的石山。"))
            .recipe((provider, item) -> {
                provider.shaped(RecipeCategory.TOOLS, item.get())
                        .define('A', ALUMINUM_INGOT.get())
                        .pattern("A")
                        .pattern("A")
                        .unlockedBy(provider.hasName(ALUMINUM_INGOT.get()), provider.hasItem(ALUMINUM_INGOT.get()))
                        .save(provider.output());
            })
            .lang(DatagenLangRegistry.ZH_CN, "凿子")
            .creativeTab(CreativeTabKey.MAIN)
            .register();

    public static final DeferredItem<HammerItem> HAMMER = SAPRegistries.item("hammer", HammerItem::new)
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A heavy tool that carves connected _Stone_ into rockeries.",
                            "将相连的_石头_雕刻成石山的重型工具。")
                    .action(
                            "Chisel in Off Hand", "副手持凿子",
                            "Hold right-click on _Stone_; the largest matching rockery is carved when the strike completes.", "对_石头_长按右键；敲击完成后会雕刻出可匹配的最大石山。"))
            .recipe((provider, item) -> {
                provider.shaped(RecipeCategory.TOOLS, item.get())
                        .define('A', ALUMINUM_INGOT.get())
                        .define('S', Items.STICK)
                        .pattern("AAA")
                        .pattern("ASA")
                        .pattern(" S ")
                        .unlockedBy(provider.hasName(ALUMINUM_INGOT.get()), provider.hasItem(ALUMINUM_INGOT.get()))
                        .save(provider.output());
            })
            .lang(DatagenLangRegistry.ZH_CN, "锤子")
            .creativeTab(CreativeTabKey.MAIN)
            .register();

    public static final DeferredItem<HarrowItem> HARROW = SAPRegistries.item("harrow", HarrowItem::new)
            .properties(properties -> properties.durability(256))
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A landscaping and shore-foraging _rake_.",
                            "可用于造景与海滩采集的_耙子_。")
                    .behaviour(
                            "When Gravel is right-clicked", "右键点击沙砾时",
                            "Rake it into a _Samon block_ that connects to nearby samon.", "将其耙成会与附近砂纹相连的_砂纹方块_。")
                    .behaviour(
                            "When the top of Samon is right-clicked", "右键点击砂纹方块顶部时",
                            "Cycle through straight and corner _rake patterns_.", "循环切换直线与转角_砂纹样式_。")
                    .behaviour(
                            "When Beach Sand beside Water is held", "对邻水海滩沙长按右键时",
                            "Rake continuously for a chance to uncover a _Clam_.", "持续耙挖，有概率找到_蛤蜊_。"))
            .recipe((provider, item) -> {
                provider.shaped(RecipeCategory.TOOLS, item.get())
                        .define('B', Items.BAMBOO)
                        .pattern("BBB")
                        .pattern(" B ")
                        .pattern(" B ")
                        .unlockedBy(provider.hasName(Items.BAMBOO), provider.hasItem(Items.BAMBOO))
                        .save(provider.output());
            })
            .lang(DatagenLangRegistry.ZH_CN, "耙子")
            .creativeTab(CreativeTabKey.MAIN)
            .register();

    public static void init() {}
}
