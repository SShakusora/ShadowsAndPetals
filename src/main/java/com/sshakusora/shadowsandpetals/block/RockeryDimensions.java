package com.sshakusora.shadowsandpetals.block;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/**
 * Defines the width, height, and depth of a rockery multi-block structure.
 * All part positions, model paths, and world-space offsets are derived from
 * these three values.
 *
 * @param width  blocks along the local X axis (1–4)
 * @param height blocks along the local Y axis (1–4)
 * @param depth  blocks along the local Z axis (1–4)
 */
public record RockeryDimensions(int width, int height, int depth) {

    public int partCount() {
        return width * height * depth;
    }

    /**
     * Converts a part index to its local (x, y, z) position within the
     * W×H×D grid, iterating in Z-major then Y then X order.
     */
    public Vec3i localPos(int part) {
        int x = part / (height * depth);
        int rem = part % (height * depth);
        int y = rem / depth;
        int z = rem % depth;
        return new Vec3i(x, y, z);
    }

    /**
     * Computes the world-space offset of a part relative to part 0,
     * applying the given horizontal facing direction.
     */
    public Vec3i worldOffset(int part, Direction facing) {
        Vec3i local = localPos(part);
        Vec3i origin = localPos(0);
        int rx = local.getX() - origin.getX();
        int ry = local.getY() - origin.getY();
        int rz = local.getZ() - origin.getZ();
        return rotateOffset(rx, ry, rz, facing);
    }

    /**
     * Model path follows the convention:
     * {@code block/rock/{W}x{H}x{D}/{x}_{y}_{z}}
     */
    public String modelPath(int part) {
        Vec3i p = localPos(part);
        return "block/rock/" + width + "x" + height + "x" + depth
                + "/" + p.getX() + "_" + p.getY() + "_" + p.getZ();
    }

    /**
     * Directory prefix for all models of this size.
     */
    public String modelDir() {
        return "block/rock/" + width + "x" + height + "x" + depth;
    }

    /**
     * Human-readable name suffix, e.g. "1×1×2".
     */
    public String displayName() {
        return width + "×" + height + "×" + depth;
    }

    private static Vec3i rotateOffset(int rx, int ry, int rz, Direction facing) {
        return switch (facing) {
            case EAST  -> new Vec3i(rz, ry, -rx);
            case SOUTH -> new Vec3i(rx, ry, rz);
            case WEST  -> new Vec3i(-rz, ry, rx);
            default    -> new Vec3i(-rx, ry, -rz);
        };
    }
}
