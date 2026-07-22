package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.item.chime.WindChimeDyeRecipe;
import com.sshakusora.shadowsandpetals.recipe.TeapotRecipe;
import com.sshakusora.shadowsandpetals.registries.builder.RegRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class RecipeSerializerRegistry {
    private static final RegRecipeBuilder.RegisteredRecipe<WindChimeDyeRecipe> WIND_CHIME_DYEING_RECIPE = SAPRegistries
            .<WindChimeDyeRecipe>recipe("wind_chime_dyeing", () -> WindChimeDyeRecipe.SERIALIZER)
            .register();

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WindChimeDyeRecipe>> WIND_CHIME_DYEING = WIND_CHIME_DYEING_RECIPE.serializer();

    private static final RegRecipeBuilder.RegisteredRecipe<TeapotRecipe> TEAPOT_BREWING_RECIPE = SAPRegistries
            .<TeapotRecipe>recipe("teapot_brewing", () -> TeapotRecipe.SERIALIZER)
            .type()
            .datagen("tea", provider -> new TeapotRecipe(
                    provider.fluidIngredient(Identifier.withDefaultNamespace("water"), FluidType.BUCKET_VOLUME),
                    provider.ingredient(ItemTags.LEAVES),
                    provider.fluidResult(FluidRegistry.TEA.getId(), FluidType.BUCKET_VOLUME),
                    200
            ))
            .register();

    public static final DeferredHolder<RecipeType<?>, RecipeType<TeapotRecipe>> TEAPOT_BREWING_TYPE = TEAPOT_BREWING_RECIPE.type().orElseThrow();

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TeapotRecipe>> TEAPOT_BREWING_SERIALIZER = TEAPOT_BREWING_RECIPE.serializer();

    private RecipeSerializerRegistry() {
    }

    public static void init() {
    }
}
