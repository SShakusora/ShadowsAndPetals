package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.LargeCurtainBlock;
import com.sshakusora.shadowsandpetals.blockentity.LargeCurtainBlockEntity;
import com.sshakusora.shadowsandpetals.client.animation.AnimatedBlockModel;
import com.sshakusora.shadowsandpetals.client.animation.AnimationControllerEvaluator;
import com.sshakusora.shadowsandpetals.client.animation.AnimationResourceRef;
import com.sshakusora.shadowsandpetals.client.animation.RigPose;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.registry.StandaloneBlockModel;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renderer for the single-block large curtain. Submits the whole closed
 * per-bone model family through the shared animation rig while ANIMATING;
 * outside the window the static block-state model renders the curtain.
 */
public class LargeCurtainBlockEntityRenderer implements BlockEntityRenderer<LargeCurtainBlockEntity, LargeCurtainBlockEntityRenderer.State> {
    private static final RandomSource PART_COLLECT_RANDOM = RandomSource.create(42L);
    private static final int[] TINTS = new int[0];
    /** Beyond this local time the clip has clamped to its final keyframe. */
    private static final float FALLBACK_END_POSE_SECONDS = 1.0F;

    private static final AnimationResourceRef.Rig RIG =
            new AnimationResourceRef.Rig(ShadowsAndPetals.asResource("large_curtain/right"));

    private static final String[] BONES = BlockModelRegistry.LARGE_CURTAIN_BONES;
    /** Lazily baked whole-rig model; the geometry is side/color-agnostic. */
    private @Nullable AnimatedBlockModel modelCache;
    /** Lazily baked mirrored rig model for the left half. */
    private @Nullable AnimatedBlockModel modelCacheLeft;

    public LargeCurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(LargeCurtainBlockEntity blockEntity) {
        // The anchor renders the whole 2x2 rig: cover the partner cells and
        // the fabric bunching beyond them.
        return new AABB(blockEntity.getBlockPos()).inflate(1.5D);
    }

    @Override
    public void extractRenderState(
            LargeCurtainBlockEntity blockEntity, State state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.getValue(LargeCurtainBlock.FACING);
        state.side = blockState.getValue(LargeCurtainBlock.SIDE);
        state.animationPose = null;
        state.model = null;
        // Outside the animation window every block renders its own static
        // quadrant model; during it only the anchor's renderer draws the
        // whole rig, so the moving curtain is submitted exactly once.
        if (!blockState.getValue(LargeCurtainBlock.ANIMATING)
                || !blockState.getValue(LargeCurtainBlock.ANCHOR)
                || blockEntity.getLevel() == null) {
            return;
        }
        boolean stateOpen = blockState.getValue(LargeCurtainBlock.OPEN);
        boolean beSynced = blockEntity.isOpen() == stateOpen;
        state.open = beSynced ? blockEntity.isOpen() : stateOpen;

        BlockAndTintGetter tintGetter = (BlockAndTintGetter) blockEntity.getLevel();
        boolean left = state.side == LargeCurtainBlock.Side.LEFT;
        AnimatedBlockModel model = left
                ? (modelCacheLeft != null ? modelCacheLeft : (modelCacheLeft = bakeModel(tintGetter, blockEntity, true)))
                : (modelCache != null ? modelCache : (modelCache = bakeModel(tintGetter, blockEntity, false)));
        if (model == null) {
            return;
        }

        float seconds = blockEntity.transitionTimeSeconds(
                blockEntity.getLevel().getGameTime(), partialTicks);
        boolean holdEndPose = !beSynced || seconds < 0.0F || seconds > FALLBACK_END_POSE_SECONDS;
        if (holdEndPose) {
            // The block entity and block state disagree (external state
            // change), no transition was recorded yet, or the animation
            // finished long ago: hold the authored end pose of the current
            // state instead of sampling a stale clock.
            seconds = FALLBACK_END_POSE_SECONDS;
        }
        state.animationPose = AnimationControllerEvaluator.sample(
                RIG.id(),
                state.open ? "open" : "closed",
                seconds
        );
        state.model = model;
    }

    /** The dye color of the placed curtain block, white for unknown states. */
    private static DyeColor dyeColorOf(BlockState blockState) {
        Block block = blockState.getBlock();
        for (DyeColor color : DyeColor.values()) {
            if (block == BlockRegistry.LARGE_CURTAINS.get(color).get()) {
                return color;
            }
        }
        return DyeColor.WHITE;
    }

    private static AnimatedBlockModel bakeModel(
            BlockAndTintGetter tintGetter,
            LargeCurtainBlockEntity blockEntity,
            boolean mirrorX
    ) {
        BlockState blockState = blockEntity.getBlockState();
        BlockPos pos = blockEntity.getBlockPos();
        Map<String, StandaloneBlockModel> boneModels = mirrorX
                ? BlockModelRegistry.LARGE_CURTAIN_LEFT_BONE_MODELS
                : BlockModelRegistry.LARGE_CURTAIN_BONE_MODELS;
        List<AnimatedBlockModel.Binding> bindings = new ArrayList<>(BONES.length);
        boolean hasAnyParts = false;
        for (String bone : BONES) {
            StandaloneBlockModel model = boneModels.get(bone);
            if (model == null) {
                continue;
            }
            BlockStateModel baked = model.get();
            if (baked == null) {
                continue;
            }
            List<BlockStateModelPart> parts = new ArrayList<>();
            PART_COLLECT_RANDOM.setSeed(42L);
            baked.collectParts(tintGetter, pos, blockState, PART_COLLECT_RANDOM, parts);
            if (parts.isEmpty()) {
                continue;
            }
            hasAnyParts = true;
            boolean hasTranslucency = baked.hasMaterialFlag(
                    tintGetter, pos, blockState, BakedQuad.FLAG_TRANSLUCENT
            );
            bindings.add(new AnimatedBlockModel.Binding(
                    RIG, bone, List.copyOf(parts), hasTranslucency, TINTS, mirrorX));
        }
        return hasAnyParts ? new AnimatedBlockModel(RIG, bindings) : null;
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
        // Same rotation the block-state model uses; the rig was authored for
        // the RIGHT curtain, and the left half mirrors it through the binding.
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facing.toYRot()));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        model.submit(pose, poseStack, submitNodeCollector, state.lightCoords);

        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public LargeCurtainBlock.Side side = LargeCurtainBlock.Side.RIGHT;
        public boolean open = true;
        public @Nullable RigPose animationPose;
        public @Nullable AnimatedBlockModel model;
    }
}