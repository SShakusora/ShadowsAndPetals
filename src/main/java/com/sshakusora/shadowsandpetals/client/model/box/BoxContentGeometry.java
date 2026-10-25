package com.sshakusora.shadowsandpetals.client.model.box;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

/**
 * Pose used by the stacked voxelized content copies inside a box.
 *
 * <p>The placed box now bakes its content into the chunk mesh instead of drawing it from a
 * block entity renderer, so the geometry has to be reproduced as a matrix. Both the matrix
 * path and {@code BoxContentRenderer} (still used by the item BEWLR) go through this class so
 * the two render paths cannot drift apart.</p>
 */
public final class BoxContentGeometry {
    public static final int LAYER_COUNT = 8;
    public static final float LOWER_Y_OFFSET = 0.0F;
    public static final float TOP_Y_OFFSET = 0.5F;

    /**
     * The eight deliberately irregular rotations/translations that make the content look like
     * a loose pile instead of eight neatly stacked copies.
     */
    private static final float[] LAYER_ROTATIONS = {
            -4.941F, 174.814F, 7.488F, 175.921F, 7.488F, 172.883F, -2.474F, 175.697F
    };

    private static final float[][] LAYER_TRANSLATIONS = {
            {0.06878F, 0.0625F, 0.01436F},
            {-0.02615F, 0.1125F, -0.03102F},
            {0.0785F, 0.1625F, 0.03709F},
            {-0.02078F, 0.2125F, -0.02095F},
            {0.11643F, 0.2625F, 0.00902F},
            {-0.03524F, 0.3125F, -0.08755F},
            {0.04715F, 0.3625F, 0.11614F},
            {-0.06111F, 0.4125F, -0.01407F}
    };

    private BoxContentGeometry() {
    }

    /**
     * Generated item textures are one pixel deep, so they keep roughly 0.8 pixel of thickness.
     * Full 3D block/item models are squashed to the same thickness so they stay inside the box.
     */
    public static float depthScale(boolean gui3d) {
        return gui3d ? 0.05F : 0.8F;
    }

    /**
     * Applies one content layer exactly like the original block entity renderer did.
     */
    public static void applyLayer(PoseStack poseStack, int layer, float yOffset, float depthScale) {
        float[] translation = LAYER_TRANSLATIONS[layer];

        poseStack.translate(0.5D + translation[0], 0.5D + yOffset + translation[1], 0.5D + translation[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(LAYER_ROTATIONS[layer]));
        poseStack.translate(0.025D, -0.475D, 0.025D);
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        poseStack.scale(0.65F, 0.65F, depthScale);
    }

    /**
     * Applies the same block-centre yaw the blockstate JSON applies to the shell model.
     */
    public static void applyFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotationDegrees(facing)));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
    }

    /**
     * Matches the {@code y} values of the generated blockstate variants.
     */
    public static float rotationDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    /**
     * Builds the complete transform for one content layer: block facing first, then the layer
     * pose, then the {@code -0.5} centring translation {@code ItemRenderer#render} applies to
     * item geometry.
     */
    public static Matrix4f contentMatrix(int layer, float yOffset, float depthScale, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyFacing(poseStack, facing);
        applyLayer(poseStack, layer, yOffset, depthScale);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        return new Matrix4f(poseStack.last().pose());
    }
}
