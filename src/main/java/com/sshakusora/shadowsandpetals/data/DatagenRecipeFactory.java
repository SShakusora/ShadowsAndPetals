package com.sshakusora.shadowsandpetals.data;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class DatagenRecipeFactory {
    private DatagenRecipeFactory() {}

    public static void ingotPile(ModRecipeProvider provider, DeferredBlock<?> pile, ItemLike ingot, String unpackedRecipeId) {
        provider.shaped(RecipeCategory.DECORATIONS, pile.get(), 2)
                .define('I', ingot)
                .pattern("III")
                .pattern("I I")
                .pattern("III")
                .unlockedBy(provider.hasName(ingot), provider.hasItem(ingot))
                .save(provider.output());

        provider.shapeless(RecipeCategory.MISC, ingot, 4)
                .requires(pile.get())
                .unlockedBy(provider.hasName(pile.get()), provider.hasItem(pile.get()))
                .save(provider.output(), provider.id(unpackedRecipeId).toString());
    }

    public static void storageBlock(ModRecipeProvider provider, DeferredBlock<?> block, ItemLike ingredient, String unpackedRecipeId) {
        provider.shaped(RecipeCategory.BUILDING_BLOCKS, block.get())
                .define('I', ingredient)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .unlockedBy(provider.hasName(ingredient), provider.hasItem(ingredient))
                .save(provider.output());

        provider.shapeless(RecipeCategory.MISC, ingredient.asItem(), 9)
                .requires(block.get())
                .unlockedBy(provider.hasName(block.get()), provider.hasItem(block.get()))
                .save(provider.output(), provider.id(unpackedRecipeId).toString());
    }
}
