package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.item.HammerItem;
import com.sshakusora.shadowsandpetals.item.HarrowItem;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredItem;

public class ItemRegistry {
    public static final DeferredItem<Item> RAW_BAUXITE = SAPRegistries.item("raw_bauxite")
            .model((provider, item) -> provider.generatedItem(item.get()))
            .lang("zh_cn", "粗矾土")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static final DeferredItem<Item> ALUMINUM_INGOT = SAPRegistries.item("aluminum_ingot")
            .model((provider, item) -> provider.generatedItem(item.get()))
            .recipe((provider, ingot) -> {
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(RAW_BAUXITE.get()), RecipeCategory.MISC, CookingBookCategory.MISC, ingot.get(), 0.7F, 200)
                        .unlockedBy(provider.hasName(RAW_BAUXITE.get()), provider.hasItem(RAW_BAUXITE.get()))
                        .save(provider.output(), provider.id("aluminum_ingot_from_smelting").toString());
                SimpleCookingRecipeBuilder.blasting(Ingredient.of(RAW_BAUXITE.get()), RecipeCategory.MISC, CookingBookCategory.MISC, ingot.get(), 0.7F, 100)
                        .unlockedBy(provider.hasName(RAW_BAUXITE.get()), provider.hasItem(RAW_BAUXITE.get()))
                        .save(provider.output(), provider.id("aluminum_ingot_from_blasting").toString());
            })
            .lang("zh_cn", "铝锭")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static final DeferredItem<Item> CHISEL = SAPRegistries.item("chisel")
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A stoneworking tool used together with a _Hammer_.",
                            "与_锤子_配合使用的石材加工工具。")
                    .action(
                            "Hold in Off Hand", "放在副手",
                            "Use a Hammer on _Stone_ to carve rockeries.", "用锤子敲击_石头_以雕刻石山。"))
            .recipe((provider, item) -> {
                provider.shaped(RecipeCategory.TOOLS, item.get())
                        .define('A', ALUMINUM_INGOT.get())
                        .pattern("A")
                        .pattern("A")
                        .unlockedBy(provider.hasName(ALUMINUM_INGOT.get()), provider.hasItem(ALUMINUM_INGOT.get()))
                        .save(provider.output());
            })
            .lang("zh_cn", "凿子")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static final DeferredItem<HammerItem> HAMMER = SAPRegistries.item("hammer", HammerItem::new)
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A heavy tool for carving _Stone_ into rockeries.",
                            "用于将_石头_雕刻成石山的重型工具。")
                    .action(
                            "Chisel in Off Hand", "副手持凿子",
                            "Hold right-click on _Stone_ to carve a rockery.", "对着_石头_长按右键以雕刻石山。"))
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
            .lang("zh_cn", "锤子")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static final DeferredItem<HarrowItem> HARROW = SAPRegistries.item("harrow", HarrowItem::new)
            .tooltipDescription(tooltip -> tooltip
                    .summary(
                            "A landscaping tool for shaping _gravel gardens_.",
                            "用于塑造_枯山水庭院_的造景工具。")
                    .behaviour(
                            "When Gravel is right-clicked", "右键点击沙砾时",
                            "Turn it into a _Samon block_.", "将其转化为_砂纹方块_。")
                    .behaviour(
                            "When the top of Samon is right-clicked", "右键点击砂纹方块顶部时",
                            "Cycle through its _connection patterns_.", "循环切换其_连接样式_。"))
            .recipe((provider, item) -> {
                provider.shaped(RecipeCategory.TOOLS, item.get())
                        .define('B', Items.BAMBOO)
                        .pattern("BBB")
                        .pattern(" B ")
                        .pattern(" B ")
                        .unlockedBy(provider.hasName(Items.BAMBOO), provider.hasItem(Items.BAMBOO))
                        .save(provider.output());
            })
            .lang("zh_cn", "耙子")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static void init() {}
}
