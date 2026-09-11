package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.blockentity.ShishiOdoshiPipeBlockEntity;

public class ShishiOdoshiPipeBlockEntityRenderer implements BlockEntityRenderer<ShishiOdoshiPipeBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public ShishiOdoshiPipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(ShishiOdoshiPipeBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // The pipe body is a normal baked block model.  Fluid motion is
        // represented by the client splash ticker in 1.21.1; keeping this
        // pass for the body also prevents invisible geometry while a pipe is
        // connected to a remote shishi odoshi.
        LegacyBlockEntityRenderSupport.renderBlock(
                blockRenderer, blockEntity.getBlockState(), poseStack, buffer, packedLight, packedOverlay);
    }
}
