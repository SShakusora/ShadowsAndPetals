package com.sshakusora.shadowsandpetals.block.decoration.bonsai;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the blocks rendered by a bonsai from data-pack mappings.
 *
 * <p>Each JSON file under {@code data/<namespace>/bonsai_trees/} contains a
 * sapling block, a trunk block, and optionally a leaves block. The mapping
 * can also be supplied by another mod through {@link #register(Identifier,
 * Identifier, Identifier)}. When no explicit mapping exists, the server-side
 * resolver can inspect the sapling's {@link TreeGrower}
 * and sample standard {@link TreeConfiguration} providers without placing a
 * configured feature in the world.</p>
 */
@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class BonsaiTreeResolver extends SimpleJsonResourceReloadListener<BonsaiTreeResolver.Definition> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier DEFAULT_LEAVES = Identifier.withDefaultNamespace("oak_leaves");
    private static final FileToIdConverter FILES = FileToIdConverter.json("bonsai_trees");
    /** Codec shared by the runtime reload listener and the data generator. */
    public static final Codec<Definition> DEFINITION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("sapling").forGetter(Definition::sapling),
            Identifier.CODEC.fieldOf("trunk").forGetter(Definition::trunk),
            Identifier.CODEC.optionalFieldOf("leaves").forGetter(Definition::leaves)
    ).apply(instance, Definition::new));

    private static final BonsaiTreeResolver INSTANCE = new BonsaiTreeResolver();
    private static final Map<Identifier, Definition> API_MAPPINGS = new ConcurrentHashMap<>();
    private static final Set<Identifier> WARNED_FALLBACKS = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> WARNED_INFERENCE = ConcurrentHashMap.newKeySet();

    private static final int PROVIDER_SAMPLES = 32;
    private static final long TRUNK_SAMPLE_SEED = 0x5341505F5452554EL;
    private static final long LEAVES_SAMPLE_SEED = 0x5341505F4C454146L;

    private volatile Map<Identifier, Definition> dataMappings = Map.of();

    private BonsaiTreeResolver() {
        super(DEFINITION_CODEC, FILES);
    }

    /**
     * Registers a mapping for a sapling supplied by a mod. Data-pack files
     * take precedence, allowing a pack to correct or override an integration
     * without code changes.
     */
    public static void register(Identifier sapling, Identifier trunk, @Nullable Identifier leaves) {
        API_MAPPINGS.put(
                Objects.requireNonNull(sapling, "sapling"),
                new Definition(
                        sapling,
                        Objects.requireNonNull(trunk, "trunk"),
                        Optional.ofNullable(leaves)
                )
        );
    }

    /** Convenience overload for mods that already hold registered blocks. */
    public static void register(Block sapling, Block trunk, @Nullable Block leaves) {
        register(
                BuiltInRegistries.BLOCK.getKey(sapling),
                BuiltInRegistries.BLOCK.getKey(trunk),
                leaves == null ? null : BuiltInRegistries.BLOCK.getKey(leaves)
        );
    }

    @SubscribeEvent
    public static void addReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(ShadowsAndPetals.asResource("bonsai_trees"), INSTANCE);
    }

    @Override
    protected void apply(
            Map<Identifier, Definition> loaded,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<Identifier, Definition> bySapling = new HashMap<>();
        for (Definition definition : loaded.values()) {
            Definition previous = bySapling.put(definition.sapling(), definition);
            if (previous != null) {
                LOGGER.warn(
                        "Multiple bonsai mappings target sapling {}; using the last loaded definition",
                        definition.sapling()
                );
            }
        }
        dataMappings = Map.copyOf(bySapling);
        WARNED_FALLBACKS.clear();
        WARNED_INFERENCE.clear();
        LOGGER.debug("Loaded {} bonsai tree mappings", dataMappings.size());
    }

    /**
     * Resolves a sapling using the active data-pack/API mapping. This overload
     * is retained for callers that do not have a server level; automatic
     * feature inspection is available through {@link #resolve(ServerLevel,
     * BlockPos, Block)}.
     */
    public static Result resolve(Block saplingBlock, HolderLookup.@Nullable Provider registries) {
        if (!(saplingBlock instanceof SaplingBlock)) {
            return null;
        }

        Identifier saplingId = BuiltInRegistries.BLOCK.getKey(saplingBlock);
        Result resolved = resolveExplicit(saplingId);
        if (resolved != null) {
            return resolved;
        }

        return fallback(saplingId);
    }

    /**
     * Resolves a sapling for planting. Explicit data/API mappings win; when no
     * override exists, the sapling's TreeGrower is inspected and its standard
     * TreeConfiguration providers are sampled without placing a feature in the
     * world.
     */
    public static @Nullable Result resolve(ServerLevel level, BlockPos pos, Block saplingBlock) {
        if (!(saplingBlock instanceof SaplingBlock)) {
            return null;
        }

        Identifier saplingId = BuiltInRegistries.BLOCK.getKey(saplingBlock);
        Result explicit = resolveExplicit(saplingId);
        if (explicit != null) {
            return explicit;
        }

        Result inferred = inferFromTreeGrower(level, pos, (SaplingBlock) saplingBlock);
        if (inferred != null) {
            return inferred;
        }

        return fallback(saplingId);
    }

    private static @Nullable Result resolveExplicit(Identifier saplingId) {
        Definition definition = INSTANCE.dataMappings.get(saplingId);
        if (definition == null) {
            definition = API_MAPPINGS.get(saplingId);
        }
        return definition == null ? null : definition.resolveBlocks();
    }

    private static Result fallback(Identifier saplingId) {
        if (WARNED_FALLBACKS.add(saplingId)) {
            LOGGER.warn("No valid bonsai mapping or standard tree configuration for {}; falling back to oak", saplingId);
        }
        return new Result(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
    }

    private static @Nullable Result inferFromTreeGrower(
            ServerLevel level,
            BlockPos pos,
            SaplingBlock sapling
    ) {
        TreeGrower grower = sapling.treeGrower;
        var configuredFeatures = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        List<Result> candidates = new ArrayList<>();

        for (Optional<ResourceKey<ConfiguredFeature<?, ?>>> key : featureKeys(grower)) {
            if (key.isEmpty()) {
                continue;
            }

            var holder = configuredFeatures.get(key.get());
            if (holder.isEmpty() || !(holder.get().value().config() instanceof TreeConfiguration tree)) {
                continue;
            }

            Block trunk = sampleProvider(tree.trunkProvider, level, pos, TRUNK_SAMPLE_SEED);
            Block leaves = sampleProvider(tree.foliageProvider, level, pos, LEAVES_SAMPLE_SEED);
            if (isUsableBlock(trunk) && isUsableBlock(leaves)) {
                candidates.add(new Result(trunk, leaves));
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        Result selected = candidates.getFirst();
        for (int i = 1; i < candidates.size(); i++) {
            Result candidate = candidates.get(i);
            if (!selected.equals(candidate)) {
                Identifier saplingId = BuiltInRegistries.BLOCK.getKey(sapling);
                if (WARNED_INFERENCE.add(saplingId)) {
                    LOGGER.warn(
                            "TreeGrower for {} has differing trunk/foliage providers; using the first resolvable configuration",
                            saplingId
                    );
                }
                break;
            }
        }
        return selected;
    }

    private static List<Optional<ResourceKey<ConfiguredFeature<?, ?>>>> featureKeys(
            TreeGrower grower
    ) {
        return List.of(
                grower.tree,
                grower.secondaryTree,
                grower.flowers,
                grower.secondaryFlowers,
                grower.megaTree,
                grower.secondaryMegaTree
        );
    }

    private static @Nullable Block sampleProvider(
            BlockStateProvider provider,
            ServerLevel level,
            BlockPos pos,
            long seed
    ) {
        Map<Block, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < PROVIDER_SAMPLES; i++) {
            try {
                BlockState state = provider.getState(level, RandomSource.create(seed + i), pos);
                if (!state.isAir()) {
                    counts.merge(state.getBlock(), 1, Integer::sum);
                }
            } catch (RuntimeException exception) {
                LOGGER.debug("Could not sample bonsai block-state provider", exception);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static boolean isUsableBlock(@Nullable Block block) {
        return block != null && block != Blocks.AIR;
    }

    public record Definition(Identifier sapling, Identifier trunk, Optional<Identifier> leaves) {
        public Definition {
            Objects.requireNonNull(sapling, "sapling");
            Objects.requireNonNull(trunk, "trunk");
            Objects.requireNonNull(leaves, "leaves");
        }

        private @Nullable Result resolveBlocks() {
            Block trunkBlock = getRegisteredBlock(trunk);
            Block leavesBlock = getRegisteredBlock(leaves.orElse(DEFAULT_LEAVES));
            return trunkBlock == null || leavesBlock == null
                    ? null
                    : new Result(trunkBlock, leavesBlock);
        }
    }

    private static @Nullable Block getRegisteredBlock(Identifier id) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return block == Blocks.AIR ? null : block;
    }

    /** Resolved trunk and leaves blocks for a bonsai. */
    public record Result(Block trunkBlock, Block leavesBlock) {
    }
}
