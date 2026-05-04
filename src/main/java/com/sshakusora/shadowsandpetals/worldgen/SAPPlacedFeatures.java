package com.sshakusora.shadowsandpetals.worldgen;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class SAPPlacedFeatures {
    public static final ResourceKey<PlacedFeature> ORE_BAUXITE_UPPER = ResourceKey.create(
            Registries.PLACED_FEATURE,
            ShadowsAndPetals.asResource("ore_bauxite_upper")
    );
    public static final ResourceKey<PlacedFeature> ORE_BAUXITE_MIDDLE = ResourceKey.create(
            Registries.PLACED_FEATURE,
            ShadowsAndPetals.asResource("ore_bauxite_middle")
    );
    public static final ResourceKey<PlacedFeature> ORE_BAUXITE_SMALL = ResourceKey.create(
            Registries.PLACED_FEATURE,
            ShadowsAndPetals.asResource("ore_bauxite_small")
    );

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        Holder<ConfiguredFeature<?, ?>> oreBauxite = configuredFeatures.getOrThrow(SAPConfiguredFeatures.ORE_BAUXITE);
        Holder<ConfiguredFeature<?, ?>> oreBauxiteSmall = configuredFeatures.getOrThrow(SAPConfiguredFeatures.ORE_BAUXITE_SMALL);

        context.register(ORE_BAUXITE_UPPER, new PlacedFeature(oreBauxite,
                commonOrePlacement(8, HeightRangePlacement.triangle(
                        VerticalAnchor.absolute(64), VerticalAnchor.absolute(128)))));

        context.register(ORE_BAUXITE_MIDDLE, new PlacedFeature(oreBauxite,
                commonOrePlacement(8, HeightRangePlacement.triangle(
                        VerticalAnchor.absolute(-16), VerticalAnchor.absolute(60)))));

        context.register(ORE_BAUXITE_SMALL, new PlacedFeature(oreBauxiteSmall,
                commonOrePlacement(8, HeightRangePlacement.uniform(
                        VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))));
    }

    private static List<PlacementModifier> commonOrePlacement(int count, PlacementModifier placement) {
        return List.of(CountPlacement.of(count), InSquarePlacement.spread(), placement, BiomeFilter.biome());
    }
}
