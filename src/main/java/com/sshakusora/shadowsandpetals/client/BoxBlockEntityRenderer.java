package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.shadowsandpetals.blockentity.BoxBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Renders only the box contents; the block model already draws the static shell.
 */
public class BoxBlockEntityRenderer implements BlockEntityRenderer<BoxBlockEntity> {
    public BoxBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            BoxBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BoxContentRenderer.renderContents(blockEntity.contentStack(), poseStack, buffer, packedLight, packedOverlay);
    }
}
