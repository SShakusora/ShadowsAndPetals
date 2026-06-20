package com.sshakusora.shadowsandpetals.compat.jei;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.item.HammerItem;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

@JeiPlugin
public final class ShadowsAndPetalsJeiPlugin implements IModPlugin {

    private static final Identifier UID = ShadowsAndPetals.asResource("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new RockeryRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<RockeryInfoRecipe> recipes = HammerItem.rockeryTemplates().stream()
                .map(template -> {
                    RockeryBlock block = template.block().get();
                    return new RockeryInfoRecipe(block, template.dimensions());
                })
                .toList();

        registration.addRecipes(RockeryRecipeCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(RockeryRecipeCategory.TYPE, new ItemStack(ItemRegistry.HAMMER.get()));
        registration.addCraftingStation(RockeryRecipeCategory.TYPE, new ItemStack(ItemRegistry.CHISEL.get()));
        registration.addCraftingStation(RockeryRecipeCategory.TYPE, new ItemStack(Blocks.STONE));
    }
}
