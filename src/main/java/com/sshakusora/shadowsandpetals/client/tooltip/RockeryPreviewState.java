package com.sshakusora.shadowsandpetals.client.tooltip;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * Picture-in-picture render state for displaying a rockery block model
 * preview inside a tooltip.
 */
public record RockeryPreviewState(
    RockeryBlock block,
    RockeryDimensions dimensions,
    Content content,
    float yawDegrees,
    boolean animate,
    int selectedPart,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public enum Content {
        STONE_STRUCTURE,
        ROCKERY
    }

    public RockeryPreviewState(RockeryBlock block, RockeryDimensions dimensions,
                               int x, int y, int width, int height,
                               @Nullable ScreenRectangle scissorArea) {
        this(block, dimensions, Content.ROCKERY, x, y, width, height, scissorArea);
    }

    public RockeryPreviewState(RockeryBlock block, RockeryDimensions dimensions, Content content,
                               int x, int y, int width, int height,
                               @Nullable ScreenRectangle scissorArea) {
        this(block, dimensions, content, 0.0F, true, -1,
                x, y, width, height, new Matrix3x2f(), scissorArea);
    }

    public RockeryPreviewState(RockeryBlock block, RockeryDimensions dimensions, Content content,
                               int x, int y, int width, int height, Matrix3x2fc parentPose,
                               @Nullable ScreenRectangle scissorArea) {
        this(block, dimensions, content, 0.0F, true, -1,
                x, y, width, height, parentPose, scissorArea);
    }

    public RockeryPreviewState(RockeryBlock block, RockeryDimensions dimensions, Content content,
                               float yawDegrees, boolean animate, int selectedPart,
                               int x, int y, int width, int height, Matrix3x2fc parentPose,
                               @Nullable ScreenRectangle scissorArea) {
        this(
            block, dimensions, content, yawDegrees, animate, selectedPart,
            x, y, x + width, y + height,
            scaleFor(dimensions, width, height),
            composePose(parentPose, x, y, x + width, y + height),
            scissorArea,
            transformedBounds(parentPose, x, y, x + width, y + height, scissorArea)
        );
    }

    public static float scaleFor(RockeryDimensions dimensions, int width, int height) {
        double horizontalExtent = Math.hypot(dimensions.width(), dimensions.depth());
        double verticalExtent = dimensions.height() * Math.cos(Math.toRadians(30.0))
                + horizontalExtent * Math.sin(Math.toRadians(30.0));
        double projectedExtent = Math.max(horizontalExtent, verticalExtent);
        return (float) (Math.min(width, height) * 0.78 / Math.max(1.0, projectedExtent));
    }

    private static Matrix3x2f rotateHalfTurnPose(int x0, int y0, int x1, int y1) {
        return new Matrix3x2f(
                -1.0F, 0.0F,
                0.0F, -1.0F,
                x0 + x1, y0 + y1
        );
    }

    private static Matrix3x2f composePose(Matrix3x2fc parentPose, int x0, int y0, int x1, int y1) {
        return new Matrix3x2f(parentPose).mul(rotateHalfTurnPose(x0, y0, x1, y1));
    }

    private static @Nullable ScreenRectangle transformedBounds(
            Matrix3x2fc parentPose, int x0, int y0, int x1, int y1,
            @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0)
                .transformAxisAligned(parentPose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}
