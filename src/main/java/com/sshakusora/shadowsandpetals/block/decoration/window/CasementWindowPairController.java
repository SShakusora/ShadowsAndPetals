package com.sshakusora.shadowsandpetals.block.decoration.window;

import com.sshakusora.shadowsandpetals.blockentity.CasementWindowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

final class CasementWindowPairController {
    private CasementWindowPairController() {
    }

    static boolean togglePair(Level level, BlockPos pos, boolean requestedOpen) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CasementWindowBlock)
                || state.getValue(CasementWindowBlock.ANIMATING)) {
            return false;
        }

        // The server owns the block state and animation clock. The client
        // only acknowledges the interaction so that the server packet is
        // the first and only local animation transition.
        if (level.isClientSide()) {
            return true;
        }

        Optional<BlockPos> partnerPos = findPartner(level, pos, state);
        if (partnerPos.isPresent()
                && level.getBlockState(partnerPos.get()).getValue(CasementWindowBlock.ANIMATING)) {
            return false;
        }

        long gameTime = level.getGameTime();
        boolean changed = setWindowIfNeeded(
                level, pos, state, requestedOpen, gameTime);
        if (partnerPos.isPresent()) {
            BlockPos partner = partnerPos.get();
            changed |= setWindowIfNeeded(
                    level,
                    partner,
                    level.getBlockState(partner),
                    requestedOpen,
                    gameTime
            );
        }
        return changed;
    }

    private static Optional<BlockPos> findPartner(Level level, BlockPos pos, BlockState state) {
        BlockPos candidatePos = CasementWindowGeometry.partnerPosition(
                pos,
                state.getValue(CasementWindowBlock.FACING),
                state.getValue(CasementWindowBlock.SIDE)
        );
        BlockState candidate = level.getBlockState(candidatePos);
        return isCompatible(state, candidate, candidatePos, pos)
                ? Optional.of(candidatePos)
                : Optional.empty();
    }

    private static boolean isCompatible(
            BlockState current,
            BlockState candidate,
            BlockPos candidatePos,
            BlockPos currentPos
    ) {
        if (!(candidate.getBlock() instanceof CasementWindowBlock)
                || !CasementWindowGeometry.sameFacing(current, candidate)
                || candidate.getValue(CasementWindowBlock.SIDE)
                != current.getValue(CasementWindowBlock.SIDE).mirror()) {
            return false;
        }
        return CasementWindowGeometry.partnerPosition(
                candidatePos,
                candidate.getValue(CasementWindowBlock.FACING),
                candidate.getValue(CasementWindowBlock.SIDE)
        ).equals(currentPos);
    }

    private static boolean setWindowIfNeeded(
            Level level,
            BlockPos pos,
            BlockState state,
            boolean targetOpen,
            long gameTime
    ) {
        if (state.getValue(CasementWindowBlock.OPEN) == targetOpen) {
            return false;
        }

        BlockState updated = state
                .setValue(CasementWindowBlock.OPEN, targetOpen)
                .setValue(CasementWindowBlock.ANIMATING, true);

        if (level.getBlockEntity(pos) instanceof CasementWindowBlockEntity window) {
            // Send the new clock while the block still has its non-animating
            // state. The client therefore receives the clock before it starts
            // rendering the ENTITYBLOCK_ANIMATED state.
            window.recordTransition(gameTime, targetOpen);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        level.setBlock(pos, updated, Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), CasementWindowBlock.ANIMATION_TICKS);
        return true;
    }
}
