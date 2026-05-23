package com.sshakusora.shadowsandpetals.worldgen;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
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
                Feature.TREE,
                createStraightTree(BlockRegistry.SAKURA_SET.log().get(), BlockRegistry.SAKURA_SET.leaves().get(), 4, 2, 0, 2).build()
        ));
        context.register(FANCY_SAKURA, new ConfiguredFeature<>(
                Feature.TREE,
                createFancyTree(BlockRegistry.SAKURA_SET.log().get(), BlockRegistry.SAKURA_SET.leaves().get()).build()
        ));
        context.register(MAPLE, new ConfiguredFeature<>(
                Feature.TREE,
                createStraightTree(BlockRegistry.MAPLE_SET.log().get(), BlockRegistry.MAPLE_SET.leaves().get(), 4, 2, 0, 2).ignoreVines().build()
        ));
        context.register(FANCY_MAPLE, new ConfiguredFeature<>(
                Feature.TREE,
                createFancyTree(BlockRegistry.MAPLE_SET.log().get(), BlockRegistry.MAPLE_SET.leaves().get()).build()
        ));
        context.register(GINKGO, new ConfiguredFeature<>(
                Feature.TREE,
                createStraightTree(BlockRegistry.GINKGO_SET.log().get(), BlockRegistry.GINKGO_SET.leaves().get(), 4, 2, 0, 2).ignoreVines().build()
        ));
        context.register(FANCY_GINKGO, new ConfiguredFeature<>(
                Feature.TREE,
                createFancyTree(BlockRegistry.GINKGO_SET.log().get(), BlockRegistry.GINKGO_SET.leaves().get()).build()
        ));
        context.register(AUTUMN_OAK, new ConfiguredFeature<>(
                Feature.TREE,
                createStraightTree(Blocks.OAK_LOG, BlockRegistry.AUTUMN_OAK_LEAVES.get(), 4, 2, 0, 2).ignoreVines().build()
        ));
        context.register(FANCY_AUTUMN_OAK, new ConfiguredFeature<>(
                Feature.TREE,
                createFancyTree(Blocks.OAK_LOG, BlockRegistry.AUTUMN_OAK_LEAVES.get()).build()
        ));
    }

    private static TreeConfiguration.TreeConfigurationBuilder createStraightTree(Block log, Block leaves, int baseHeight, int heightRandA, int heightRandB, int foliageRadius) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(log),
                new StraightTrunkPlacer(baseHeight, heightRandA, heightRandB),
                BlockStateProvider.simple(leaves),
                new BlobFoliagePlacer(ConstantInt.of(foliageRadius), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        );
    }

    private static TreeConfiguration.TreeConfigurationBuilder createFancyTree(Block log, Block leaves) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(log),
                new FancyTrunkPlacer(3, 11, 0),
                BlockStateProvider.simple(leaves),
                new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4),
                new TwoLayersFeatureSize(0, 0, 0, OptionalInt.of(4))
        );
    }
}
