package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    private RecipeOutput recipeOutput;

    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        this.recipeOutput = this.output;
        try {
            for (var generator : DatagenRecipeRegistry.generators()) {
                generator.accept(this);
            }
        } finally {
            this.recipeOutput = null;
        }
    }

    public RecipeOutput output() {
        if (recipeOutput == null) {
            throw new IllegalStateException("RecipeOutput is only available while datagen is running");
        }
        return recipeOutput;
    }

    public Criterion<InventoryChangeTrigger.TriggerInstance> hasItem(ItemLike item) {
        return has(item);
    }

    public Criterion<InventoryChangeTrigger.TriggerInstance> hasTag(TagKey<Item> tag) {
        return has(tag);
    }

    public Ingredient ingredient(TagKey<Item> tag) {
        return Ingredient.of(items.getOrThrow(tag));
    }

    public String hasName(ItemLike item) {
        return getHasName(item);
    }

    public String hasName(TagKey<Item> tag) {
        return "has_" + tag.location().getPath().replace('/', '_');
    }

    public Identifier id(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike item) {
        return super.shaped(category, item);
    }

    public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike item, int count) {
        return super.shaped(category, item, count);
    }

    public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike item) {
        return super.shapeless(category, item);
    }

    public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike item, int count) {
        return super.shapeless(category, item, count);
    }

    public void stonecutter(RecipeCategory category, ItemLike result, int count, ItemLike ingredient) {
        stonecutterResultFromBase(category, result, ingredient, count);
    }

    public void slabFromBase(RecipeCategory category, ItemLike result, ItemLike base) {
        slabBuilder(category, result, Ingredient.of(base))
                .unlockedBy(getHasName(base), has(base))
                .save(output());
    }

    public void storageBlock(RecipeCategory unpackedCategory, ItemLike unpacked, RecipeCategory packedCategory, ItemLike packed, String unpackedRecipeId) {
        nineBlockStorageRecipes(
                unpackedCategory,
                unpacked,
                packedCategory,
                packed,
                id(getSimpleRecipeName(packed)).toString(),
                null,
                id(unpackedRecipeId).toString(),
                null
        );
    }

    public void stairsFromBase(ItemLike result, ItemLike base) {
        stairBuilder(result, Ingredient.of(base))
                .unlockedBy(getHasName(base), has(base))
                .save(output());
    }

    public void save(RecipeBuilder builder) {
        builder.save(output());
    }

    public void save(RecipeBuilder builder, Identifier id) {
        builder.save(output(), id.toString());
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        public String getName() {
            return "ShadowsAndPetals Recipes";
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }
    }
}
