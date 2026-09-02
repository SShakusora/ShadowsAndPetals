package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.CurtainBlock;
import com.sshakusora.shadowsandpetals.blockentity.CurtainBlockEntity;
import com.sshakusora.shadowsandpetals.client.animation.AnimatedBlockModel;
import com.sshakusora.shadowsandpetals.client.animation.AnimationControllerEvaluator;
import com.sshakusora.shadowsandpetals.client.animation.AnimationResourceRef;
import com.sshakusora.shadowsandpetals.client.animation.RigPose;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Experimental renderer for the two-block curtain. Submits the per-bone baked
 * models of the matching half through its resource-driven animation rig.
 */
public class CurtainBlockEntityRenderer implements BlockEntityRenderer<CurtainBlockEntity, CurtainBlockEntityRenderer.State> {
    private static final RandomSource PART_COLLECT_RANDOM = RandomSource.create(42L);
    private static final int[] TINTS = new int[0];
    /** Beyond this local time the clip has clamped to its final keyframe. */
    private static final float FALLBACK_END_POSE_SECONDS = 1.0F;

    private static final AnimationResourceRef.Rig UPPER_RIG =
            new AnimationResourceRef.Rig(ShadowsAndPetals.asResource("animation/curtain_upper_r"));
    private static final AnimationResourceRef.Rig LOWER_RIG =
            new AnimationResourceRef.Rig(ShadowsAndPetals.asResource("animation/curtain_lower_r"));

    private static final String[] UPPER_BONES = {
            "g1", "g1_1", "g2", "g2_1", "g3", "g3_1", "g4", "g4_1", "group"
    };
    private static final String[] LOWER_BONES = {
            "g1", "g2", "g3", "g4"
    };

    private @Nullable AnimatedBlockModel cachedUpperModel;
    private @Nullable AnimatedBlockModel cachedLowerModel;

    public CurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(CurtainBlockEntity blockEntity) {
        // The closed curtain folds beyond the block face; keep the whole
        // moving volume inside the render culling box.
        return new AABB(blockEntity.getBlockPos()).inflate(0.25D);
    }

    @Override
    public void extractRenderState(
            CurtainBlockEntity blockEntity, State state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.facing = blockEntity.getBlockState().getValue(CurtainBlock.FACING);
        boolean upper = blockEntity.getBlockState().getValue(CurtainBlock.HALF) == DoubleBlockHalf.UPPER;
        // The block entity carries OPEN and the animation clock in one data
        // packet, so it is the authoritative animation state; the block-state
        // property can be observed one packet earlier or later.
        state.open = blockEntity.isOpen();
        state.animationPose = null;
        state.model = null;
        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockAndTintGetter tintGetter = (BlockAndTintGetter) blockEntity.getLevel();
        AnimatedBlockModel model = upper
                ? resolveUpperModel(tintGetter, blockEntity)
                : resolveLowerModel(tintGetter, blockEntity);
        if (model == null) {
            return;
        }

        float seconds = blockEntity.transitionTimeSeconds(
                blockEntity.getLevel().getGameTime(), partialTicks);
        if (seconds < 0.0F || seconds > FALLBACK_END_POSE_SECONDS) {
            // No transition recorded yet, or the animation finished long ago
            // (including right after a world reload): hold the authored end
            // pose of the current state instead of sampling a stale clock.
            seconds = FALLBACK_END_POSE_SECONDS;
        }
        state.animationPose = AnimationControllerEvaluator.sample(
                upper ? UPPER_RIG.id() : LOWER_RIG.id(),
                state.open ? "on" : "off",
                seconds
        );
        state.model = model;
    }

    private AnimatedBlockModel resolveUpperModel(BlockAndTintGetter tintGetter, CurtainBlockEntity blockEntity) {
        BlockStateModel[] models = {
                BlockModelRegistry.CURTAIN_G1.get(),
                BlockModelRegistry.CURTAIN_G1_1.get(),
                BlockModelRegistry.CURTAIN_G2.get(),
                BlockModelRegistry.CURTAIN_G2_1.get(),
                BlockModelRegistry.CURTAIN_G3.get(),
                BlockModelRegistry.CURTAIN_G3_1.get(),
                BlockModelRegistry.CURTAIN_G4.get(),
                BlockModelRegistry.CURTAIN_G4_1.get(),
                BlockModelRegistry.CURTAIN_GROUP.get()
        };
        if (cachedUpperModel != null) {
            return cachedUpperModel;
        }
        AnimatedBlockModel model = bakeModel(tintGetter, blockEntity, UPPER_RIG, UPPER_BONES, models);
        cachedUpperModel = model;
        return model;
    }

    private AnimatedBlockModel resolveLowerModel(BlockAndTintGetter tintGetter, CurtainBlockEntity blockEntity) {
        BlockStateModel[] models = {
                BlockModelRegistry.CURTAIN_LOWER_G1.get(),
                BlockModelRegistry.CURTAIN_LOWER_G2.get(),
                BlockModelRegistry.CURTAIN_LOWER_G3.get(),
                BlockModelRegistry.CURTAIN_LOWER_G4.get()
        };
        if (cachedLowerModel != null) {
            return cachedLowerModel;
        }
        AnimatedBlockModel model = bakeModel(tintGetter, blockEntity, LOWER_RIG, LOWER_BONES, models);
        cachedLowerModel = model;
        return model;
    }

    private static AnimatedBlockModel bakeModel(
            BlockAndTintGetter tintGetter,
            CurtainBlockEntity blockEntity,
            AnimationResourceRef.Rig rig,
            String[] boneNames,
            BlockStateModel[] models
    ) {
        var blockState = blockEntity.getBlockState();
        BlockPos pos = blockEntity.getBlockPos();
        List<AnimatedBlockModel.Binding> bindings = new ArrayList<>(boneNames.length);
        boolean hasAnyParts = false;
        for (int index = 0; index < boneNames.length; index++) {
            BlockStateModel model = models[index];
            if (model == null) {
                continue;
            }
            List<BlockStateModelPart> parts = new ArrayList<>();
            PART_COLLECT_RANDOM.setSeed(42L);
            model.collectParts(tintGetter, pos, blockState, PART_COLLECT_RANDOM, parts);
            if (parts.isEmpty()) {
                continue;
            }
            hasAnyParts = true;
            boolean hasTranslucency = model.hasMaterialFlag(
                    tintGetter, pos, blockState, BakedQuad.FLAG_TRANSLUCENT
            );
            bindings.add(new AnimatedBlockModel.Binding(
                    rig, boneNames[index], List.copyOf(parts), hasTranslucency, TINTS));
        }
        return hasAnyParts ? new AnimatedBlockModel(rig, bindings) : null;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        AnimatedBlockModel model = state.model;
        RigPose pose = state.animationPose;
        if (model == null || pose == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        model.submit(pose, poseStack, submitNodeCollector, state.lightCoords);

        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean open = true;
        public @Nullable RigPose animationPose;
        public @Nullable AnimatedBlockModel model;
    }
}