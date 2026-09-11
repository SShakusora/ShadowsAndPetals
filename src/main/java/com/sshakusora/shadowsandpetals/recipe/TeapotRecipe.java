package com.sshakusora.shadowsandpetals.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sshakusora.shadowsandpetals.registries.RecipeSerializerRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public record TeapotRecipe(
        SizedFluidIngredient fluid,
        Ingredient ingredient,
        FluidStack result,
        int processingTime
) implements Recipe<TeapotRecipeInput> {
    public static final MapCodec<TeapotRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(TeapotRecipe::fluid),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(TeapotRecipe::ingredient),
            FluidStack.CODEC.fieldOf("result").forGetter(TeapotRecipe::result),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("processing_time").forGetter(TeapotRecipe::processingTime)
    ).apply(instance, TeapotRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeapotRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedFluidIngredient.STREAM_CODEC,
            TeapotRecipe::fluid,
            Ingredient.CONTENTS_STREAM_CODEC,
            TeapotRecipe::ingredient,
            FluidStack.STREAM_CODEC,
            TeapotRecipe::result,
            ByteBufCodecs.VAR_INT,
            TeapotRecipe::processingTime,
            TeapotRecipe::new
    );

    public static final RecipeSerializer<TeapotRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<TeapotRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TeapotRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    public TeapotRecipe {
        if (processingTime <= 0) {
            throw new IllegalArgumentException("Teapot recipe processing time must be positive");
        }
    }

    @Override
    public boolean matches(TeapotRecipeInput input, Level level) {
        return fluid.test(input.fluid()) && ingredient.test(input.ingredient());
    }

    @Override
    public ItemStack assemble(TeapotRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String getGroup() {
        return "";
    }

    @Override
    public RecipeSerializer<TeapotRecipe> getSerializer() {
        return RecipeSerializerRegistry.TEAPOT_BREWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<TeapotRecipe> getType() {
        return RecipeSerializerRegistry.TEAPOT_BREWING_TYPE.get();
    }

}