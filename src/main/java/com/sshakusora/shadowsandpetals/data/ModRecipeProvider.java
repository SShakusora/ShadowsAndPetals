package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    private RecipeOutput recipeOutput;
    private final ResourceManager serverResourceManager;
    private final ResourceManager clientResourceManager;

    public ModRecipeProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries,
            ResourceManager serverResourceManager,
            ResourceManager clientResourceManager
    ) {
        super(output, registries);
        this.serverResourceManager = serverResourceManager;
        this.clientResourceManager = clientResourceManager;
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

    /**
     * Datapack resource manager used to resolve real item tags. The plain registry lookup from
     * {@link net.minecraft.data.registries.VanillaRegistries#createLookup()} has no tag data, so
     * recipe generators that enumerate tag contents must read tag files instead.
     */
    public ResourceManager serverResourceManager() {
        return serverResourceManager;
    }

    /**
     * Client asset resource manager used to inspect item model JSON, mirroring the creative-tab
     * model filter during recipe datagen.
     */
    public ResourceManager clientResourceManager() {
        return clientResourceManager;
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

    public ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, path);
    }

    public void save(RecipeBuilder builder) {
        builder.save(output());
    }

    public void save(RecipeBuilder builder, ResourceLocation id) {
        builder.save(output(), id);
    }
}
