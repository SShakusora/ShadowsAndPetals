package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/** Pure coordinate rules shared by the large curtain block and its tests. */
final class LargeCurtainGeometry {
    private LargeCurtainGeometry() {
    }

    static Direction innerStep(Direction facing, boolean rightSide) {
        return rightSide
                ? facing.getClockWise().getOpposite()
                : facing.getClockWise();
    }

    static BlockPos[] structurePositions(BlockPos anchor, Direction inner) {
        return new BlockPos[]{
                anchor,
                anchor.relative(inner),
                anchor.above(),
                anchor.above().relative(inner)
        };
    }

    static boolean allReplaceable(BlockPos anchor, Direction inner, Predicate<BlockPos> replaceable) {
        for (BlockPos part : structurePositions(anchor, inner)) {
            if (!replaceable.test(part)) {
                return false;
            }
        }
        return true;
    }

    static BlockPos anchorOf(BlockPos pos, Direction inner, boolean upper, boolean innerColumn) {
        BlockPos anchor = upper ? pos.below() : pos;
        return innerColumn ? anchor.relative(inner.getOpposite()) : anchor;
    }

    static boolean isAnchorPosition(BlockPos pos, Direction inner, boolean upper, boolean innerColumn) {
        return pos.equals(anchorOf(pos, inner, upper, innerColumn));
    }

    static boolean sideFromNeighbour(boolean neighbourIsRight, boolean neighbourIsOnObserverLeft, boolean sneaking) {
        return sneaking ? neighbourIsRight : neighbourIsOnObserverLeft;
    }

    static BlockPos partnerAnchor(BlockPos anchor, Direction inner) {
        return anchor.relative(inner.getOpposite());
    }

    static boolean targetOpen(boolean requestedOpen, boolean localPowered, boolean partnerPowered) {
        return requestedOpen || localPowered || partnerPowered;
    }

    static CollisionBox closedCollisionBox(boolean upperHalf) {
        return new CollisionBox(0, upperHalf ? 0 : 3, 13, 16, 16, 16);
    }

    /**
     * Returns the north-facing collision box for an open quadrant. The lower
     * outer quadrant is empty because its fabric has piled into the inner
     * column; the upper outer quadrant retains only the one-pixel rail.
     */
    static @Nullable CollisionBox openCollisionBox(
            boolean outerColumn, boolean upperHalf, boolean leftSide
    ) {
        if (outerColumn) {
            return upperHalf ? new CollisionBox(0, 14, 14, 16, 15, 15) : null;
        }
        double minY = upperHalf ? 0 : 3;
        return leftSide
                ? new CollisionBox(7, minY, 12, 15, 16, 17)
                : new CollisionBox(1, minY, 12, 9, 16, 17);
    }

    record CollisionBox(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
    }
}
