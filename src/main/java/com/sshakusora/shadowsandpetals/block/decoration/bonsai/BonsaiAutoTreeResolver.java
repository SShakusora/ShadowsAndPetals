package com.sshakusora.shadowsandpetals.block.decoration.bonsai;

import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatic tree resolution for the bonsai pot, modelled on the way Applied
 * Energistics 2 discovers facade-compatible blocks from arbitrary mods:
 * instead of maintaining per-mod integrations, derive the tree's trunk and
 * leaves directly from the vanilla tree pipeline that every standard sapling
 * already participates in.
 *
 * <p>Resolution chain: {@code SaplingBlock.treeGrower} → the grower's
 * {@code tree} (fallback {@code megaTree}) {@code ConfiguredFeature} key →
 * {@link TreeConfiguration#trunkProvider}/{@link TreeConfiguration#foliageProvider}
 * → a representative sampled block. Data-pack and API mappings registered
 * through {@link BonsaiTreeResolver} keep precedence; this resolver only
 * fills the gap for mods that were never explicitly integrated. Non-standard
 * features (vanilla azalea flowers, bamboo, custom prefab trees without a
 * {@link TreeConfiguration}) return no result and fall back to the existing
 * mapping system.</p>
 */
public final class BonsaiAutoTreeResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Cache of one resolved definition per sapling block, keyed by registry
     * id. Negative results (no resolvable tree) are cached too, so a modded
     * sapling without a standard tree feature does not trigger a registry
     * lookup on every interaction. Entries survive registry reloads because
     * they are only reachable through a live registry access.
     */
    private static final Map<Identifier, Optional<BonsaiTreeResolver.Result>> CACHE =
            new ConcurrentHashMap<>();
    /** Providers are sampled once per cache miss; sampling is deterministic. */
    private static final RandomSource SAMPLE_RANDOM = RandomSource.create(42L);

    private BonsaiAutoTreeResolver() {
    }

    /**
     * Resolves the trunk and leaves blocks a sapling would grow into, using
     * the vanilla tree pipeline. Returns {@code null} when the sapling does
     * not participate in the standard {@link TreeGrower} → tree feature
     * chain, when registries are unavailable, or when the configured feature
     * is missing from the given registry access.
     */
    public static BonsaiTreeResolver.@Nullable Result resolve(
            @Nullable Block saplingBlock,
            HolderLookup.@Nullable Provider registries
    ) {
        if (saplingBlock == null || registries == null) {
            return null;
        }
        if (!(saplingBlock instanceof SaplingBlock)) {
            return null;
        }

        Identifier saplingId = BuiltInRegistries.BLOCK.getKey(saplingBlock);
        Optional<BonsaiTreeResolver.Result> cached = CACHE.get(saplingId);
        if (cached != null) {
            return cached.orElse(null);
        }

        BonsaiTreeResolver.Result resolved = resolveUncached(saplingBlock, registries);
        CACHE.put(saplingId, Optional.ofNullable(resolved));
        if (resolved == null) {
            LOGGER.debug("No standard tree feature for sapling {}", saplingId);
        }
        return resolved;
    }

    private static BonsaiTreeResolver.@Nullable Result resolveUncached(
            Block saplingBlock,
            HolderLookup.Provider registries
    ) {
        SaplingBlock sapling = (SaplingBlock) saplingBlock;
        TreeGrower grower = sapling.treeGrower;
        if (grower == null) {
            return null;
        }

        // Prefer the regular tree variant; mega variants share the same wood
        // set in practice, and the regular variant matches what a single
        // sapling grows into most of the time.
        BonsaiTreeResolver.Result result = resolveFeature(grower.tree, registries);
        if (result == null) {
            result = resolveFeature(grower.megaTree, registries);
        }
        return result;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static BonsaiTreeResolver.@Nullable Result resolveFeature(
            Optional<ResourceKey<ConfiguredFeature<?, ?>>> featureKey,
            HolderLookup.Provider registries
    ) {
        if (featureKey.isEmpty()) {
            return null;
        }

        HolderLookup.RegistryLookup<ConfiguredFeature<?, ?>> registry =
                registries.lookupOrThrow(Registries.CONFIGURED_FEATURE);
        Holder.Reference<ConfiguredFeature<?, ?>> holder = registry.get(featureKey.get()).orElse(null);
        if (holder == null) {
            return null;
        }

        ConfiguredFeature<?, ?> feature = holder.value();
        return extractFromConfiguration(feature.config());
    }

    /**
     * Extracts the representative trunk and leaves blocks from a configured
     * feature's configuration. Package-visible so the resolution logic can
     * be unit-tested without a bootstrapped registry; returns {@code null}
     * for anything that is not a standard {@link TreeConfiguration}.
     */
    static BonsaiTreeResolver.@Nullable Result extractFromConfiguration(
            net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration config
    ) {
        if (!(config instanceof TreeConfiguration treeConfig)) {
            // Flower features, bamboo, and custom prefab configurations do
            // not expose providers; leave them to explicit mappings.
            return null;
        }

        Block trunk = representativeBlock(treeConfig.trunkProvider);
        Block leaves = representativeBlock(treeConfig.foliageProvider);
        if (trunk == null || leaves == null) {
            return null;
        }
        return new BonsaiTreeResolver.Result(trunk, leaves);
    }

    /**
     * Samples one state from the provider and keeps only the block. The
     * renderer resolves materials and tints from the block's default state,
     * so property-rich samples (axis rotation, distance, waterlogged) would
     * only risk mismatched keys.
     *
     * <p>The patched {@code BlockStateProvider#getState} expects a
     * {@code WorldGenLevel}, but the providers used by tree configurations
     * (simple, weighted, rotated) never read it; null is passed and any
     * provider that does touch a level fails closed into the explicit
     * mapping fallback instead of crashing the interaction.</p>
     */
    private static @Nullable Block representativeBlock(BlockStateProvider provider) {
        try {
            return provider.getState(null, SAMPLE_RANDOM, BlockPos.ZERO).getBlock();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Drops the per-sapling resolution cache. */
    public static void invalidateCache() {
        CACHE.clear();
    }
}