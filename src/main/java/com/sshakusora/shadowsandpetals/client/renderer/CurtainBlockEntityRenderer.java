package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.blockentity.CurtainBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class CurtainBlockEntityRenderer implements BlockEntityRenderer<CurtainBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public CurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CurtainBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(CurtainBlock.ANIMATING)) {
            return;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        String color = path.endsWith("_curtain")
                ? path.substring(0, path.length() - "_curtain".length()) : "white";
        String side = state.getValue(CurtainBlock.SIDE).getSerializedName();
        String pose = state.getValue(CurtainBlock.OPEN) ? "open" : "closed";
        String half = state.getValue(CurtainBlock.HALF) == DoubleBlockHalf.UPPER ? "upper" : "lower";

        poseStack.pushPose();
        Direction facing = state.getValue(CurtainBlock.FACING);
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        LegacyBlockEntityRenderSupport.renderStandalone(
                blockRenderer,
                ShadowsAndPetals.asResource("block/curtain/static/" + side + "/" + pose + "/" + color + "/" + half),
                state,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
