package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowBlock;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowGeometry;
import com.sshakusora.shadowsandpetals.blockentity.CasementWindowBlockEntity;
import com.sshakusora.shadowsandpetals.util.MathUtils;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Renders the closed casement mesh while its invisible block state animates. */
public final class CasementWindowBlockEntityRenderer
        implements BlockEntityRenderer<CasementWindowBlockEntity,
        CasementWindowBlockEntityRenderer.State> {
    private static final RandomSource PART_COLLECT_RANDOM = RandomSource.create(42L);
    private final Map<BlockState, CachedModel> modelCache = new HashMap<>();

    @SuppressWarnings("unused")
    public CasementWindowBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(CasementWindowBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D);
    }

    @Override
    public void extractRenderState(
            CasementWindowBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTicks, cameraPosition, breakProgress);

        BlockState blockState = blockEntity.getBlockState();
        state.parts = List.of();
        state.hasTranslucency = false;
        state.angle = 0.0F;
        if (!blockState.getValue(CasementWindowBlock.ANIMATING)
                || blockEntity.getLevel() == null) {
            return;
        }

        boolean targetOpen = blockState.getValue(CasementWindowBlock.OPEN);
        float progress = blockEntity.transitionProgress(
                blockEntity.getLevel().getGameTime(), partialTicks);
        if (blockEntity.targetOpen() != targetOpen || progress < 0.0F) {
            // The clock packet is deliberately sent before the animating
            // block-state packet. Hold the invisible state until both agree.
            return;
        }

        progress = Math.min(progress, 1.0F);
        float eased = MathUtils.easeOutCubic(progress);
        var side = blockState.getValue(CasementWindowBlock.SIDE);
        float targetAngle = CasementWindowGeometry.targetAngle(side);
        state.angle = targetOpen
                ? targetAngle * eased
                : targetAngle * (1.0F - eased);

        BlockState closedState = blockState
                .setValue(CasementWindowBlock.OPEN, false)
                .setValue(CasementWindowBlock.ANIMATING, false);
        BlockAndTintGetter tintGetter = (BlockAndTintGetter) blockEntity.getLevel();
        CachedModel cached = modelCache.get(closedState);
        if (cached == null) {
            BlockStateModel model = Minecraft.getInstance()
                    .getModelManager()
                    .getBlockStateModelSet()
                    .get(closedState);
            List<BlockStateModelPart> parts = new ArrayList<>();
            PART_COLLECT_RANDOM.setSeed(42L);
            model.collectParts(
                    tintGetter,
                    blockEntity.getBlockPos(),
                    closedState,
                    PART_COLLECT_RANDOM,
                    parts
            );
            cached = new CachedModel(
                    List.copyOf(parts),
                    model.hasMaterialFlag(
                            tintGetter,
                            blockEntity.getBlockPos(),
                            closedState,
                            BakedQuad.FLAG_TRANSLUCENT
                    )
            );
            modelCache.put(closedState, cached);
        }
        state.parts = cached.parts;
        state.hasTranslucency = cached.hasTranslucency;
        state.pivot = CasementWindowGeometry.pivot(
                blockState.getValue(CasementWindowBlock.FACING), side);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (state.parts.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(state.pivot.x, state.pivot.y, state.pivot.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.angle));
        poseStack.translate(-state.pivot.x, -state.pivot.y, -state.pivot.z);
        submitNodeCollector.submitMultiLayerBlockModel(
                poseStack,
                state.parts,
                state.hasTranslucency,
                BlockModelRenderState.EMPTY_TINTS,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        private List<BlockStateModelPart> parts = List.of();
        private boolean hasTranslucency;
        private Vec3 pivot = new Vec3(0.5D, 0.5D, 0.5D);
        private float angle;
    }

    private record CachedModel(List<BlockStateModelPart> parts, boolean hasTranslucency) {
    }
}
