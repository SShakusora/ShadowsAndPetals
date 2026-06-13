package com.sshakusora.shadowsandpetals.client.ct;

/**
 * Defines the layout and index mapping for a connected texture atlas.
 * <p>
 * Each type has a {@link #sheetSize} (number of columns) and a method
 * {@link #getTextureIndex(CTContext)} that maps a connection context to a
 * tile index within the atlas.
 */
public enum CTTextureType {

    /**
     * Omnidirectional connected texture for full cube blocks.
     * <p>
     * Atlas layout: 8 columns x 8 rows = 64 tiles.
     * The mapping follows Create's OMNIDIRECTIONAL algorithm.
     */
    OMNIDIRECTIONAL(8) {
        @Override
        public int getTextureIndex(CTContext c) {
            int tileX = 0, tileY = 0;
            int borders = (!c.up ? 1 : 0) + (!c.down ? 1 : 0) + (!c.left ? 1 : 0) + (!c.right ? 1 : 0);

            if (c.up) tileX++;
            if (c.down) tileX += 2;
            if (c.left) tileY++;
            if (c.right) tileY += 2;

            if (borders == 0) {
                if (c.topRight) tileX++;
                if (c.topLeft) tileX += 2;
                if (c.bottomRight) tileY += 2;
                if (c.bottomLeft) tileY++;
            }

            if (borders == 1) {
                if (!c.right) {
                    if (c.topLeft || c.bottomLeft) {
                        tileY = 4;
                        tileX = -1 + (c.bottomLeft ? 1 : 0) + (c.topLeft ? 1 : 0) * 2;
                    }
                }
                if (!c.left) {
                    if (c.topRight || c.bottomRight) {
                        tileY = 5;
                        tileX = -1 + (c.bottomRight ? 1 : 0) + (c.topRight ? 1 : 0) * 2;
                    }
                }
                if (!c.down) {
                    if (c.topLeft || c.topRight) {
                        tileY = 6;
                        tileX = -1 + (c.topLeft ? 1 : 0) + (c.topRight ? 1 : 0) * 2;
                    }
                }
                if (!c.up) {
                    if (c.bottomLeft || c.bottomRight) {
                        tileY = 7;
                        tileX = -1 + (c.bottomLeft ? 1 : 0) + (c.bottomRight ? 1 : 0) * 2;
                    }
                }
            }

            if (borders == 2) {
                if ((c.up && c.left && c.topLeft) || (c.down && c.left && c.bottomLeft)
                        || (c.up && c.right && c.topRight) || (c.down && c.right && c.bottomRight)) {
                    tileX += 3;
                }
            }

            return tileX + sheetSize * tileY;
        }
    },

    /** Horizontal-only connections (left / right) with a 2-column atlas. */
    HORIZONTAL(2) {
        @Override
        public int getTextureIndex(CTContext c) {
            return (c.right ? 1 : 0) + (c.left ? 2 : 0);
        }
    },

    /** Vertical-only connections (up / down) with a 2-column atlas. */
    VERTICAL(2) {
        @Override
        public int getTextureIndex(CTContext c) {
            return (c.up ? 1 : 0) + (c.down ? 2 : 0);
        }
    };

    protected final int sheetSize;

    CTTextureType(int sheetSize) {
        this.sheetSize = sheetSize;
    }

    public int getSheetSize() {
        return sheetSize;
    }

    /**
     * Maps a connection context to the tile index within the atlas.
     * The index follows the formula {@code index = col + sheetSize * row}.
     */
    public abstract int getTextureIndex(CTContext c);
}
