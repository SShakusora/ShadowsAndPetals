package com.sshakusora.shadowsandpetals.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class SAPTreeGrowers {
    private SAPTreeGrowers() { }

    public static final TreeGrower SAKURA = create(
            "grower_sakura",
            0.1F,
            Optional.empty(),
            Optional.empty(),
            Optional.of(SAPConfiguredFeatures.SAKURA),
            Optional.of(SAPConfiguredFeatures.FANCY_SAKURA),
            Optional.empty(),
            Optional.empty()
    );

    public static final TreeGrower MAPLE = create(
            "grower_maple",
            0.05F,
            Optional.empty(),
            Optional.empty(),
            Optional.of(SAPConfiguredFeatures.MAPLE),
            Optional.of(SAPConfiguredFeatures.FANCY_MAPLE),
            Optional.empty(),
            Optional.empty()
    );

    public static final TreeGrower GINKGO = create(
            "grower_ginkgo",
            0.1F,
            Optional.empty(),
            Optional.empty(),
            Optional.of(SAPConfiguredFeatures.GINKGO),
            Optional.of(SAPConfiguredFeatures.FANCY_GINKGO),
            Optional.empty(),
            Optional.empty()
    );

    public static final TreeGrower AUTUMN_OAK = create(
            "grower_autumn_oak",
            0.1F,
            Optional.empty(),
            Optional.empty(),
            Optional.of(SAPConfiguredFeatures.AUTUMN_OAK),
            Optional.of(SAPConfiguredFeatures.FANCY_AUTUMN_OAK),
            Optional.empty(),
            Optional.empty()
    );

    public static TreeGrower create(
            String name,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> megaTree,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> tree,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> flowers
    ) {
        return new TreeGrower(name, megaTree, tree, flowers);
    }

    public static TreeGrower create(
            String name,
            float secondaryChance,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> megaTree,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryMegaTree,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> tree,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryTree,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> flowers,
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryFlowers
    ) {
        return new TreeGrower(
                name,
                secondaryChance,
                megaTree,
                secondaryMegaTree,
                tree,
                secondaryTree,
                flowers,
                secondaryFlowers
        );
    }
}
