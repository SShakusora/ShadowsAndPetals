package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.CurtainBlock;
import com.sshakusora.shadowsandpetals.blockentity.CurtainBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.client.animation.AnimatedBlockModel;
import com.sshakusora.shadowsandpetals.client.animation.AnimationControllerEvaluator;
import com.sshakusora.shadowsandpetals.client.animation.AnimationResourceRef;
import com.sshakusora.shadowsandpetals.client.animation.RigPose;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.registry.StandaloneBlockModelSet;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Experimental renderer for the two-block curtain. Submits the per-bone baked
 * models of the matching half and side through its resource-driven animation
 * rig.
 */
public class CurtainBlockEntityRenderer implements BlockEntityRenderer<CurtainBlockEntity, CurtainBlockEntityRenderer.State> {
    private static final RandomSource PART_COLLECT_RANDOM = RandomSource.create(42L);
    private static final int[] TINTS = new int[0];
    /** Beyond this local time the clip has clamped to its final keyframe. */
    private static final float FALLBACK_END_POSE_SECONDS = 1.0F;

    private static final AnimationResourceRef.Rig UPPER_R_RIG =
            new AnimationResourceRef.Rig(ShadowsAndPetals.asResource("animation/curtain_upper_r"));
    private static final AnimationResourceRef.Rig LOWER_R_RIG =
            new AnimationResourceRef.Rig(ShadowsAndPetals.asResource("animation/curtain_lower_r"));
    private static final AnimationResourceRef.Rig UPPER_L_RIG =
            new AnimationResourceRef.Rig(ShadowsAndPetals.asResource("animation/curtain_upper_l"));
    private static final AnimationResourceRef.Rig LOWER_L_RIG =
            new AnimationResourceRef.Rig(ShadowsAndPetals.asResource("animation/curtain_lower_l"));

    private static final String[] UPPER_BONES = BlockModelRegistry.CURTAIN_UPPER_BONES;
    private static final String[] LOWER_BONES = BlockModelRegistry.CURTAIN_LOWER_BONES;

    /** Baked per-bone models keyed by (half, side, dye color). */
    private final Map<CurtainVariant, AnimatedBlockModel> cachedModels = new HashMap<>();

    private record CurtainVariant(boolean upper, boolean left, DyeColor color) {
    }

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
        boolean left = blockEntity.getBlockState().getValue(CurtainBlock.SIDE) == CurtainBlock.Side.LEFT;
        // The block entity carries OPEN and the animation clock in one data
        // packet, seeded by its constructor from the block state. When it
        // disagrees with the block-state property (external state change
        // before the entity synced), the block state wins for the pose.
        boolean stateOpen = blockEntity.getBlockState().getValue(CurtainBlock.OPEN);
        boolean beSynced = blockEntity.isOpen() == stateOpen;
        state.open = beSynced ? blockEntity.isOpen() : stateOpen;
        state.animationPose = null;
        state.model = null;
        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockAndTintGetter tintGetter = (BlockAndTintGetter) blockEntity.getLevel();
        AnimationResourceRef.Rig rig;
        if (upper) {
            rig = left ? UPPER_L_RIG : UPPER_R_RIG;
        } else {
            rig = left ? LOWER_L_RIG : LOWER_R_RIG;
        }
        StandaloneBlockModelSet<BlockModelRegistry.CurtainBoneKey> modelSet = upper
                ? (left ? BlockModelRegistry.CURTAIN_UPPER_L : BlockModelRegistry.CURTAIN_UPPER_R)
                : (left ? BlockModelRegistry.CURTAIN_LOWER_L : BlockModelRegistry.CURTAIN_LOWER_R);
        DyeColor color = dyeColorOf(blockEntity.getBlockState());
        AnimatedBlockModel model = resolveModel(
                tintGetter, blockEntity, rig, upper ? UPPER_BONES : LOWER_BONES, modelSet,
                new CurtainVariant(upper, left, color));
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
                rig.id(),
                state.open ? "on" : "off",
                seconds
        );
        state.model = model;
    }

    /** The dye color of the placed curtain block, white for unknown states. */
    private static DyeColor dyeColorOf(BlockState blockState) {
        Block block = blockState.getBlock();
        for (DyeColor color : DyeColor.values()) {
            if (block == BlockRegistry.CURTAINS.get(color).get()) {
                return color;
            }
        }
        return DyeColor.WHITE;
    }

    private AnimatedBlockModel resolveModel(
            BlockAndTintGetter tintGetter,
            CurtainBlockEntity blockEntity,
            AnimationResourceRef.Rig rig,
            String[] boneNames,
            StandaloneBlockModelSet<BlockModelRegistry.CurtainBoneKey> modelSet,
            CurtainVariant variant
    ) {
        AnimatedBlockModel cached = cachedModels.get(variant);
        if (cached != null) {
            return cached;
        }
        // Each bone binds its own per-bone model file: the set keys pair the
        // dye color with the bone name.
        BlockStateModel[] models = new BlockStateModel[boneNames.length];
        for (int index = 0; index < boneNames.length; index++) {
            models[index] = modelSet.get(
                    new BlockModelRegistry.CurtainBoneKey(variant.color(), boneNames[index]));
        }
        AnimatedBlockModel baked = bakeModel(tintGetter, blockEntity, rig, boneNames, models);
        cachedModels.put(variant, baked);
        return baked;
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