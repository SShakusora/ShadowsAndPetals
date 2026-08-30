package com.sshakusora.shadowsandpetals.client.renderer;

import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiModelTransform;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;

/**
 * Decides which render path owns the tree geometry for a bonsai block.
 *
 * <p>The decision intentionally depends only on the block position.  If it
 * depended on the current shape or rotation, a section rebuild could observe
 * the old and new states on different threads and briefly submit both a BER
 * tree and a chunk tree (or neither).  The envelope below covers every shape,
 * dead/live state, and all sixteen rotations, so the route remains stable for
 * the lifetime of a block position.</p>
 */
public final class BonsaiRenderRouting {
    private static final double BOUNDS_MARGIN = 1.0D / 64.0D;
    private static final AABB MAX_TREE_BOUNDS = calculateMaxTreeBounds();

    private BonsaiRenderRouting() {
    }

    /** Returns true when the tree can cross the owning 16x16x16 section. */
    public static boolean usesBer(BlockPos pos) {
        // Work in section-relative coordinates to keep this hot path free of
        // temporary SectionPos/AABB allocations during chunk compilation.
        double localX = SectionPos.sectionRelative(pos.getX());
        double localY = SectionPos.sectionRelative(pos.getY());
        double localZ = SectionPos.sectionRelative(pos.getZ());
        return localX + MAX_TREE_BOUNDS.minX < 0.0D
                || localX + MAX_TREE_BOUNDS.maxX > SectionPos.SECTION_SIZE
                || localY + MAX_TREE_BOUNDS.minY < 0.0D
                || localY + MAX_TREE_BOUNDS.maxY > SectionPos.SECTION_SIZE
                || localZ + MAX_TREE_BOUNDS.minZ < 0.0D
                || localZ + MAX_TREE_BOUNDS.maxZ > SectionPos.SECTION_SIZE;
    }

    /** The conservative envelope used by the stable route decision. */
    public static AABB maxTreeBounds() {
        return MAX_TREE_BOUNDS;
    }

    /** Returns the exact conservative tree bounds for one visual state. */
    public static AABB treeBounds(BonsaiBlockEntity.Shape shape, boolean dead, int rotation) {
        AABB source = sourceBounds(shape, dead);
        double angle = Math.toRadians(BonsaiModelTransform.rotationDegrees(rotation));
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (double x : new double[]{source.minX, source.maxX}) {
            for (double z : new double[]{source.minZ, source.maxZ}) {
                double relativeX = x - 0.5D;
                double relativeZ = z - 0.5D;
                double rotatedX = relativeX * cos + relativeZ * sin + 0.5D;
                double rotatedZ = -relativeX * sin + relativeZ * cos + 0.5D;
                minX = Math.min(minX, rotatedX);
                minZ = Math.min(minZ, rotatedZ);
                maxX = Math.max(maxX, rotatedX);
                maxZ = Math.max(maxZ, rotatedZ);
            }
        }

        return new AABB(minX, source.minY, minZ, maxX, source.maxY, maxZ)
                .inflate(BOUNDS_MARGIN);
    }

    private static AABB calculateMaxTreeBounds() {
        AABB result = treeBounds(BonsaiBlockEntity.Shape.SEMI_CASCADE, false, 0);
        for (BonsaiBlockEntity.Shape shape : BonsaiBlockEntity.Shape.values()) {
            for (boolean dead : new boolean[]{false, true}) {
                for (int rotation = 0; rotation < 16; rotation++) {
                    AABB bounds = treeBounds(shape, dead, rotation);
                    result = result.minmax(bounds);
                }
            }
        }

        // Keep the same margin on every axis.  This is deliberately
        // conservative: a section-floor block may take the BER path, but no
        // rotated/rounded vertex can be lost at a section boundary.
        return result;
    }

    private static AABB sourceBounds(BonsaiBlockEntity.Shape shape, boolean dead) {
        return switch (shape) {
            case SEMI_CASCADE -> dead
                    ? new AABB(3.0D / 16.0D, 0.0D, -12.0D / 16.0D,
                    13.0D / 16.0D, 23.0D / 16.0D, 19.0D / 16.0D)
                    : new AABB(3.0D / 16.0D, 0.0D, -16.0D / 16.0D,
                    13.0D / 16.0D, 27.0D / 16.0D, 21.0D / 16.0D);
            case SLANTING -> dead
                    ? new AABB(5.0D / 16.0D, 0.0D, 0.0D,
                    13.0D / 16.0D, 31.0D / 16.0D, 23.0D / 16.0D)
                    : new AABB(-2.0D / 16.0D, 0.0D, -4.0D / 16.0D,
                    14.0D / 16.0D, 32.0D / 16.0D, 28.0D / 16.0D);
            case TWIN -> dead
                    ? new AABB(6.0D / 16.0D, 0.0D, 0.0D,
                    10.0D / 16.0D, 32.0D / 16.0D, 17.0D / 16.0D)
                    : new AABB(3.0D / 16.0D, 0.0D, -3.0D / 16.0D,
                    13.0D / 16.0D, 32.0D / 16.0D, 19.0D / 16.0D);
            case WINDSWEPT -> dead
                    ? new AABB(6.0D / 16.0D, 0.0D, -15.0D / 16.0D,
                    11.0D / 16.0D, 23.0D / 16.0D, 13.0D / 16.0D)
                    : new AABB(4.0D / 16.0D, 0.0D, -16.0D / 16.0D,
                    11.0D / 16.0D, 23.0D / 16.0D, 13.0D / 16.0D);
        };
    }
}