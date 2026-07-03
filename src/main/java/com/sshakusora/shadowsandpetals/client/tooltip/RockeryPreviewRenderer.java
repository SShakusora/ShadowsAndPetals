package com.sshakusora.shadowsandpetals.client.tooltip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

/**
 * Picture-in-picture renderer that draws a rockery block model preview
 * into an offscreen texture using the PiP's own buffer source,
 * then blits it into the tooltip.
 */
public class RockeryPreviewRenderer extends PictureInPictureRenderer<RockeryPreviewState> {

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final long ROTATE_DURATION_NANOS = 1_550_000_000L;
    private static final long RESET_GAP_NANOS = 1_000_000_000L;
    private static final RenderType SELECTION_OUTLINE = RenderType.create(
            "rockery_selection_outline",
            RenderSetup.builder(RenderPipelines.LINES_DEPTH_BIAS)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );

    private final BlockModelResolver blockModelResolver;
    private final QuadInstance quadInstance = new QuadInstance();
    private long animationStartNanos = -1L;
    private long lastRenderNanos = -1L;

    public RockeryPreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
        this.blockModelResolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
        this.quadInstance.setLightCoords(15728880);
        this.quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
    }

    @Override
    public Class<RockeryPreviewState> getRenderStateClass() {
        return RockeryPreviewState.class;
    }

    @Override
    protected String getTextureLabel() {
        return ShadowsAndPetals.MOD_ID + ":rockery_preview";
    }

    @Override
    protected void renderToTexture(RockeryPreviewState state, PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);

        RockeryDimensions dims = state.dimensions();
        float yaw = state.yawDegrees() + (state.animate() ? animatedYaw() : 0.0F);

        poseStack.mulPose(Axis.XP.rotationDegrees(-30));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45 + yaw));
        poseStack.translate(-dims.width() / 2.0F, -dims.height() / 2.0F, -dims.depth() / 2.0F);

        BlockModelRenderState blockModel = new BlockModelRenderState();

        for (int part = 0; part < dims.partCount(); part++) {
            Vec3i local = dims.localPos(part);

            BlockState partState = state.content() == RockeryPreviewState.Content.STONE_STRUCTURE
                    ? Blocks.STONE.defaultBlockState()
                    : state.block().defaultBlockState()
                            .setValue(RockeryBlock.FACING, Direction.SOUTH)
                            .setValue(RockeryBlock.PART, part);

            blockModelResolver.update(blockModel, partState, DisplayRenderer.BLOCK_DISPLAY_CONTEXT);

            List<BlockStateModelPart> parts = blockModel.modelParts;
            RenderType renderType = blockModel.renderType;

            if (parts != null && !parts.isEmpty() && renderType != null) {
                poseStack.pushPose();
                poseStack.translate(
                    local.getX(),
                    local.getY(),
                    local.getZ()
                );
                renderModelParts(parts, renderType, poseStack);
                poseStack.popPose();
            }
        }

        if (state.content() == RockeryPreviewState.Content.STONE_STRUCTURE
                && state.selectedPart() >= 0
                && state.selectedPart() < dims.partCount()) {
            Vec3i selected = dims.localPos(state.selectedPart());
            VertexConsumer lines = this.bufferSource.getBuffer(SELECTION_OUTLINE);
            poseStack.pushPose();
            poseStack.translate(selected.getX() + 0.5F, selected.getY() + 0.5F, selected.getZ() + 0.5F);
            poseStack.scale(1.045F, 1.045F, 1.045F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            ShapeRenderer.renderShape(poseStack, lines, Shapes.block(), 0.0, 0.0, 0.0,
                    0xFFFFFFFF, 8.0F);
            poseStack.popPose();
        }
    }

    private void renderModelParts(List<BlockStateModelPart> parts, RenderType renderType, PoseStack poseStack) {
        VertexConsumer buffer = this.bufferSource.getBuffer(renderType);
        PoseStack.Pose pose = poseStack.last();

        for (BlockStateModelPart part : parts) {
            for (Direction direction : DIRECTIONS) {
                List<BakedQuad> quads = part.getQuads(direction);
                for (BakedQuad quad : quads) {
                    buffer.putBakedQuad(pose, quad, quadInstance);
                }
            }
            List<BakedQuad> unculled = part.getQuads(null);
            for (BakedQuad quad : unculled) {
                buffer.putBakedQuad(pose, quad, quadInstance);
            }
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    private float animatedYaw() {
        long now = System.nanoTime();
        if (shouldResetAnimation(now)) {
            animationStartNanos = now;
        }

        lastRenderNanos = now;

        long elapsed = Math.max(0L, now - animationStartNanos);
        return elapsed * 90.0F / ROTATE_DURATION_NANOS;
    }

    private boolean shouldResetAnimation(long now) {
        return animationStartNanos < 0L
                || lastRenderNanos < 0L
                || now - lastRenderNanos > RESET_GAP_NANOS;
    }

}
