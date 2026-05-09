package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.compat.CompatInfo;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredItem;

public class ItemRegistry {
    public static final DeferredItem<Item> RAW_BAUXITE = SAPRegistries.item("raw_bauxite")
            .alias(CompatInfo.CHINJUFU_MOD, "item_bauxite")
            .model((provider, item) -> provider.generatedItem(item.get()))
            .lang("zh_cn", "粗矾土")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static final DeferredItem<Item> ALUMINUM_INGOT = SAPRegistries.item("aluminum_ingot")
            .alias(CompatInfo.CHINJUFU_MOD, "item_ingot_alumi")
            .model((provider, item) -> provider.generatedItem(item.get()))
            .recipe((provider, ingot) -> {
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(RAW_BAUXITE.get()), RecipeCategory.MISC, ingot.get(), 0.7F, 200)
                        .unlockedBy(provider.hasName(RAW_BAUXITE.get()), provider.hasItem(RAW_BAUXITE.get()))
                        .save(provider.output(), provider.id("aluminum_ingot_from_smelting"));
                SimpleCookingRecipeBuilder.blasting(Ingredient.of(RAW_BAUXITE.get()), RecipeCategory.MISC, ingot.get(), 0.7F, 100)
                        .unlockedBy(provider.hasName(RAW_BAUXITE.get()), provider.hasItem(RAW_BAUXITE.get()))
                        .save(provider.output(), provider.id("aluminum_ingot_from_blasting"));
            })
            .lang("zh_cn", "铝锭")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static void init() {}
}
