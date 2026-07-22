package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenRecipeRegistry;
import com.sshakusora.shadowsandpetals.data.ModRecipeProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fluent builder for recipe serializer registration, with optional recipe type and datagen wiring.
 */
public final class RegRecipeBuilder<R extends Recipe<?>> {
    private final DeferredRegister<RecipeSerializer<?>> serializerRegistry;
    private final DeferredRegister<RecipeType<?>> typeRegistry;
    private final String name;
    private Supplier<? extends RecipeSerializer<R>> serializerFactory;
    private Supplier<? extends RecipeType<R>> typeFactory;
    private final Map<Identifier, Function<ModRecipeProvider, R>> datagenRecipes = new LinkedHashMap<>();

    public RegRecipeBuilder(
            DeferredRegister<RecipeSerializer<?>> serializerRegistry,
            DeferredRegister<RecipeType<?>> typeRegistry,
            String name
    ) {
        this.serializerRegistry = serializerRegistry;
        this.typeRegistry = typeRegistry;
        this.name = name;
    }

    /** Sets the serializer factory required by this registration. */
    public RegRecipeBuilder<R> serializer(Supplier<? extends RecipeSerializer<R>> factory) {
        this.serializerFactory = Objects.requireNonNull(factory);
        return this;
    }

    /** Registers a simple recipe type with the same id as the serializer. */
    public RegRecipeBuilder<R> type() {
        return type(() -> RecipeType.simple(ShadowsAndPetals.asResource(name)));
    }

    /** Registers a custom recipe type factory. */
    public RegRecipeBuilder<R> type(Supplier<? extends RecipeType<R>> factory) {
        this.typeFactory = Objects.requireNonNull(factory);
        return this;
    }

    /** Adds a generated recipe under this mod's namespace. */
    public RegRecipeBuilder<R> datagen(String path, Function<ModRecipeProvider, R> factory) {
        return datagen(ShadowsAndPetals.asResource(path), factory);
    }

    /** Adds a generated recipe with an explicit id. */
    public RegRecipeBuilder<R> datagen(Identifier id, Function<ModRecipeProvider, R> factory) {
        if (datagenRecipes.putIfAbsent(id, Objects.requireNonNull(factory)) != null) {
            throw new IllegalStateException("Duplicate recipe datagen entry for " + id);
        }
        return this;
    }

    /** Finalizes serializer, optional type, and datagen registration. */
    public RegisteredRecipe<R> register() {
        if (serializerFactory == null) {
            throw new IllegalStateException("Recipe serializer factory is required for '" + name + "'");
        }

        DeferredHolder<RecipeSerializer<?>, RecipeSerializer<R>> serializer =
                serializerRegistry.register(name, serializerFactory);
        DeferredHolder<RecipeType<?>, RecipeType<R>> type = typeFactory == null
                ? null
                : typeRegistry.register(name, typeFactory);

        for (var entry : datagenRecipes.entrySet()) {
            Identifier id = entry.getKey();
            Function<ModRecipeProvider, R> factory = entry.getValue();
            DatagenRecipeRegistry.add(id, provider -> provider.output().accept(
                    ResourceKey.create(Registries.RECIPE, id),
                    factory.apply(provider),
                    null
            ));
        }

        return new RegisteredRecipe<>(serializer, Optional.ofNullable(type));
    }

    public record RegisteredRecipe<R extends Recipe<?>>(
            DeferredHolder<RecipeSerializer<?>, RecipeSerializer<R>> serializer,
            Optional<DeferredHolder<RecipeType<?>, RecipeType<R>>> type
    ) {
    }
}
