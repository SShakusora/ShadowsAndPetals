package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.blockentity.WindChimeBlockEntity;
import net.minecraft.core.Direction;

public class WindChimeBlockEntityRenderer implements BlockEntityRenderer<WindChimeBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public WindChimeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(WindChimeBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        WindChimeColors colors = blockEntity.getColors();
        long time = blockEntity.getLevel().getGameTime();
        float natural = blockEntity.getNaturalMotionWeight(partialTick);
        float bodyX = blockEntity.getBodyX(partialTick)
                + natural * (float) Math.sin((time + partialTick) * 0.075D) * 2.5F;
        float bodyY = blockEntity.getBodyY(partialTick)
                + natural * (float) Math.sin((time + partialTick) * 0.024D) * 3.0F;
        float bodyZ = blockEntity.getBodyZ(partialTick)
                + natural * (float) Math.sin((time + partialTick) * 0.061D) * 2.0F;
        float mainX = blockEntity.getMainX(partialTick)
                + natural * (float) Math.sin((time + partialTick) * 0.075D - 0.65D) * 4.0F;
        float mainY = blockEntity.getMainY(partialTick)
                + natural * (float) Math.sin((time + partialTick) * 0.029D - 0.8D) * 7.0F;
        float mainZ = blockEntity.getMainZ(partialTick)
                + natural * (float) Math.sin((time + partialTick) * 0.061D + 1.15D) * 3.0F;

        float axisRotation = blockEntity.getBlockState().getValue(
                com.sshakusora.shadowsandpetals.block.decoration.WindChimeBlock.HORIZONTAL_AXIS
        ) == Direction.Axis.X ? 90.0F : 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(axisRotation));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        rotateAround(poseStack, 0.5D, 1.0D, 0.5D, bodyX, bodyY, bodyZ);
        LegacyBlockEntityRenderSupport.renderStandalone(
                blockRenderer, WindChimeColors.blockBodyModelId(colors.ribbon()),
                blockEntity.getBlockState(), poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(axisRotation));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        rotateAround(poseStack, 0.5D, 1.0D - 4.0D / 16.0D, 0.5D, mainX, mainY, mainZ);
        poseStack.translate(0.0D, -4.0D / 16.0D, 0.0D);
        LegacyBlockEntityRenderSupport.renderStandalone(
                blockRenderer, WindChimeColors.blockMainRibbonModelId(colors.ribbon()),
                blockEntity.getBlockState(), poseStack, buffer, packedLight, packedOverlay);
        LegacyBlockEntityRenderSupport.renderStandalone(
                blockRenderer, WindChimeColors.blockVaneModelId(colors.vane()),
                blockEntity.getBlockState(), poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void rotateAround(
            PoseStack poseStack, double x, double y, double z,
            float xDegrees, float yDegrees, float zDegrees
    ) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(xDegrees));
        poseStack.mulPose(Axis.YP.rotationDegrees(yDegrees));
        poseStack.mulPose(Axis.ZP.rotationDegrees(zDegrees));
        poseStack.translate(-x, -y, -z);
    }
}
