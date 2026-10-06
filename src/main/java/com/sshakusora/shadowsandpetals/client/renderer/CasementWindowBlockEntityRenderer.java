package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowBlock;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowGeometry;
import com.sshakusora.shadowsandpetals.blockentity.CasementWindowBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BakedModelSupport;
import com.sshakusora.shadowsandpetals.util.MathUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class CasementWindowBlockEntityRenderer
        implements BlockEntityRenderer<CasementWindowBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public CasementWindowBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public AABB getRenderBoundingBox(CasementWindowBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D);
    }

    @Override
    public void render(
            CasementWindowBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(CasementWindowBlock.ANIMATING)
                || blockEntity.getLevel() == null) {
            return;
        }

        boolean targetOpen = state.getValue(CasementWindowBlock.OPEN);
        float progress = blockEntity.transitionProgress(
                blockEntity.getLevel().getGameTime(), partialTick);
        if (blockEntity.targetOpen() != targetOpen || progress < 0.0F) {
            // Block state and the animation clock arrive in separate packets.
            // Do not render an endpoint with a stale clock: the next frame
            // will render normally once the server transition is available.
            return;
        }
        progress = Math.min(progress, 1.0F);
        float eased = MathUtils.easeOutCubic(progress);
        float targetAngle = CasementWindowGeometry.targetAngle(
                state.getValue(CasementWindowBlock.SIDE));
        float angle = targetOpen
                ? targetAngle * eased
                : targetAngle * (1.0F - eased);

        BlockState renderState = state
                .setValue(CasementWindowBlock.OPEN, false)
                .setValue(CasementWindowBlock.ANIMATING, false);
        BakedModel model = BakedModelSupport.blockModel(renderState);
        var pivot = CasementWindowGeometry.pivot(
                state.getValue(CasementWindowBlock.FACING),
                state.getValue(CasementWindowBlock.SIDE));

        poseStack.pushPose();
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
        LegacyBlockEntityRenderSupport.renderModel(
                blockRenderer,
                model,
                renderState,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
