package com.sshakusora.shadowsandpetals.block.decoration.bonsai;

import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Shared model-space transform used by the chunk-rendered pot, tree geometry,
 * and the selection outline.
 */
public final class BonsaiModelTransform {
    /**
     * The Blockbench exports use the pot's long axis as model Z. Keep this
     * compensation in one place so the pot, tree, and outline stay aligned.
     */
    public static final float MODEL_AUTHORING_ROTATION_DEGREES = 90.0F;

    private BonsaiModelTransform() {
    }

    public static float rotationDegrees(int rotationSegment) {
        return -RotationSegment.convertToDegrees(rotationSegment)
                + MODEL_AUTHORING_ROTATION_DEGREES;
    }

    /**
     * Creates a matrix that rotates model coordinates around the block centre.
     * Matrix coordinates are in the normal 0-1 block-local coordinate system.
     */
    public static Matrix4f aroundBlockCenter(int rotationSegment) {
        return new Matrix4f()
                .translate(0.5F, 0.0F, 0.5F)
                .rotateY((float) Math.toRadians(rotationDegrees(rotationSegment)))
                .translate(-0.5F, 0.0F, -0.5F);
    }

    /**
     * Rotates a model JSON point in its 0-16 coordinate system around (8, 8)
     * in the X/Z plane. This is the same transform as {@link
     * #aroundBlockCenter(int)}, without a lossy unit conversion. The signs
     * intentionally match JOML's column-vector {@code rotateY} convention.
     */
    public static Vec3 transformModelPoint(Vec3 point, int rotationSegment) {
        double angle = Math.toRadians(rotationDegrees(rotationSegment));
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double relativeX = point.x - 8.0D;
        double relativeZ = point.z - 8.0D;
        return new Vec3(
                relativeX * cos + relativeZ * sin + 8.0D,
                point.y,
                -relativeX * sin + relativeZ * cos + 8.0D
        );
    }
}
