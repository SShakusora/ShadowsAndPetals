package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.sshakusora.shadowsandpetals.blockentity.AbstractCurtainBlockEntity;
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

/** A complete logical curtain, independent of its physical size. */
record CurtainStructure(
        BlockPos anchor,
        BlockState anchorState,
        Direction facing,
        CurtainSide side,
        List<BlockPos> members,
        CurtainSize size
) {
    enum CurtainSize {
        NORMAL,
        LONG,
        LARGE
    }

    CurtainStructure {
        anchor = anchor.immutable();
        members = members.stream().map(BlockPos::immutable).toList();
    }

    static Optional<CurtainStructure> resolve(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LargeCurtainBlock) {
            return resolveLarge(level, pos, state);
        }
        if (state.getBlock() instanceof LongCurtainBlock) {
            return resolveLong(level, pos, state);
        }
        if (state.getBlock() instanceof CurtainBlock) {
            return resolveNormal(level, pos, state);
        }
        return Optional.empty();
    }

    static CurtainSide sideForPlacement(
            Level level, BlockPos railPos, Direction facing, boolean sneaking
    ) {
        Direction leftDir = facing.getClockWise();
        Direction[] both = {leftDir, leftDir.getOpposite()};
        Set<BlockPos> seenAnchors = new LinkedHashSet<>();
        for (Direction direction : both) {
            for (int distance = 1; distance <= 2; distance++) {
                BlockPos neighbourPos = railPos.relative(direction, distance);
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
                return direction == leftDir ? CurtainSide.RIGHT : CurtainSide.LEFT;
            }
        }
        return CurtainSide.LEFT;
    }

    /** Returns the horizontal partner of the physical structure anchor. */
    BlockPos partnerAnchor() {
        return partnerAnchor(anchor, facing, side);
    }

    /**
     * The upper row used as the common horizontal curtain rail. The physical
     * anchor remains the lower row for multi-block curtains because structure
     * maintenance and block-entity state are rooted there.
     */
    BlockPos railPosition() {
        return size == CurtainSize.NORMAL ? anchor : anchor.above();
    }

    BlockPos partnerRailPosition() {
        return partnerRailPosition(railPosition(), facing, side);
    }

    static Direction towardPartner(Direction facing, CurtainSide side) {
        return side == CurtainSide.LEFT
                ? facing.getClockWise().getOpposite()
                : facing.getClockWise();
    }

    static BlockPos partnerAnchor(BlockPos anchor, Direction facing, CurtainSide side) {
        return anchor.relative(towardPartner(facing, side));
    }

    /** Returns the horizontal partner position on the shared upper rail. */
    static BlockPos partnerRailPosition(BlockPos railPosition, Direction facing, CurtainSide side) {
        return railPosition.relative(towardPartner(facing, side));
    }

    boolean hasLiveRedstoneSignal(Level level) {
        return members.stream().anyMatch(level::hasNeighborSignal);
    }

    boolean hasStoredPower(Level level) {
        return members.stream()
                .map(level::getBlockState)
                .filter(state -> state.getBlock() == anchorState.getBlock())
                .anyMatch(state -> state.getValue(AbstractCurtainBlock.POWERED));
    }

    static boolean requiresAnimation(Stream<Boolean> memberOpenStates, boolean targetOpen) {
        return memberOpenStates.anyMatch(memberOpen -> memberOpen != targetOpen);
    }

    void setPowered(Level level, boolean powered) {
        for (BlockPos member : members) {
            BlockState state = level.getBlockState(member);
            if (state.getBlock() != anchorState.getBlock()) {
                continue;
            }
            level.setBlock(member, state.setValue(AbstractCurtainBlock.POWERED, powered), Block.UPDATE_ALL);
        }
    }

    void setOpen(Level level, boolean open, long gameTime) {
        boolean powered = hasLiveRedstoneSignal(level);
        boolean shouldAnimate = requiresAnimation(members.stream()
                .map(level::getBlockState)
                .filter(state -> state.getBlock() == anchorState.getBlock())
                .map(state -> state.getValue(AbstractCurtainBlock.OPEN)), open);
        int animationFlags = level.isClientSide() ? Block.UPDATE_ALL_IMMEDIATE : Block.UPDATE_ALL;

        for (BlockPos member : members) {
            BlockState state = level.getBlockState(member);
            if (state.getBlock() != anchorState.getBlock()) {
                continue;
            }

            BlockState updated = state.setValue(AbstractCurtainBlock.POWERED, powered);
            if (shouldAnimate) {
                recordClock(level, member, gameTime, open);
                level.setBlock(member, updated
                        .setValue(AbstractCurtainBlock.OPEN, open)
                        .setValue(AbstractCurtainBlock.ANIMATING, true), animationFlags);
                level.scheduleTick(member, state.getBlock(), AbstractCurtainBlock.ANIMATION_TICKS);
            } else if (updated != state) {
                level.setBlock(member, updated, Block.UPDATE_ALL);
            }
        }
    }

    private static Optional<CurtainStructure> resolveNormal(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CurtainBlock)) {
            return Optional.empty();
        }
        return Optional.of(new CurtainStructure(
                pos,
                state,
                state.getValue(AbstractCurtainBlock.FACING),
                state.getValue(AbstractCurtainBlock.SIDE),
                List.of(pos),
                CurtainSize.NORMAL
        ));
    }

    private static Optional<CurtainStructure> resolveLong(Level level, BlockPos pos, BlockState state) {
        BlockPos anchor = state.getValue(LongCurtainBlock.HALF) == DoubleBlockHalf.LOWER
                ? pos : pos.below();
        BlockState lower = level.getBlockState(anchor);
        BlockState upper = level.getBlockState(anchor.above());
        if (!(lower.getBlock() instanceof LongCurtainBlock)
                || lower.getBlock() != state.getBlock()
                || lower.getValue(LongCurtainBlock.HALF) != DoubleBlockHalf.LOWER
                || upper.getBlock() != state.getBlock()
                || upper.getValue(LongCurtainBlock.HALF) != DoubleBlockHalf.UPPER
                || !sameCommonProperties(lower, upper)) {
            return Optional.empty();
        }
        return Optional.of(new CurtainStructure(
                anchor,
                lower,
                lower.getValue(AbstractCurtainBlock.FACING),
                lower.getValue(AbstractCurtainBlock.SIDE),
                List.of(anchor, anchor.above()),
                CurtainSize.LONG
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
                    ? LargeCurtainBlock.Column.OUTER : LargeCurtainBlock.Column.INNER;
            boolean anchorPart = index == 0;
            if (!(part.getBlock() instanceof LargeCurtainBlock)
                    || part.getBlock() != anchorState.getBlock()
                    || part.getValue(AbstractCurtainBlock.FACING) != anchorState.getValue(AbstractCurtainBlock.FACING)
                    || part.getValue(AbstractCurtainBlock.SIDE) != anchorState.getValue(AbstractCurtainBlock.SIDE)
                    || part.getValue(LargeCurtainBlock.HALF) != half
                    || part.getValue(LargeCurtainBlock.COLUMN) != column
                    || part.getValue(LargeCurtainBlock.ANCHOR) != anchorPart) {
                return Optional.empty();
            }
        }
        return Optional.of(new CurtainStructure(
                anchor,
                anchorState,
                anchorState.getValue(AbstractCurtainBlock.FACING),
                anchorState.getValue(AbstractCurtainBlock.SIDE),
                members,
                CurtainSize.LARGE
        ));
    }

    private static boolean sameCommonProperties(BlockState first, BlockState second) {
        return first.getValue(AbstractCurtainBlock.FACING) == second.getValue(AbstractCurtainBlock.FACING)
                && first.getValue(AbstractCurtainBlock.SIDE) == second.getValue(AbstractCurtainBlock.SIDE);
    }

    private static void recordClock(Level level, BlockPos pos, long gameTime, boolean open) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractCurtainBlockEntity curtain) {
            curtain.recordTransition(gameTime, open);
            curtain.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
    }
}
