package com.sshakusora.shadowsandpetals.worldgen;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.worldgen.feature.config.PrefabTreeConfiguration;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;
import java.util.OptionalInt;

public class SAPConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> SAKURA = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("sakura")
    );
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_SAKURA = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("fancy_sakura")
    );
    public static final ResourceKey<ConfiguredFeature<?, ?>> MAPLE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("maple")
    );
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_MAPLE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("fancy_maple")
    );
    public static final ResourceKey<ConfiguredFeature<?, ?>> GINKGO = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("ginkgo")
    );
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_GINKGO = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("fancy_ginkgo")
    );
    public static final ResourceKey<ConfiguredFeature<?, ?>> AUTUMN_OAK = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("autumn_oak")
    );
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_AUTUMN_OAK = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ShadowsAndPetals.asResource("fancy_autumn_oak")
    );
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

        context.register(SAKURA, new ConfiguredFeature<>(
                SAPFeatures.PREFAB_TREE.get(),
                createPrefabTree(
                        1, 0.12F, 0.26F,
                        ShadowsAndPetals.asResource("sakura/small_1"),
                        ShadowsAndPetals.asResource("sakura/small_2"),
                        ShadowsAndPetals.asResource("sakura/small_3"),
                        ShadowsAndPetals.asResource("sakura/middle_1"),
                        ShadowsAndPetals.asResource("sakura/middle_2")
                )
        ));
        context.register(FANCY_SAKURA, new ConfiguredFeature<>(
                SAPFeatures.PREFAB_TREE.get(),
                createPrefabTree(
                        1, 0.12F, 0.26F,
                        ShadowsAndPetals.asResource("sakura/large_1"),
                        ShadowsAndPetals.asResource("sakura/large_2"),
                        ShadowsAndPetals.asResource("sakura/large_3")
                )
        ));
        context.register(MAPLE, new ConfiguredFeature<>(
                SAPFeatures.PREFAB_TREE.get(),
                createPrefabTree(
                        1, 0.10F, 0.32F,
                        ShadowsAndPetals.asResource("maple/small_1"),
                        ShadowsAndPetals.asResource("maple/small_2"),
                        ShadowsAndPetals.asResource("maple/small_3"),
                        ShadowsAndPetals.asResource("maple/middle_1"),
                        ShadowsAndPetals.asResource("maple/middle_2"),
                        ShadowsAndPetals.asResource("maple/middle_3")
                )
        ));
        context.register(FANCY_MAPLE, new ConfiguredFeature<>(
                SAPFeatures.PREFAB_TREE.get(),
                createPrefabTree(
                        1, 0.10F, 0.32F,
                        ShadowsAndPetals.asResource("maple/large_1"),
                        ShadowsAndPetals.asResource("maple/large_2")
                )
        ));
        context.register(GINKGO, new ConfiguredFeature<>(
                SAPFeatures.PREFAB_TREE.get(),
                createPrefabTree(
                        1, 0.08F, 0.24F,
                        ShadowsAndPetals.asResource("ginkgo/small_1"),
                        ShadowsAndPetals.asResource("ginkgo/small_2"),
                        ShadowsAndPetals.asResource("ginkgo/small_3"),
                        ShadowsAndPetals.asResource("ginkgo/middle_1"),
                        ShadowsAndPetals.asResource("ginkgo/middle_2"),
                        ShadowsAndPetals.asResource("ginkgo/middle_3")
                )
        ));
        context.register(FANCY_GINKGO, new ConfiguredFeature<>(
                SAPFeatures.PREFAB_TREE.get(),
                createPrefabTree(
                        1, 0.08F, 0.24F,
                        ShadowsAndPetals.asResource("ginkgo/large_1"),
                        ShadowsAndPetals.asResource("ginkgo/large_2")
                )
        ));
        context.register(AUTUMN_OAK, new ConfiguredFeature<>(
                Feature.TREE,
                createAutumnOakTree(BlockRegistry.AUTUMN_OAK_LEAVES.get()).ignoreVines().build()
        ));
        context.register(FANCY_AUTUMN_OAK, new ConfiguredFeature<>(
                Feature.TREE,
                createFancyAutumnOakTree(BlockRegistry.AUTUMN_OAK_LEAVES.get()).build()
        ));
    }

    private static TreeConfiguration.TreeConfigurationBuilder createAutumnOakTree(Block leaves) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.OAK_LOG),
                new StraightTrunkPlacer(4, 2, 0),
                BlockStateProvider.simple(leaves),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        );
    }

    private static TreeConfiguration.TreeConfigurationBuilder createFancyAutumnOakTree(Block leaves) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.OAK_LOG),
                new FancyTrunkPlacer(3, 11, 0),
                BlockStateProvider.simple(leaves),
                new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4),
                new TwoLayersFeatureSize(0, 0, 0, OptionalInt.of(4))
        );
    }

    private static PrefabTreeConfiguration createPrefabTree(
            int leafCoreRadius,
            float leafSurfaceErosion,
            float leafNoiseScale,
            Identifier... templates
    ) {
        return new PrefabTreeConfiguration(
                List.of(templates),
                true,
                true,
                1,
                true,
                leafCoreRadius,
                leafSurfaceErosion,
                leafNoiseScale
        );
    }
}
