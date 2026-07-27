package com.sshakusora.shadowsandpetals.compat.jei;

import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import com.sshakusora.shadowsandpetals.recipe.WindChimeDyeRecipe;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.Comparator;
import java.util.List;

final class WindChimeDyeRecipeExtension implements ICraftingCategoryExtension<WindChimeDyeRecipe> {
    static final WindChimeDyeRecipeExtension INSTANCE = new WindChimeDyeRecipeExtension();

    private WindChimeDyeRecipeExtension() {
    }

    @Override
    public List<SlotDisplay> getIngredients(RecipeHolder<WindChimeDyeRecipe> recipeHolder) {
        return display(recipeHolder).ingredients();
    }

    @Override
    public int getWidth(RecipeHolder<WindChimeDyeRecipe> recipeHolder) {
        return display(recipeHolder).width();
    }

    @Override
    public int getHeight(RecipeHolder<WindChimeDyeRecipe> recipeHolder) {
        return display(recipeHolder).height();
    }

    @Override
    public void onDisplayedIngredientsUpdate(
            RecipeHolder<WindChimeDyeRecipe> recipeHolder,
            List<IRecipeSlotDrawable> recipeSlots,
            IFocusGroup focuses
    ) {
        if (recipeHolder.value().target() != WindChimeDyeRecipe.Target.BOTH) {
            return;
        }

        List<IRecipeSlotDrawable> dyeSlots = recipeSlots.stream()
                .filter(slot -> slot.getRole() == RecipeIngredientRole.INPUT)
                .filter(slot -> slot.getDisplayedItemStack()
                        .map(stack -> stack.has(DataComponents.DYE))
                        .orElse(false))
                .sorted(Comparator.comparingInt(slot -> slot.getAreaIncludingBackground().getY()))
                .toList();
        if (dyeSlots.size() != 2) {
            return;
        }

        DyeColor ribbon = displayedDye(dyeSlots.get(0));
        DyeColor vane = displayedDye(dyeSlots.get(1));
        if (ribbon == null || vane == null) {
            return;
        }

        ItemStack result = new ItemStack(BlockRegistry.WIND_CHIME.asItem());
        new WindChimeColors(ribbon, vane).applyToStack(result);
        recipeSlots.stream()
                .filter(slot -> slot.getRole() == RecipeIngredientRole.OUTPUT)
                .findFirst()
                .ifPresent(slot -> {
                    slot.clearDisplayOverrides();
                    slot.createDisplayOverrides().add(result);
                });
    }

    private static DyeColor displayedDye(IRecipeSlotDrawable slot) {
        return slot.getDisplayedItemStack()
                .map(stack -> stack.get(DataComponents.DYE))
                .orElse(null);
    }

    private static ShapedCraftingRecipeDisplay display(RecipeHolder<WindChimeDyeRecipe> recipeHolder) {
        return (ShapedCraftingRecipeDisplay) recipeHolder.value().display().getFirst();
    }
}
