package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.BoxBlock;
import com.sshakusora.shadowsandpetals.blockentity.BoxBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

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
        BlockState state = blockEntity.getBlockState();
        SlabType type = state.getValue(BoxBlock.TYPE);
        Direction facing = state.getValue(BoxBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-modelRotation(facing)));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        switch (type) {
            case DOUBLE -> {
                BoxContentRenderer.renderContents(blockEntity.contentStack(), poseStack, buffer, packedLight, packedOverlay);
                BoxContentRenderer.renderContents(blockEntity.topContentStack(), poseStack, buffer, packedLight, packedOverlay, 0.5F);
            }
            case TOP -> BoxContentRenderer.renderContents(
                    blockEntity.contentStack(), poseStack, buffer, packedLight, packedOverlay, 0.5F);
            default -> BoxContentRenderer.renderContents(
                    blockEntity.contentStack(), poseStack, buffer, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    /**
     * Matches the blockstate y rotation used for the shell models.
     */
    private static float modelRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}
