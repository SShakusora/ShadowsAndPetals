package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.item.chime.WindChimeDyeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class RecipeSerializerRegistry {
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WindChimeDyeRecipe>> WIND_CHIME_DYEING =
            SAPRegistries.RECIPE_SERIALIZERS.register("wind_chime_dyeing", () -> WindChimeDyeRecipe.SERIALIZER);

    private RecipeSerializerRegistry() {
    }

    public static void init() {
    }
}
