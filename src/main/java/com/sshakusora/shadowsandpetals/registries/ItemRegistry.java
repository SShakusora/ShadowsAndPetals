package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.compat.CompatInfo;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredItem;

public class ItemRegistry {
    public static final DeferredItem<Item> RAW_ALUMINUM = SAPRegistries.item("raw_aluminum")
            .alias(CompatInfo.CHINJUFU_MOD, "item_bauxite")
            .lang("zh_cn", "粗铝")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static final DeferredItem<Item> ALUMINUM_INGOT = SAPRegistries.item("aluminum_ingot")
            .alias(CompatInfo.CHINJUFU_MOD, "item_ingot_alumi")
            .recipe((provider, ingot) -> {
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(RAW_ALUMINUM.get()), RecipeCategory.MISC, ingot.get(), 0.7F, 200)
                        .unlockedBy(provider.hasName(RAW_ALUMINUM.get()), provider.hasItem(RAW_ALUMINUM.get()))
                        .save(provider.output(), provider.id("aluminum_ingot_from_smelting"));
                SimpleCookingRecipeBuilder.blasting(Ingredient.of(RAW_ALUMINUM.get()), RecipeCategory.MISC, ingot.get(), 0.7F, 100)
                        .unlockedBy(provider.hasName(RAW_ALUMINUM.get()), provider.hasItem(RAW_ALUMINUM.get()))
                        .save(provider.output(), provider.id("aluminum_ingot_from_blasting"));
            })
            .lang("zh_cn", "铝锭")
            .creativeTab(CreativeTabType.MAIN)
            .register();

    public static void init() {}
}
