package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.nature.SandExcavationBlock;
import com.sshakusora.shadowsandpetals.blockentity.SandExcavationBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelRenderer.BrightnessGetter;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class SandExcavationBlockEntityRenderer implements BlockEntityRenderer<
        SandExcavationBlockEntity,
        SandExcavationBlockEntityRenderer.State
> {
    private final ItemModelResolver itemModelResolver;

    public SandExcavationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            SandExcavationBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.hitDirection = blockEntity.getHitDirection();
        state.dustProgress = blockEntity.getBlockState().getValue(SandExcavationBlock.DUSTED);
        if (blockEntity.getLevel() != null && state.hitDirection != null) {
            state.lightCoords = LevelRenderer.getLightCoords(
                    BrightnessGetter.DEFAULT,
                    blockEntity.getLevel(),
                    blockEntity.getBlockState(),
                    blockEntity.getBlockPos().relative(state.hitDirection)
            );
        }
        itemModelResolver.updateForTopItem(
                state.itemState,
                blockEntity.getItem(),
                ItemDisplayContext.FIXED,
                blockEntity.getLevel(),
                null,
                0
        );
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.dustProgress <= 0 || state.hitDirection == null || state.itemState.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        float[] translations = translations(state.hitDirection, state.dustProgress);
        poseStack.translate(translations[0], translations[1], translations[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(75.0F));
        boolean eastWest = state.hitDirection == Direction.EAST || state.hitDirection == Direction.WEST;
        poseStack.mulPose(Axis.YP.rotationDegrees((eastWest ? 90 : 0) + 11));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        state.itemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static float[] translations(Direction direction, int completionState) {
        float[] translations = {0.5F, 0.0F, 0.5F};
        float completionOffset = completionState / 10.0F * 0.75F;
        switch (direction) {
            case EAST -> translations[0] = 0.73F + completionOffset;
            case WEST -> translations[0] = 0.25F - completionOffset;
            case UP -> translations[1] = 0.25F + completionOffset;
            case DOWN -> translations[1] = -0.23F - completionOffset;
            case NORTH -> translations[2] = 0.25F - completionOffset;
            case SOUTH -> translations[2] = 0.73F + completionOffset;
        }
        return translations;
    }

    @Override
    public AABB getRenderBoundingBox(SandExcavationBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 0.25F,
                pos.getY() - 0.25F,
                pos.getZ() - 0.25F,
                pos.getX() + 1.25F,
                pos.getY() + 1.25F,
                pos.getZ() + 1.25F
        );
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState itemState = new ItemStackRenderState();
        public int dustProgress;
        public @Nullable Direction hitDirection;
    }
}
