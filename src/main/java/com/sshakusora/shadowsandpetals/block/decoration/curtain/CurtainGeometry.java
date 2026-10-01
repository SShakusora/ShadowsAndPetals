package com.sshakusora.shadowsandpetals.block.decoration.curtain;

/** Pure pixel-space collision rules for the one-cell curtain. */
final class CurtainGeometry {
    private CurtainGeometry() {
    }

    static CollisionBox closed() {
        return new CollisionBox(0, 0, 13, 16, 16, 16);
    }

    static CollisionBox open(boolean leftSide) {
        double minX = leftSide ? 12 : 0;
        double maxX = leftSide ? 16 : 4;
        return new CollisionBox(minX, 0, 12, maxX, 16, 17);
    }

    record CollisionBox(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
    }
}
