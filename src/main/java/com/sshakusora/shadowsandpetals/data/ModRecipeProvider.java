package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    private HolderLookup.Provider registries;
    private RecipeOutput recipeOutput;

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.Provider registries) {
        this.registries = registries;
        buildRecipes(recipeOutput);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        this.recipeOutput = output;
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
        return Ingredient.of(tag);
    }

    public SizedFluidIngredient fluidIngredient(ResourceLocation id, int amount) {
        var holder = registries.lookupOrThrow(Registries.FLUID)
                .getOrThrow(ResourceKey.create(Registries.FLUID, id));
        return SizedFluidIngredient.of(holder.value(), amount);
    }

    public FluidStack fluidResult(ResourceLocation id, int amount) {
        var holder = registries.lookupOrThrow(Registries.FLUID)
                .getOrThrow(ResourceKey.create(Registries.FLUID, id));
        return new FluidStack(holder, amount);
    }

    public String hasName(ItemLike item) {
        return getHasName(item);
    }

    public String hasName(TagKey<Item> tag) {
        return "has_" + tag.location().getPath().replace('/', '_');
    }

    public ResourceLocation id(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike item) {
        return ShapedRecipeBuilder.shaped(category, item);
    }

    public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike item, int count) {
        return ShapedRecipeBuilder.shaped(category, item, count);
    }

    public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike item) {
        return ShapelessRecipeBuilder.shapeless(category, item);
    }

    public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike item, int count) {
        return ShapelessRecipeBuilder.shapeless(category, item, count);
    }

    public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemStack result) {
        return ShapelessRecipeBuilder.shapeless(category, result);
    }

    public void stonecutter(RecipeCategory category, ItemLike result, int count, ItemLike ingredient) {
        stonecutterResultFromBase(output(), category, result, ingredient, count);
    }

    public void slabFromBase(RecipeCategory category, ItemLike result, ItemLike base) {
        ShapedRecipeBuilder.shaped(category, result, 6)
                .define('#', Ingredient.of(base))
                .pattern("###")
                .unlockedBy(getHasName(base), has(base))
                .save(output());
    }

    public void storageBlock(RecipeCategory unpackedCategory, ItemLike unpacked, RecipeCategory packedCategory, ItemLike packed, String unpackedRecipeId) {
        nineBlockStorageRecipes(
                output(),
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

    public void save(RecipeBuilder builder, ResourceLocation id) {
        builder.save(output(), id.toString());
    }

}