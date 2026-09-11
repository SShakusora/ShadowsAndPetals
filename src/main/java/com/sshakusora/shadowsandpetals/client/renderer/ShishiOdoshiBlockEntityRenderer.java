package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.ShishiOdoshiBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.blockentity.ShishiOdoshiBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShishiOdoshiBlockEntityRenderer implements BlockEntityRenderer<ShishiOdoshiBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public ShishiOdoshiBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(ShishiOdoshiBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        LegacyBlockEntityRenderSupport.renderBlock(
                blockRenderer, state, poseStack, buffer, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.getValue(ShishiOdoshiBlock.FACING).toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        poseStack.translate(8.0D / 16.0D, 9.0D / 16.0D, 9.0D / 16.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(blockEntity.getTipAngle(partialTick)));
        poseStack.translate(-8.0D / 16.0D, -9.0D / 16.0D, -9.0D / 16.0D);
        LegacyBlockEntityRenderSupport.renderStandalone(
                blockRenderer,
                ShadowsAndPetals.asResource("block/shishi_odoshi/main"),
                state,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
