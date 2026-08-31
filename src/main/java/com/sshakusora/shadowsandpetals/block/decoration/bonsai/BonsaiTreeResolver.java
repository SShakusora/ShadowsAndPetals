package com.sshakusora.shadowsandpetals.block.decoration.bonsai;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
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
 * Identifier, Identifier)}. This avoids reaching into the private details
 * of {@link net.minecraft.world.level.block.grower.TreeGrower}; feature
 * providers are not guaranteed to expose one simple trunk or foliage block.</p>
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
        LOGGER.debug("Loaded {} bonsai tree mappings", dataMappings.size());
    }

    /**
     * Resolves a sapling using the active data-pack/API mapping. Resolution is
     * deliberately registry-based and does not depend on a random configured
     * feature.
     */
    public static @Nullable Result resolve(Block saplingBlock) {
        if (!(saplingBlock instanceof SaplingBlock)) {
            return null;
        }

        Identifier saplingId = BuiltInRegistries.BLOCK.getKey(saplingBlock);
        Definition definition = INSTANCE.dataMappings.get(saplingId);
        if (definition == null) {
            definition = API_MAPPINGS.get(saplingId);
        }

        Result resolved = definition == null ? null : definition.resolveBlocks();
        if (resolved != null) {
            return resolved;
        }

        if (WARNED_FALLBACKS.add(saplingId)) {
            LOGGER.warn("No valid bonsai mapping for {}; falling back to oak", saplingId);
        }
        return new Result(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
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
