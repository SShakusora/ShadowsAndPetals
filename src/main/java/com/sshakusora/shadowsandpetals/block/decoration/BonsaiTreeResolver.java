package com.sshakusora.shadowsandpetals.block.decoration;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the trunk and leaf blocks for a given sapling by inspecting its
 * {@link TreeGrower}'s {@link ConfiguredFeature}. Falls back to oak for
 * growers that use non-{@link TreeConfiguration} features (e.g. SAP's prefab trees).
 */
public final class BonsaiTreeResolver {
    private BonsaiTreeResolver() {
    }

    /**
     * Resolves the trunk and leaf blocks for the given sapling block.
     * Requires a {@link Level} to access the configured-feature registry.
     *
     * @return a {@link Result} with the resolved blocks, or {@code null} if the
     *         sapling block is not a {@link SaplingBlock}.
     */
    public static @Nullable Result resolve(Block saplingBlock, @Nullable Level level) {
        if (!(saplingBlock instanceof SaplingBlock sapling)) {
            return null;
        }

        TreeGrower grower = sapling.treeGrower;
        if (grower == null) {
            return new Result(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        }

        // Try to resolve via TreeGrower -> ConfiguredFeature -> TreeConfiguration
        if (level != null) {
            var lookup = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
            ResourceKey<ConfiguredFeature<?, ?>> featureKey = grower.getConfiguredFeature(
                    net.minecraft.util.RandomSource.create(0L), false
            );
            if (featureKey != null) {
                var holderOpt = lookup.get(featureKey);
                if (holderOpt.isPresent()) {
                    Holder<ConfiguredFeature<?, ?>> holder = holderOpt.get();
                    ConfiguredFeature<?, ?> feature = holder.value();
                    if (feature.config() instanceof TreeConfiguration treeConfig) {
                        Block trunk = getBlockFromProvider(treeConfig.trunkProvider, Blocks.OAK_LOG);
                        Block leaves = getBlockFromProvider(treeConfig.foliageProvider, Blocks.OAK_LEAVES);
                        return new Result(trunk, leaves);
                    }
                }
            }
        }

        // Fallback for non-TreeConfiguration features (e.g. SAP prefab trees)
        return resolveSapBonsai(saplingBlock);
    }

    /**
     * Resolves the trunk and leaf blocks for SAP's prefab trees by matching
     * the sapling block against known wood sets.
     */
    private static Result resolveSapBonsai(Block saplingBlock) {
        Identifier saplingId = BuiltInRegistries.BLOCK.getKey(saplingBlock);
        String path = saplingId.getPath();

        // Check if this is one of SAP's wood set saplings (sakura_sapling, maple_sapling, etc.)
        for (var woodType : com.sshakusora.shadowsandpetals.block.WoodSetList.Type.values()) {
            if (path.equals(woodType.getName() + "_sapling")) {
                var woodSet = com.sshakusora.shadowsandpetals.registries.BlockRegistry.WOOD_SETS.get(woodType);
                return new Result(woodSet.log().get(), woodSet.leaves().get());
            }
        }

        // Check autumn oak sapling
        if (path.equals("autumn_oak_sapling")) {
            return new Result(Blocks.OAK_LOG, com.sshakusora.shadowsandpetals.registries.BlockRegistry.AUTUMN_OAK_LEAVES.get());
        }

        // Ultimate fallback
        return new Result(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
    }

    private static Block getBlockFromProvider(BlockStateProvider provider, Block fallback) {
        if (provider instanceof SimpleStateProvider simple) {
            BlockState state = simple.state;
            return state.getBlock();
        }
        return fallback;
    }

    /**
     * Resolved trunk and leaf blocks for a bonsai.
     */
    public record Result(Block trunkBlock, Block leavesBlock) {
    }
}