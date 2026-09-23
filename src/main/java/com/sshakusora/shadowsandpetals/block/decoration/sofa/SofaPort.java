package com.sshakusora.shadowsandpetals.block.decoration.sofa;

import net.minecraft.core.Direction;

/**
 * A connection port exported by one sofa model.
 *
 * <p>The face tells us which neighbouring block owns the other half of the
 * connection. The back direction is part of the port identity as well: two
 * ports only match when their faces are opposite and their backs are equal.</p>
 */
record SofaPort(Direction face, Direction back) {
    SofaPort {
        if (!face.getAxis().isHorizontal() || !back.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Sofa ports must be horizontal");
        }
    }

    SofaPort rotateClockwise(int quarterTurns) {
        return new SofaPort(
                rotate(face, quarterTurns),
                rotate(back, quarterTurns));
    }

    private static Direction rotate(Direction direction, int quarterTurns) {
        Direction rotated = direction;
        for (int turn = 0; turn < Math.floorMod(quarterTurns, 4); turn++) {
            rotated = rotated.getClockWise();
        }
        return rotated;
    }
}
