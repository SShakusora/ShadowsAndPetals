package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import com.sshakusora.shadowsandpetals.block.nature.SandExcavationBlock;
import com.sshakusora.shadowsandpetals.blockentity.SandExcavationBlockEntity;

public class SandExcavationBlockEntityRenderer implements BlockEntityRenderer<SandExcavationBlockEntity> {
    private final ItemRenderer itemRenderer;

    public SandExcavationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SandExcavationBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        int dusted = blockEntity.getBlockState().getValue(SandExcavationBlock.DUSTED);
        Direction direction = blockEntity.getHitDirection();
        if (dusted <= 0 || direction == null || blockEntity.getItem().isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        float offset = dusted / 10.0F * 0.75F;
        switch (direction) {
            case EAST -> poseStack.translate(0.73F + offset, 0.0F, 0.5F);
            case WEST -> poseStack.translate(0.25F - offset, 0.0F, 0.5F);
            case UP -> poseStack.translate(0.5F, 0.25F + offset, 0.5F);
            case DOWN -> poseStack.translate(0.5F, -0.23F - offset, 0.5F);
            case NORTH -> poseStack.translate(0.5F, 0.0F, 0.25F - offset);
            case SOUTH -> poseStack.translate(0.5F, 0.0F, 0.73F + offset);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(75.0F));
        boolean eastWest = direction == Direction.EAST || direction == Direction.WEST;
        poseStack.mulPose(Axis.YP.rotationDegrees((eastWest ? 90.0F : 0.0F) + 11.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        LegacyBlockEntityRenderSupport.renderItem(
                itemRenderer,
                blockEntity.getItem(),
                poseStack,
                buffer,
                blockEntity.getLevel(),
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
