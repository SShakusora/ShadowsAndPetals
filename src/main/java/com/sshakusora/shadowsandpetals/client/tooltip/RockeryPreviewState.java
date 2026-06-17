package com.sshakusora.shadowsandpetals.client.tooltip;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Picture-in-picture render state for displaying a rockery block model
 * preview inside a tooltip.
 */
public record RockeryPreviewState(
    RockeryBlock block,
    RockeryDimensions dimensions,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public RockeryPreviewState(RockeryBlock block, RockeryDimensions dimensions,
                               int x, int y, int width, int height,
                               @Nullable ScreenRectangle scissorArea) {
        this(
            block, dimensions,
            x, y, x + width, y + height,
            scaleFor(dimensions, width, height),
            rotateHalfTurnPose(x, y, x + width, y + height),
            scissorArea,
            PictureInPictureRenderState.getBounds(x, y, x + width, y + height, scissorArea)
        );
    }

    private static float scaleFor(RockeryDimensions dimensions, int width, int height) {
        int maxDimension = Math.max(dimensions.width(), Math.max(dimensions.height(), dimensions.depth()));
        return Math.min(width, height) * 0.70F / Math.max(1, maxDimension);
    }

    private static Matrix3x2f rotateHalfTurnPose(int x0, int y0, int x1, int y1) {
        return new Matrix3x2f(
                -1.0F, 0.0F,
                0.0F, -1.0F,
                x0 + x1, y0 + y1
        );
    }
}
