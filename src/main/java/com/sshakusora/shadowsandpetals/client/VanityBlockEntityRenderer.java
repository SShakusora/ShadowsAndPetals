package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import com.sshakusora.shadowsandpetals.blockentity.VanityBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VanityBlockEntityRenderer implements BlockEntityRenderer<VanityBlockEntity, VanityBlockEntityRenderer.State> {
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final RandomSource DRAWER_RANDOM = RandomSource.create(42L);

    public VanityBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VanityBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        var blockState = blockEntity.getBlockState();
        state.facing = blockState.getValue(VanityBlock.FACING);
        state.progress = blockEntity.getDrawerProgress(partialTicks);
        state.drawerTravelScale = blockEntity.getDrawerTravelScale();
        state.drawerTravelLimit = blockEntity.getDrawerTravelLimit();
        state.drawerModelParts.clear();

        BlockStateModel drawerModel = BlockModelRegistry.getVanityDrawerModel(blockState.getBlock());
        if (drawerModel == null) {
            state.drawerHasTranslucency = false;
            return;
        }

        var level = blockEntity.getLevel();
        if (level == null) return;
        var tintGetter = (BlockAndTintGetter) level;

        DRAWER_RANDOM.setSeed(42L);
        drawerModel.collectParts(tintGetter, blockEntity.getBlockPos(), blockState, DRAWER_RANDOM, state.drawerModelParts);
        state.drawerHasTranslucency = drawerModel.hasMaterialFlag(tintGetter, blockEntity.getBlockPos(), blockState, BakedQuad.FLAG_TRANSLUCENT);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() - 180));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        poseStack.pushPose();
        float fullTravelDistance = VanityBlock.BASE_DRAWER_TRAVEL_DISTANCE * state.drawerTravelScale;
        float targetTranslation = easeOutCubic(state.progress) * fullTravelDistance;
        poseStack.translate(0, 0, -Math.min(targetTranslation, Mth.clamp(state.drawerTravelLimit, 0.0F, fullTravelDistance)));
        submitNodeCollector.submitMultiLayerBlockModel(
                poseStack,
                state.drawerModelParts,
                state.drawerHasTranslucency,
                BlockModelRenderState.EMPTY_TINTS,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();

        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public final List<BlockStateModelPart> drawerModelParts = new ArrayList<>();
        public Direction facing = Direction.NORTH;
        public float progress;
        public boolean drawerHasTranslucency;
        public float drawerTravelScale = 1.0F;
        public float drawerTravelLimit = VanityBlock.BASE_DRAWER_TRAVEL_DISTANCE;
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1.0F - progress;
        return 1.0F - inverse * inverse * inverse;
    }
}
