package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.CopperTeapotBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillCopperTeapotBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.blockentity.CopperTeapotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CopperTeapotBlockEntityRenderer implements BlockEntityRenderer<CopperTeapotBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public CopperTeapotBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CopperTeapotBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        LegacyBlockEntityRenderSupport.renderBlock(
                blockRenderer, state, poseStack, buffer, packedLight, packedOverlay);

        float progress = easeOutCubic(blockEntity.getLidProgress(partialTick));
        if (progress <= 0.0F && !(state.getBlock() instanceof IroriGrillCopperTeapotBlock)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.getValue(CopperTeapotBlock.FACING).toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        poseStack.translate(
                0.0D,
                (state.getBlock() instanceof IroriGrillCopperTeapotBlock
                        ? CopperTeapotBlock.IRORI_RENDER_OFFSET : 0.0D)
                        + progress * CopperTeapotBlockEntity.MAX_LID_LIFT,
                0.0D
        );
        LegacyBlockEntityRenderSupport.renderStandalone(
                blockRenderer,
                ShadowsAndPetals.asResource("block/teapot/copper/lid"),
                state,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1.0F - progress;
        return 1.0F - inverse * inverse * inverse;
    }
}
