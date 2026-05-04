package com.sshakusora.shadowsandpetals.worldgen;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class SAPConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_BAUXITE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("ore_bauxite")
    );
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_BAUXITE_SMALL = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("ore_bauxite_small")
    );

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        List<OreConfiguration.TargetBlockState> bauxiteOres = List.of(
                OreConfiguration.target(stoneReplaceables, BlockRegistry.BAUXITE_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, BlockRegistry.DEEPSLATE_BAUXITE_ORE.get().defaultBlockState())
        );

        context.register(ORE_BAUXITE, new ConfiguredFeature<>(
                Feature.ORE, new OreConfiguration(bauxiteOres, 8)
        ));
        context.register(ORE_BAUXITE_SMALL, new ConfiguredFeature<>(
                Feature.ORE, new OreConfiguration(bauxiteOres, 4)
        ));
    }
}
