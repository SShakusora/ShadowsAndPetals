package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LargeCurtainBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.blockentity.LargeCurtainBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class LargeCurtainBlockEntityRenderer implements BlockEntityRenderer<LargeCurtainBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public LargeCurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LargeCurtainBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(LargeCurtainBlock.ANIMATING)) {
            return;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        String color = path.endsWith("_large_curtain")
                ? path.substring(0, path.length() - "_large_curtain".length()) : "white";
        String side = state.getValue(LargeCurtainBlock.SIDE).getSerializedName();
        String pose = state.getValue(LargeCurtainBlock.OPEN) ? "open" : "closed";
        String half = state.getValue(LargeCurtainBlock.HALF) == DoubleBlockHalf.UPPER ? "upper" : "lower";
        String column = state.getValue(LargeCurtainBlock.COLUMN).getSerializedName();

        poseStack.pushPose();
        Direction facing = state.getValue(LargeCurtainBlock.FACING);
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        LegacyBlockEntityRenderSupport.renderStandalone(
                blockRenderer,
                ShadowsAndPetals.asResource("block/large_curtain/static/" + side + "/" + pose + "/" + color
                        + "/" + half + "_" + column),
                state,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
