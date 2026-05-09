package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    private RecipeOutput recipeOutput;

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.Provider holderLookup) {
        this.recipeOutput = recipeOutput;
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

    public String hasName(ItemLike item) {
        return getHasName(item);
    }

    public String hasName(TagKey<Item> tag) {
        return "has_" + tag.location().getPath().replace('/', '_');
    }

    public Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, path);
    }

    public void save(RecipeBuilder builder) {
        builder.save(output());
    }

    public void save(RecipeBuilder builder, Identifier id) {
        builder.save(output(), id);
    }
}
