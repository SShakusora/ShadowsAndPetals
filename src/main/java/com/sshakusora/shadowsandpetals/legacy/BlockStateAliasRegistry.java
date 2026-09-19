package com.sshakusora.shadowsandpetals.legacy;

import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class BlockStateAliasRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Rule> RULES = new ArrayList<>();
    private static final Map<Block, Rule> RULES_BY_LEGACY_BLOCK = new IdentityHashMap<>();
    private static boolean ruleIndexDirty = true;

    private BlockStateAliasRegistry() {}

    public static synchronized void add(
            Supplier<? extends Block> legacyBlock,
            Supplier<BlockState> targetState,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {
        RULES.add(new Rule(legacyBlock, targetState, converter));
        ruleIndexDirty = true;
    }

    // State conversion must run before the block-entity alias pass.  The latter validates the
    // target BlockEntityType against the post-migration state, so leaving this at NORMAL can make
    // a legacy Tansu entity see its temporary compatibility block instead of the new vanity.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || chunk.getLevel().isClientSide()) {
            return;
        }

        migrateChunk(chunk);
    }

    /**
     * Applies all registered state aliases to a loaded chunk.  This is public so the block-entity
     * migration pass can force the state conversion first on NeoForge versions that dispatch the
     * LOWEST chunk listener before the HIGHEST listener during chunk deserialization.
     */
    public static void migrateChunk(LevelChunk chunk) {
        if (chunk.getLevel().isClientSide()) {
            return;
        }

        ensureRuleIndex();
        Map<Block, Rule> rulesByLegacyBlock = RULES_BY_LEGACY_BLOCK;
        if (rulesByLegacyBlock.isEmpty()) {
            return;
        }

        long startedAt = System.nanoTime();
        int candidateSections = 0;
        int scannedSections = 0;
        int migratedBlocks = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Map<Rule, BlockState> targetStates = new IdentityHashMap<>();
        LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || !section.maybeHas(state -> rulesByLegacyBlock.containsKey(state.getBlock()))) {
                continue;
            }

            candidateSections++;
            int sectionMinY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            int sectionMaxY = Math.min(sectionMinY + LevelChunkSection.SECTION_HEIGHT, chunk.getLevel().getMaxBuildHeight());
            int sectionMinYClamped = Math.max(sectionMinY, chunk.getLevel().getMinBuildHeight());
            scannedSections++;

            for (int localY = sectionMinYClamped - sectionMinY; localY < sectionMaxY - sectionMinY; localY++) {
                for (int localZ = 0; localZ < LevelChunkSection.SECTION_WIDTH; localZ++) {
                    for (int localX = 0; localX < LevelChunkSection.SECTION_WIDTH; localX++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        Rule rule = rulesByLegacyBlock.get(state.getBlock());
                        if (rule == null) {
                            continue;
                        }

                        BlockState targetState = targetStates.computeIfAbsent(rule, ignored -> rule.targetState.get());
                        BlockState replacement = rule.converter.apply(state, targetState);
                        if (replacement == null || replacement == state) {
                            continue;
                        }

                        pos.set(
                                chunk.getPos().getMinBlockX() + localX,
                                sectionMinY + localY,
                                chunk.getPos().getMinBlockZ() + localZ
                        );
                        if (chunk.setBlockState(pos, replacement, false) != null) {
                            migratedBlocks++;
                        }
                    }
                }
            }
        }

        if (migratedBlocks > 0) {
            // LevelChunk#setBlockState normally sets this flag itself. Keep the explicit mark here
            // because migration is a load-time transformation and must survive every future
            // implementation change in the chunk mutation path.
            chunk.setUnsaved(true);
        }

        long elapsedNanos = System.nanoTime() - startedAt;
        LOGGER.debug(
                "Processed block-state aliases for chunk {}: {} migrated blocks, {} candidate sections, {} scanned sections in {} ms",
                chunk.getPos(),
                migratedBlocks,
                candidateSections,
                scannedSections,
                elapsedNanos / 1_000_000.0D
        );
    }

    private static synchronized void ensureRuleIndex() {
        if (!ruleIndexDirty) {
            return;
        }

        RULES_BY_LEGACY_BLOCK.clear();
        for (Rule rule : RULES) {
            Block legacyBlock = rule.legacyBlock.get();
            Rule previous = RULES_BY_LEGACY_BLOCK.putIfAbsent(legacyBlock, rule);
            if (previous != null) {
                // Preserve the old list-based behavior (the first rule wins) while making
                // duplicate registrations visible during development.
                LOGGER.warn("Duplicate block-state alias registered for {}; keeping the first rule", legacyBlock);
            }
        }

        ruleIndexDirty = false;
    }

    private record Rule(
            Supplier<? extends Block> legacyBlock,
            Supplier<BlockState> targetState,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {}
}
