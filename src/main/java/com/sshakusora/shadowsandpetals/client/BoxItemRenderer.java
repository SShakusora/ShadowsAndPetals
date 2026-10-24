package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.shadowsandpetals.util.BoxContents;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders inventory, held, and dropped boxes with the shell plus their stored content.
 */
public class BoxItemRenderer extends BlockEntityWithoutLevelRenderer {
    public BoxItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BoxContentRenderer.renderShell(poseStack, buffer, packedLight, packedOverlay);
        BoxContentRenderer.renderContents(BoxContents.contentStack(stack), poseStack, buffer, packedLight, packedOverlay);
    }
}
