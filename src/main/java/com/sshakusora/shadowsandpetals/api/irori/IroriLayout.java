package com.sshakusora.shadowsandpetals.api.irori;

/**
 * Stable, implementation-independent view of an Irori component's surface layout.
 */
public record IroriLayout(
        int width,
        int depth,
        double offsetX,
        double offsetZ,
        boolean rotated,
        int centerWidth,
        int centerDepth
) {
    public IroriLayout {
        if (width <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Irori dimensions must be positive");
        }
        if (centerWidth <= 0 || centerWidth > width || centerDepth <= 0 || centerDepth > depth) {
            throw new IllegalArgumentException("Irori center dimensions must fit inside the component");
        }
    }
}
