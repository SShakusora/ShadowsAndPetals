package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.util.BoxContents;
import com.sshakusora.shadowsandpetals.util.GeneratedItemModels;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class DatagenRecipeFactory {
    private DatagenRecipeFactory() {}

    public static void ingotPile(ModRecipeProvider provider, DeferredBlock<?> pile, ItemLike ingot, String unpackedRecipeId) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pile.get(), 2)
                .define('I', ingot)
                .pattern("III")
                .pattern("I I")
                .pattern("III")
                .unlockedBy(provider.hasName(ingot), provider.hasItem(ingot))
                .save(provider.output());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ingot, 4)
                .requires(pile.get())
                .unlockedBy(provider.hasName(pile.get()), provider.hasItem(pile.get()))
                .save(provider.output(), provider.id(unpackedRecipeId));
    }

    public static void storageBlock(ModRecipeProvider provider, DeferredBlock<?> block, ItemLike ingredient, String unpackedRecipeId) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block.get())
                .define('I', ingredient)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .unlockedBy(provider.hasName(ingredient), provider.hasItem(ingredient))
                .save(provider.output());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ingredient.asItem(), 9)
                .requires(block.get())
                .unlockedBy(provider.hasName(block.get()), provider.hasItem(block.get()))
                .save(provider.output(), provider.id(unpackedRecipeId));
    }

    /**
     * Emits the empty-box stonecutting recipe plus one shapeless recipe per tagged content
     * item, so every box variant shown in the boxes tab is craftable.
     */
    public static void boxes(ModRecipeProvider provider, ItemLike box, BlockEntityType<?> boxType) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ItemTags.PLANKS), RecipeCategory.DECORATIONS, box)
                .unlockedBy("has_planks", provider.hasTag(ItemTags.PLANKS))
                .save(provider.output(), provider.id("box_from_planks"));

        Item boxItem = box.asItem();
        for (Item content : BoxContents.taggedContentItems(
                provider.serverResourceManager(),
                item -> GeneratedItemModels.hasGeneratedParent(provider.clientResourceManager(), item)
        )) {
            if (content == boxItem) {
                continue;
            }

            ItemStack result = BoxContents.createContentBox(boxItem, content, boxType);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, result)
                    .requires(boxItem)
                    .requires(content, 8)
                    .unlockedBy(provider.hasName(content), provider.hasItem(content))
                    .save(provider.output(), provider.id(contentBoxRecipeName(content)));
        }
    }

    private static String contentBoxRecipeName(Item content) {
        ResourceLocation contentId = BuiltInRegistries.ITEM.getKey(content);
        if (contentId == null) {
            throw new IllegalArgumentException("Unregistered content item " + content);
        }

        String path = contentId.getPath() + "_box";
        return contentId.getNamespace().equals("minecraft")
                ? path
                : contentId.getNamespace() + "_" + path;
    }
}
