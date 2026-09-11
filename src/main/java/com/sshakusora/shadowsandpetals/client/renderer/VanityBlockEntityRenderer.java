package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.blockentity.VanityBlockEntity;

public class VanityBlockEntityRenderer implements BlockEntityRenderer<VanityBlockEntity> {
    public VanityBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VanityBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
    }
}
