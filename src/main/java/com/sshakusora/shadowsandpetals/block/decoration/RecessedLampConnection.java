package com.sshakusora.shadowsandpetals.block.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Shared connection rules for the two recessed-lamp block implementations. */
public final class RecessedLampConnection {
    private static final int HALF_PIXEL_SCALE = 2;

    private RecessedLampConnection() {
    }

    public static boolean isLamp(BlockState state) {
        return state.getBlock() instanceof RecessedLampBlock
                || state.getBlock() instanceof RecessedLampCompositeBlock;
    }

    public static Direction face(BlockState state) {
        return state.getValue(RecessedLampBlock.MOUNT).face();
    }

    /** Returns the lamp face height in half-pixels relative to its block position. */
    public static int localSurfaceHeight(BlockState state) {
        RecessedLampBlock.Mount mount = state.getValue(RecessedLampBlock.MOUNT);
        boolean composite = state.getBlock() instanceof RecessedLampCompositeBlock;
        return switch (mount) {
            case FLOOR -> 1;
            case FLOOR_SLAB -> composite ? 17 : -15;
            case CEILING -> 31;
            case CEILING_SLAB -> composite ? 15 : 47;
        };
    }

    public static int worldSurfaceHeight(BlockPos pos, BlockState state) {
        return pos.getY() * 16 * HALF_PIXEL_SCALE + localSurfaceHeight(state);
    }

    public static boolean canConnect(BlockPos firstPos, BlockState first,
                                     BlockPos secondPos, BlockState second) {
        return isLamp(second)
                && face(first) == face(second)
                && worldSurfaceHeight(firstPos, first) == worldSurfaceHeight(secondPos, second);
    }

    public static boolean hasConnection(BlockGetter level, BlockPos pos, BlockState state,
                                        Direction direction) {
        if (!direction.getAxis().isHorizontal()) {
            return false;
        }
        BlockPos horizontal = pos.relative(direction);
        for (int yOffset = -1; yOffset <= 1; yOffset++) {
            BlockPos candidatePos = horizontal.offset(0, yOffset, 0);
            if (canConnect(pos, state, candidatePos, level.getBlockState(candidatePos))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasDiagonalConnection(BlockGetter level, BlockPos pos, BlockState state,
                                                Direction first, Direction second) {
        if (!first.getAxis().isHorizontal() || !second.getAxis().isHorizontal()) {
            return false;
        }
        BlockPos horizontal = pos.relative(first).relative(second);
        for (int yOffset = -1; yOffset <= 1; yOffset++) {
            BlockPos candidatePos = horizontal.offset(0, yOffset, 0);
            if (canConnect(pos, state, candidatePos, level.getBlockState(candidatePos))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuilds nearby lamp models after a lamp is placed or removed. The vertical
     * range covers ordinary and composite slab-mounted lamps, whose block
     * positions may differ by one block while their faces are coplanar.
     */
    public static void notifyNeighbors(Level level, BlockPos pos) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE;
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos neighborPos = pos.offset(x, y, z);
                    BlockState neighborState = level.getBlockState(neighborPos);
                    if (isLamp(neighborState)) {
                        level.sendBlockUpdated(neighborPos, neighborState, neighborState, flags);
                    }
                }
            }
        }
    }
}
