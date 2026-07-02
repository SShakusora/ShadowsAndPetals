package com.sshakusora.shadowsandpetals.worldgen.feature;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.sshakusora.shadowsandpetals.worldgen.feature.config.PrefabTreeConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PrefabTreeFeature extends Feature<PrefabTreeConfiguration> {
    private static final Rotation[] ROTATIONS = new Rotation[]{
            Rotation.NONE,
            Rotation.CLOCKWISE_90,
            Rotation.CLOCKWISE_180,
            Rotation.COUNTERCLOCKWISE_90
    };
    private static final int BLOCK_UPDATE_FLAGS = 19;
    private static final int EDGE_UPDATE_FLAGS = 3;

    public PrefabTreeFeature(Codec<PrefabTreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<PrefabTreeConfiguration> context) {
        PrefabTreeConfiguration config = context.config();
        if (config.templates().isEmpty()) {
            return false;
        }

        WorldGenLevel level = context.level();
        ServerLevel serverLevel = level.getLevel();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        Identifier templateId = config.templates().get(random.nextInt(config.templates().size()));
        Optional<StructureTemplate> optionalTemplate = serverLevel.getStructureManager().get(templateId);
        if (optionalTemplate.isEmpty()) {
            return false;
        }

        StructureTemplate template = optionalTemplate.get();
        Rotation rotation = config.allowRotation() ? ROTATIONS[random.nextInt(ROTATIONS.length)] : Rotation.NONE;
        Mirror mirror = pickMirror(random, config.allowMirror());
        BlockPos localAnchor = getTemplateAnchor(template);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(mirror)
                .setRotationPivot(localAnchor)
                .setIgnoreEntities(true)
                .setKnownShape(false)
                .setRandom(random)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                .addProcessor(new SkipBlockedLeavesProcessor(level));

        BlockPos placementOrigin = alignTemplateToSapling(settings, localAnchor, origin);
        BoundingBox boundingBox = template.getBoundingBox(settings, placementOrigin);
        if (!hasRoomForTrunk(level, template, settings, placementOrigin, origin)) {
            return false;
        }

        if (!template.placeInWorld(level, placementOrigin, placementOrigin, settings, random, BLOCK_UPDATE_FLAGS)) {
            return false;
        }

        Set<BlockPos> logs = Sets.newHashSet();
        Set<BlockPos> leaves = Sets.newHashSet();
        collectTreeBlocks(level, boundingBox, logs, leaves);
        if (logs.isEmpty()) {
            return false;
        }

        extendBaseLogs(level, logs, config.trunkBaseExtensionMax());

        if (config.updateLeafDistance()) {
            collectTreeBlocks(level, boundingBox, logs, leaves);
            List<BlockPos> allPlacedBlocks = new ArrayList<>(logs);
            allPlacedBlocks.addAll(leaves);
            Optional<BoundingBox> updatedBounds = BoundingBox.encapsulatingPositions(allPlacedBlocks);
            if (updatedBounds.isPresent()) {
                DiscreteVoxelShape shape = TreeFeature.updateLeaves(level, updatedBounds.get(), logs, Set.of(), Set.of());
                StructureTemplate.updateShapeAtEdge(level, EDGE_UPDATE_FLAGS, shape, updatedBounds.get().minX(), updatedBounds.get().minY(), updatedBounds.get().minZ());
            }
        }

        return true;
    }

    private static Mirror pickMirror(RandomSource random, boolean allowMirror) {
        if (!allowMirror || !random.nextBoolean()) {
            return Mirror.NONE;
        }

        return random.nextBoolean() ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
    }

    private static BlockPos getTemplateAnchor(StructureTemplate template) {
        return new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
    }

    private static BlockPos alignTemplateToSapling(StructurePlaceSettings settings, BlockPos localAnchor, BlockPos saplingPos) {
        BlockPos transformedAnchor = StructureTemplate.calculateRelativePosition(settings, localAnchor);
        return saplingPos.offset(-transformedAnchor.getX(), -transformedAnchor.getY(), -transformedAnchor.getZ());
    }

    private static boolean hasRoomForTrunk(
            WorldGenLevel level,
            StructureTemplate template,
            StructurePlaceSettings settings,
            BlockPos placementOrigin,
            BlockPos treeOrigin
    ) {
        StructurePlaceSettings preflightSettings = settings.copy().setRandom(null);
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!block.defaultBlockState().is(BlockTags.LOGS)) {
                continue;
            }

            for (StructureTemplate.StructureBlockInfo blockInfo : template.filterBlocks(placementOrigin, preflightSettings, block)) {
                BlockPos pos = blockInfo.pos();
                if (!level.ensureCanWrite(pos) || !level.isInsideBuildHeight(pos)) {
                    return false;
                }

                if (!pos.equals(treeOrigin) && isBlockedForTree(level.getBlockState(pos))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isBlockedForTree(BlockState state) {
        return !(state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.LEAVES) || state.canBeReplaced());
    }

    private static void collectTreeBlocks(LevelAccessor level, BoundingBox bounds, Set<BlockPos> logs, Set<BlockPos> leaves) {
        logs.clear();
        leaves.clear();

        for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS)) {
                logs.add(pos.immutable());
            } else if (state.is(BlockTags.LEAVES)) {
                leaves.add(pos.immutable());
            }
        }
    }

    private static void extendBaseLogs(LevelAccessor level, Set<BlockPos> logs, int maxExtension) {
        if (maxExtension <= 0 || logs.isEmpty()) {
            return;
        }

        List<BlockPos> baseLogs = getBaseLogs(level, logs);

        for (BlockPos baseLog : baseLogs) {
            BlockState logState = level.getBlockState(baseLog);
            BlockPos.MutableBlockPos cursor = baseLog.below().mutable();
            for (int depth = 0; depth < maxExtension && cursor.getY() >= level.getMinY(); depth++) {
                BlockState currentState = level.getBlockState(cursor);
                if (isBlockedForTree(currentState)) {
                    break;
                }

                level.setBlock(cursor, logState, BLOCK_UPDATE_FLAGS);
                logs.add(cursor.immutable());
                cursor.move(0, -1, 0);
            }
        }
    }

    private static List<BlockPos> getBaseLogs(LevelAccessor level, Set<BlockPos> logs) {
        List<BlockPos> baseLogs = new ArrayList<>();
        for (BlockPos pos : logs) {
            if (!level.getBlockState(pos.below()).is(BlockTags.LOGS)) {
                baseLogs.add(pos);
            }
        }

        return baseLogs;
    }

    private static final class SkipBlockedLeavesProcessor extends StructureProcessor {
        private final WorldGenLevel level;

        private SkipBlockedLeavesProcessor(WorldGenLevel level) {
            this.level = level;
        }

        @Override
        public StructureTemplate.@Nullable StructureBlockInfo process(
                LevelReader ignoredLevel,
                BlockPos targetPosition,
                BlockPos referencePos,
                StructureTemplate.StructureBlockInfo originalBlockInfo,
                StructureTemplate.StructureBlockInfo processedBlockInfo,
                StructurePlaceSettings settings,
                @Nullable StructureTemplate template
        ) {
            if (!processedBlockInfo.state().is(BlockTags.LEAVES)) {
                return processedBlockInfo;
            }

            BlockPos pos = processedBlockInfo.pos();
            if (!this.level.ensureCanWrite(pos)
                    || !this.level.isInsideBuildHeight(pos)
                    || isBlockedForTree(this.level.getBlockState(pos))) {
                return null;
            }

            return processedBlockInfo;
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return StructureProcessorType.NOP;
        }
    }
}
