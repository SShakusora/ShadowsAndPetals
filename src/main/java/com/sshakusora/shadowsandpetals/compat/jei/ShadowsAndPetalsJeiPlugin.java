package com.sshakusora.shadowsandpetals.compat.jei;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import com.sshakusora.shadowsandpetals.item.hammer.HammerItem;
import com.sshakusora.shadowsandpetals.recipe.WindChimeDyeRecipe;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

@JeiPlugin
public class ShadowsAndPetalsJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return ShadowsAndPetals.asResource("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(
                BlockRegistry.WIND_CHIME.asItem(),
                (stack, context) -> context == UidContext.Recipe
                        ? null
                        : WindChimeColors.fromStack(stack)
        );
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RockeryRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
                RockeryRecipeCategory.TYPE,
                ItemRegistry.HAMMER.get(),
                ItemRegistry.CHISEL.get(),
                Blocks.STONE
        );
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(
                WindChimeDyeRecipe.class,
                WindChimeDyeRecipeExtension.INSTANCE
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RockeryRecipeCategory.TYPE, rockeryRecipes());
    }

    private static List<RockeryInfoRecipe> rockeryRecipes() {
        return HammerItem.rockeryTemplates().stream()
                .map(template -> new RockeryInfoRecipe(template.block().get(), template.dimensions()))
                .toList();
    }
}
