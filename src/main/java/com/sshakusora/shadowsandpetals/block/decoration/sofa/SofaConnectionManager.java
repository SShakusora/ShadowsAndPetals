package com.sshakusora.shadowsandpetals.block.decoration.sofa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Bridges the pure sofa planner with a live server level. */
final class SofaConnectionManager {
    private static final int SHAPE_UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Map<ServerLevel, Map<BlockPos, PendingPlacement>> PENDING_PLACEMENTS =
            new WeakHashMap<>();

    private SofaConnectionManager() {
    }

    static void reconcileAfterPlacement(ServerLevel level, BlockPos placedPos) {
        BlockState state = level.getBlockState(placedPos);
        if (!(state.getBlock() instanceof SofaBlock)) {
            return;
        }
        reconcile(level, placedPos);
        BlockState reconciledState = level.getBlockState(placedPos);
        if (reconciledState.getBlock() instanceof SofaBlock) {
            rememberPlacement(level, placedPos, reconciledState);
        }
    }

    /**
     * Completes the item-placement lifecycle. BlockItem invokes onPlace before
     * applying a possible BLOCK_STATE component, so a matching post-onPlace
     * state means onPlace already performed the complete reconciliation.
     */
    static void reconcileAfterItemPlacement(
            ServerLevel level,
            BlockPos placedPos,
            BlockState placedState
    ) {
        PendingPlacement pending = takePendingPlacement(level, placedPos);
        if (pending != null && pending.matches(placedState)) {
            return;
        }
        reconcile(level, placedPos);
    }

    static void reconcileAfterRemoval(ServerLevel level, BlockPos removedPos) {
        takePendingPlacement(level, removedPos);
        apply(level, SofaConnectionPlanner.planRemoval(layout(level), removedPos));
    }

    private static void reconcile(ServerLevel level, BlockPos placedPos) {
        apply(level, SofaConnectionPlanner.planPlacement(layout(level), placedPos));
    }

    private static void rememberPlacement(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        Map<BlockPos, PendingPlacement> pending = pendingFor(level);
        pending.put(pos.immutable(), new PendingPlacement(
                level.getGameTime(),
                state.getValue(SofaBlock.FACING),
                state.getValue(SofaBlock.SHAPE)));
    }

    private static @Nullable PendingPlacement takePendingPlacement(
            ServerLevel level,
            BlockPos pos
    ) {
        return pendingFor(level).remove(pos);
    }

    private static Map<BlockPos, PendingPlacement> pendingFor(ServerLevel level) {
        Map<BlockPos, PendingPlacement> pending = PENDING_PLACEMENTS.computeIfAbsent(
                level,
                ignored -> new HashMap<>());
        long currentTick = level.getGameTime();
        pending.entrySet().removeIf(entry -> entry.getValue().tick() != currentTick);
        return pending;
    }

    private static SofaConnectionPlanner.SofaLayout layout(BlockGetter level) {
        return pos -> {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SofaBlock)) {
                return null;
            }
            return new SofaConnectionPlanner.SofaState(
                    state.getValue(SofaBlock.FACING),
                    state.getValue(SofaBlock.SHAPE));
        };
    }

    private static void apply(
            ServerLevel level,
            Map<BlockPos, SofaBlock.SofaShape> shapes
    ) {
        shapes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingLong(BlockPos::asLong)))
                .forEach(entry -> {
                    BlockPos pos = entry.getKey();
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof SofaBlock
                            && state.getValue(SofaBlock.SHAPE) != entry.getValue()) {
                        level.setBlock(
                                pos,
                                state.setValue(SofaBlock.SHAPE, entry.getValue()),
                                SHAPE_UPDATE_FLAGS);
                    }
                });
    }

    private record PendingPlacement(
            long tick,
            Direction facing,
            SofaBlock.SofaShape shape
    ) {
        private boolean matches(BlockState state) {
            return state.getBlock() instanceof SofaBlock
                    && state.getValue(SofaBlock.FACING) == facing
                    && state.getValue(SofaBlock.SHAPE) == shape;
        }
    }
}
