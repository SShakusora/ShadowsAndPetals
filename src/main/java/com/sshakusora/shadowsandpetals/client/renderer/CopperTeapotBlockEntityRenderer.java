package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.CopperTeapotBlock;
import com.sshakusora.shadowsandpetals.blockentity.CopperTeapotBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.util.MathUtils;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CopperTeapotBlockEntityRenderer implements
        BlockEntityRenderer<CopperTeapotBlockEntity, CopperTeapotBlockEntityRenderer.State> {
    private static final RandomSource PART_COLLECT_RANDOM = RandomSource.create(42L);

    private @Nullable BlockStateModel cachedLidModel;
    private List<BlockStateModelPart> cachedLidModelParts = List.of();
    private boolean cachedLidHasTranslucency;

    public CopperTeapotBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            CopperTeapotBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTicks, cameraPosition, breakProgress);

        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.getValue(CopperTeapotBlock.FACING);
        state.lidProgress = blockEntity.getLidProgress(partialTicks);
        state.onIrori = blockState.getValue(CopperTeapotBlock.ON_IRORI);

        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockStateModel lidModel = BlockModelRegistry.getCopperTeapotLidModel();
        if (lidModel == null) {
            state.lidModelParts = List.of();
            return;
        }

        BlockAndTintGetter tintGetter = (BlockAndTintGetter) blockEntity.getLevel();
        if (cachedLidModel != lidModel) {
            List<BlockStateModelPart> parts = new ArrayList<>();
            PART_COLLECT_RANDOM.setSeed(42L);
            lidModel.collectParts(
                    tintGetter, blockEntity.getBlockPos(), blockState, PART_COLLECT_RANDOM, parts);
            cachedLidModelParts = List.copyOf(parts);
            cachedLidHasTranslucency = lidModel.hasMaterialFlag(
                    tintGetter, blockEntity.getBlockPos(), blockState, BakedQuad.FLAG_TRANSLUCENT);
            cachedLidModel = lidModel;
        }

        state.lidModelParts = cachedLidModelParts;
        state.lidHasTranslucency = cachedLidHasTranslucency;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (state.lidModelParts.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        poseStack.translate(
                0.0D,
                (state.onIrori ? CopperTeapotBlock.IRORI_RENDER_OFFSET : 0.0D)
                        + MathUtils.easeOutCubic(state.lidProgress) * CopperTeapotBlockEntity.MAX_LID_LIFT,
                0.0D
        );
        submitNodeCollector.submitMultiLayerBlockModel(
                poseStack,
                state.lidModelParts,
                state.lidHasTranslucency,
                BlockModelRenderState.EMPTY_TINTS,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CopperTeapotBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D);
    }

    public static class State extends BlockEntityRenderState {
        public List<BlockStateModelPart> lidModelParts = List.of();
        public Direction facing = Direction.NORTH;
        public float lidProgress;
        public boolean lidHasTranslucency;
        public boolean onIrori;
    }
}
