package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.sshakusora.shadowsandpetals.blockentity.CurtainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A complete logical curtain, independent of the curtain's physical width.
 * Small curtains contain two members; large curtains contain four members.
 *
 * <p>The lower anchor is the small curtain's lower half or the large
 * curtain's lower outer quadrant. Both curtain families therefore use the
 * same centre-facing partner geometry.</p>
 */
record CurtainStructure(
        BlockPos anchor,
        BlockState anchorState,
        Direction facing,
        CurtainBlock.Side side,
        List<BlockPos> members,
        boolean large
) {
    CurtainStructure {
        anchor = anchor.immutable();
        members = members.stream().map(BlockPos::immutable).toList();
    }

    static Optional<CurtainStructure> resolve(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LargeCurtainBlock) {
            return resolveLarge(level, pos, state);
        }
        if (state.getBlock() instanceof CurtainBlock) {
            return resolveSmall(level, pos, state);
        }
        return Optional.empty();
    }

    /**
     * Chooses a side using either curtain family as the placement neighbour.
     */
    static CurtainBlock.Side sideForPlacement(
            Level level, BlockPos lowerPos, Direction facing, boolean sneaking
    ) {
        Direction leftDir = facing.getClockWise();
        Direction[] both = {leftDir, leftDir.getOpposite()};
        Set<BlockPos> seenAnchors = new LinkedHashSet<>();
        for (Direction direction : both) {
            for (int distance = 1; distance <= 2; distance++) {
                BlockPos neighbourPos = lowerPos.relative(direction, distance);
                Optional<CurtainStructure> resolved = resolve(level, neighbourPos);
                if (resolved.isEmpty() || !seenAnchors.add(resolved.get().anchor())) {
                    continue;
                }
                CurtainStructure neighbour = resolved.get();
                if (neighbour.facing() != facing) {
                    continue;
                }
                if (sneaking) {
                    return neighbour.side();
                }
                // A neighbour on the observer's left means this curtain is
                // the observer's right, matching the existing placement rule.
                return direction == leftDir
                        ? CurtainBlock.Side.RIGHT
                        : CurtainBlock.Side.LEFT;
            }
        }
        return CurtainBlock.Side.LEFT;
    }

    BlockPos partnerAnchor() {
        return partnerAnchor(anchor, facing, side);
    }

    static Direction towardPartner(Direction facing, CurtainBlock.Side side) {
        return side == CurtainBlock.Side.LEFT
                ? facing.getClockWise().getOpposite()
                : facing.getClockWise();
    }

    static BlockPos partnerAnchor(
            BlockPos anchor, Direction facing, CurtainBlock.Side side
    ) {
        return anchor.relative(towardPartner(facing, side));
    }

    boolean hasLiveRedstoneSignal(Level level) {
        return members.stream().anyMatch(level::hasNeighborSignal);
    }

    boolean hasStoredPower(Level level) {
        return members.stream()
                .map(level::getBlockState)
                .filter(state -> state.getBlock() == anchorState.getBlock())
                .anyMatch(state -> state.getValue(CurtainBlock.POWERED));
    }

    /**
     * Returns whether any member of this logical curtain still needs to move
     * to the requested pose.
     */
    static boolean requiresAnimation(Stream<Boolean> memberOpenStates, boolean targetOpen) {
        return memberOpenStates.anyMatch(memberOpen -> memberOpen != targetOpen);
    }

    void setPowered(Level level, boolean powered) {
        for (BlockPos member : members) {
            BlockState state = level.getBlockState(member);
            if (state.getBlock() != anchorState.getBlock()) {
                continue;
            }
            level.setBlock(member, state.setValue(CurtainBlock.POWERED, powered), Block.UPDATE_ALL);
        }
    }

    void setOpen(Level level, boolean open, long gameTime) {
        boolean powered = hasLiveRedstoneSignal(level);
        // A logical curtain is the animation unit.  If all of its members are
        // already at the requested pose, keep their static models and clocks
        // intact; the partner controller may still call this method only to
        // synchronize POWERED.
        boolean shouldAnimate = requiresAnimation(members.stream()
                .map(level::getBlockState)
                .filter(state -> state.getBlock() == anchorState.getBlock())
                .map(state -> state.getValue(CurtainBlock.OPEN)), open);

        for (BlockPos member : members) {
            BlockState state = level.getBlockState(member);
            if (state.getBlock() != anchorState.getBlock()) {
                continue;
            }

            BlockState updated = state.setValue(CurtainBlock.POWERED, powered);
            if (shouldAnimate) {
                recordClock(level, member, gameTime, open);
                level.setBlock(member, updated
                        .setValue(CurtainBlock.OPEN, open)
                        .setValue(CurtainBlock.ANIMATING, true), Block.UPDATE_ALL);
                level.scheduleTick(member, state.getBlock(), CurtainBlock.ANIMATION_TICKS);
            } else if (updated != state) {
                // Keep OPEN, ANIMATING and the existing animation clock when
                // this logical curtain is already at the requested pose.
                level.setBlock(member, updated, Block.UPDATE_ALL);
            }
        }
    }

    private static Optional<CurtainStructure> resolveSmall(Level level, BlockPos pos, BlockState state) {
        BlockPos anchor = state.getValue(CurtainBlock.HALF) == DoubleBlockHalf.LOWER
                ? pos
                : pos.below();
        BlockState lower = level.getBlockState(anchor);
        BlockState upper = level.getBlockState(anchor.above());
        if (lower.getBlock() != state.getBlock()
                || lower.getValue(CurtainBlock.HALF) != DoubleBlockHalf.LOWER
                || upper.getBlock() != state.getBlock()
                || upper.getValue(CurtainBlock.HALF) != DoubleBlockHalf.UPPER
                || !sameCommonProperties(lower, upper)) {
            return Optional.empty();
        }
        return Optional.of(new CurtainStructure(
                anchor,
                lower,
                lower.getValue(CurtainBlock.FACING),
                lower.getValue(CurtainBlock.SIDE),
                List.of(anchor, anchor.above()),
                false
        ));
    }

    private static Optional<CurtainStructure> resolveLarge(Level level, BlockPos pos, BlockState state) {
        BlockPos anchor = LargeCurtainBlock.anchorOf(pos, state);
        BlockState anchorState = level.getBlockState(anchor);
        if (!(anchorState.getBlock() instanceof LargeCurtainBlock)
                || anchorState.getValue(LargeCurtainBlock.HALF) != DoubleBlockHalf.LOWER
                || anchorState.getValue(LargeCurtainBlock.COLUMN) != LargeCurtainBlock.Column.OUTER
                || !anchorState.getValue(LargeCurtainBlock.ANCHOR)) {
            return Optional.empty();
        }

        BlockPos[] parts = LargeCurtainBlock.structurePositions(anchor, anchorState);
        List<BlockPos> members = List.of(parts);
        for (int index = 0; index < parts.length; index++) {
            BlockState part = level.getBlockState(parts[index]);
            DoubleBlockHalf half = index >= 2 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;
            LargeCurtainBlock.Column column = (index & 1) == 0
                    ? LargeCurtainBlock.Column.OUTER
                    : LargeCurtainBlock.Column.INNER;
            boolean anchorPart = index == 0;
            if (!(part.getBlock() instanceof LargeCurtainBlock)
                    || part.getBlock() != anchorState.getBlock()
                    || part.getValue(LargeCurtainBlock.FACING) != anchorState.getValue(LargeCurtainBlock.FACING)
                    || part.getValue(LargeCurtainBlock.SIDE) != anchorState.getValue(LargeCurtainBlock.SIDE)
                    || part.getValue(LargeCurtainBlock.HALF) != half
                    || part.getValue(LargeCurtainBlock.COLUMN) != column
                    || part.getValue(LargeCurtainBlock.ANCHOR) != anchorPart) {
                return Optional.empty();
            }
        }
        return Optional.of(new CurtainStructure(
                anchor,
                anchorState,
                anchorState.getValue(LargeCurtainBlock.FACING),
                anchorState.getValue(LargeCurtainBlock.SIDE),
                members,
                true
        ));
    }

    private static boolean sameCommonProperties(BlockState first, BlockState second) {
        return first.getValue(CurtainBlock.FACING) == second.getValue(CurtainBlock.FACING)
                && first.getValue(CurtainBlock.SIDE) == second.getValue(CurtainBlock.SIDE);
    }

    private static void recordClock(Level level, BlockPos pos, long gameTime, boolean open) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CurtainBlockEntity curtain) {
            curtain.recordTransition(gameTime, open);
            curtain.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
    }
}
