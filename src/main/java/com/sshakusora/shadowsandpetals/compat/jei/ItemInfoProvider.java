package com.sshakusora.shadowsandpetals.compat.jei;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

@JeiPlugin
public final class ItemInfoProvider implements IModPlugin {

    @Override
    public @NonNull Identifier getPluginUid() {
        return ShadowsAndPetals.asResource("jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(
                BlockRegistry.IRORI.get(),
                Component.translatable(BuiltinLanguageKeys.JEI_IRORI_INFO_1.key()),
                Component.translatable(BuiltinLanguageKeys.JEI_IRORI_INFO_2.key()),
                Component.translatable(BuiltinLanguageKeys.JEI_IRORI_INFO_3.key())
        );

        registration.addIngredientInfo(
                ItemRegistry.CHISEL.get(),
                Component.translatable(BuiltinLanguageKeys.JEI_CHISEL_INFO_1.key())
        );

        registration.addIngredientInfo(
                ItemRegistry.HAMMER.get(),
                Component.translatable(BuiltinLanguageKeys.JEI_HAMMER_INFO_1.key())
        );
    }
}
