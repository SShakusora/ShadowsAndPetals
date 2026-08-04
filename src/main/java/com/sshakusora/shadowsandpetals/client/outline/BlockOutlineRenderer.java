package com.sshakusora.shadowsandpetals.client.outline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.joml.Vector3f;

public final class BlockOutlineRenderer implements CustomBlockOutlineRenderer {
    private static final double MODEL_UNIT_TO_BLOCK_UNIT = 1.0D / 16.0D;

    private final OutlineGeometry geometry;
    private final float lineWidth;

    public BlockOutlineRenderer(OutlineGeometry geometry, float lineWidth) {
        this.geometry = geometry;
        this.lineWidth = Math.max(lineWidth, 1.0F);
    }

    @Override
    public boolean render(
            BlockOutlineRenderState renderState,
            MultiBufferSource.BufferSource buffer,
            PoseStack poseStack,
            boolean translucentPass,
            LevelRenderState levelRenderState
    ) {
        if (renderState.isTranslucent() != translucentPass) {
            return false;
        }

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        BlockPos blockPos = renderState.pos();
        double offsetX = blockPos.getX() - cameraPos.x;
        double offsetY = blockPos.getY() - cameraPos.y;
        double offsetZ = blockPos.getZ() - cameraPos.z;

        if (renderState.highContrast()) {
            VertexConsumer secondary = buffer.getBuffer(RenderTypes.secondaryBlockOutline());
            renderGeometry(poseStack, secondary, offsetX, offsetY, offsetZ, 0xFF000000, 7.0F);
        }

        VertexConsumer lines = buffer.getBuffer(RenderTypes.lines());
        int color = renderState.highContrast() ? -11010079 : ARGB.black(102);
        renderGeometry(poseStack, lines, offsetX, offsetY, offsetZ, color, lineWidth);
        return true;
    }

    private void renderGeometry(
            PoseStack poseStack,
            VertexConsumer buffer,
            double offsetX,
            double offsetY,
            double offsetZ,
            int color,
            float width
    ) {
        PoseStack.Pose pose = poseStack.last();
        for (OutlineGeometry.Line line : geometry.lines()) {
            Vec3 from = line.from();
            Vec3 to = line.to();
            // OutlineGeometry uses model JSON's 0-16 coordinates; the render
            // pipeline expects block-local world coordinates in the 0-1 range.
            float directionX = (float) ((to.x - from.x) * MODEL_UNIT_TO_BLOCK_UNIT);
            float directionY = (float) ((to.y - from.y) * MODEL_UNIT_TO_BLOCK_UNIT);
            float directionZ = (float) ((to.z - from.z) * MODEL_UNIT_TO_BLOCK_UNIT);
            if (directionX == 0.0F && directionY == 0.0F && directionZ == 0.0F) {
                continue;
            }

            Vector3f normal = new Vector3f(directionX, directionY, directionZ).normalize();
            buffer.addVertex(
                    pose,
                    (float) (from.x * MODEL_UNIT_TO_BLOCK_UNIT + offsetX),
                    (float) (from.y * MODEL_UNIT_TO_BLOCK_UNIT + offsetY),
                    (float) (from.z * MODEL_UNIT_TO_BLOCK_UNIT + offsetZ)
            ).setColor(color).setNormal(pose, normal).setLineWidth(width);
            buffer.addVertex(
                    pose,
                    (float) (to.x * MODEL_UNIT_TO_BLOCK_UNIT + offsetX),
                    (float) (to.y * MODEL_UNIT_TO_BLOCK_UNIT + offsetY),
                    (float) (to.z * MODEL_UNIT_TO_BLOCK_UNIT + offsetZ)
            ).setColor(color).setNormal(pose, normal).setLineWidth(width);
        }
    }
}
