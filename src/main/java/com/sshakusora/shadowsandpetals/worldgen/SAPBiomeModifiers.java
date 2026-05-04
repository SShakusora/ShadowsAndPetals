package com.sshakusora.shadowsandpetals.worldgen;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class SAPBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_ORE_BAUXITE_UPPER = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            ShadowsAndPetals.asResource("add_ore_bauxite_upper")
    );
    public static final ResourceKey<BiomeModifier> ADD_ORE_BAUXITE_MIDDLE = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            ShadowsAndPetals.asResource("add_ore_bauxite_middle")
    );
    public static final ResourceKey<BiomeModifier> ADD_ORE_BAUXITE_SMALL = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            ShadowsAndPetals.asResource("add_ore_bauxite_small")
    );

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        context.register(ADD_ORE_BAUXITE_UPPER, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(SAPPlacedFeatures.ORE_BAUXITE_UPPER)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));

        context.register(ADD_ORE_BAUXITE_MIDDLE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(SAPPlacedFeatures.ORE_BAUXITE_MIDDLE)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));

        context.register(ADD_ORE_BAUXITE_SMALL, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(SAPPlacedFeatures.ORE_BAUXITE_SMALL)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));
    }
}
