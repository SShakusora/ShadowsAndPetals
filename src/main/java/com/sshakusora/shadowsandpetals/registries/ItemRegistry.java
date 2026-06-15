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
