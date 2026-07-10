package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.WindChimeBlock;
import com.sshakusora.shadowsandpetals.blockentity.WindChimeBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindChimeBlockEntityRenderer implements BlockEntityRenderer<WindChimeBlockEntity, WindChimeBlockEntityRenderer.State> {
    private static final RandomSource PART_RANDOM = RandomSource.create(42L);
    private static final float PIVOT = 8.0F / 16.0F;
    private static final float MODEL_OFFSET_Y = -4.0F / 16.0F;
    private static final float BODY_PIVOT_Y = 1.0F;
    private static final float MAIN_PIVOT_Y = 1.0F + MODEL_OFFSET_Y;
    private static final float FULL_CIRCLE = (float) (Math.PI * 2.0D);
    private static final long PROFILE_SALT = 0x9E3779B97F4A7C15L;
    private static final long BODY_Y_WANDER_SALT = 0xD1B54A32D192ED03L;
    private static final long MAIN_Y_WANDER_SALT = 0xABC98388FB8FAC03L;
    private static final long WANDER_STEP = 0x9E3779B97F4A7C15L;
    private static final int BODY_Y_WANDER_INTERVAL = 120;
    private static final int MAIN_Y_WANDER_INTERVAL = 75;
    private static final float BODY_Y_WANDER_AMPLITUDE = 10.0F;
    private static final float MAIN_Y_WANDER_AMPLITUDE = 18.0F;
    private final Map<DyeColor, CachedParts> cachedBodyParts = new HashMap<>();
    private final Map<WindChimeColors, CachedParts> cachedHangingParts = new HashMap<>();

    public WindChimeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(WindChimeBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos())
                .expandTowards(0.0D, -0.5D, 0.0D);
    }

    @Override
    public void extractRenderState(
            WindChimeBlockEntity blockEntity, State state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        if (blockEntity.getLevel() == null) {
            return;
        }

        long gameTime = blockEntity.getLevel().getGameTime();
        updateMotionProfile(state, blockEntity.getBlockPos().asLong());
        float secondaryWave = sampleWave(gameTime, partialTicks, state.timeScale, 0.031F, state.secondaryPhase);
        float bodyYWander = sampleWander(
                gameTime, partialTicks, state.profilePosition,
                BODY_Y_WANDER_SALT, BODY_Y_WANDER_INTERVAL
        ) * BODY_Y_WANDER_AMPLITUDE;
        float mainYWander = sampleWander(
                gameTime, partialTicks, state.profilePosition,
                MAIN_Y_WANDER_SALT, MAIN_Y_WANDER_INTERVAL
        ) * MAIN_Y_WANDER_AMPLITUDE;
        float naturalMotionWeight = blockEntity.getNaturalMotionWeight(partialTicks);
        state.colors = blockEntity.getColors();
        state.axisRotation = blockEntity.getBlockState().getValue(WindChimeBlock.HORIZONTAL_AXIS) == Direction.Axis.X
                ? 90.0F
                : 0.0F;
        state.bodySwingX = naturalMotionWeight * state.bodyAmplitude * (
                sampleWave(gameTime, partialTicks, state.timeScale, 0.075F, state.phaseX) * 2.5F
                        + secondaryWave * 0.8F * state.secondaryAmplitude
        )
                + blockEntity.getBodyX(partialTicks);
        state.bodySwingY = naturalMotionWeight
                * (sampleWave(gameTime, partialTicks, state.timeScale, 0.024F, state.phaseY) * 3.0F * state.bodyAmplitude + bodyYWander)
                + blockEntity.getBodyY(partialTicks);
        state.bodySwingZ = naturalMotionWeight
                * sampleWave(gameTime, partialTicks, state.timeScale, 0.061F, state.phaseZ + 1.8F) * 2.0F * state.bodyAmplitude
                + blockEntity.getBodyZ(partialTicks);
        state.mainSwingX = naturalMotionWeight
                * sampleWave(gameTime, partialTicks, state.timeScale, 0.075F, state.phaseX - 0.65F) * 4.0F * state.mainAmplitude
                + blockEntity.getMainX(partialTicks);
        state.mainSwingY = naturalMotionWeight
                * (sampleWave(gameTime, partialTicks, state.timeScale, 0.029F, state.phaseY - 0.8F) * 7.0F * state.mainAmplitude
                + mainYWander)
                + blockEntity.getMainY(partialTicks);
        state.mainSwingZ = naturalMotionWeight
                * sampleWave(gameTime, partialTicks, state.timeScale, 0.061F, state.phaseZ + 1.15F) * 3.0F * state.mainAmplitude
                + blockEntity.getMainZ(partialTicks);

        BlockStateModel bodyModel = BlockModelRegistry.getWindChimeBodyModel(state.colors.ribbon());
        BlockStateModel mainRibbonModel = BlockModelRegistry.getWindChimeMainRibbonModel(state.colors.ribbon());
        BlockStateModel vaneModel = BlockModelRegistry.getWindChimeVaneModel(state.colors.vane());
        if (bodyModel == null || mainRibbonModel == null || vaneModel == null) {
            state.bodyParts = List.of();
            state.hangingParts = List.of();
            return;
        }
        BlockAndTintGetter tintGetter = (BlockAndTintGetter) blockEntity.getLevel();
        CachedParts body = cachedBodyParts.computeIfAbsent(
                state.colors.ribbon(),
                ignored -> collectParts(tintGetter, blockEntity, bodyModel));
        CachedParts hanging = cachedHangingParts.computeIfAbsent(
                state.colors,
                ignored -> collectParts(tintGetter, blockEntity, mainRibbonModel, vaneModel));
        state.bodyParts = body.parts();
        state.bodyHasTranslucency = body.hasTranslucency();
        state.hangingParts = hanging.parts();
        state.hangingHasTranslucency = hanging.hasTranslucency();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.bodyParts.isEmpty() || state.hangingParts.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(PIVOT, 0.0F, PIVOT);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.axisRotation));
        poseStack.translate(-PIVOT, 0.0F, -PIVOT);
        poseStack.translate(PIVOT, BODY_PIVOT_Y, PIVOT);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.bodySwingX));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.bodySwingY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.bodySwingZ));
        poseStack.translate(-PIVOT, -BODY_PIVOT_Y, -PIVOT);
        collector.submitMultiLayerBlockModel(
                poseStack, state.bodyParts, state.bodyHasTranslucency, state.tints,
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.pushPose();
        poseStack.translate(PIVOT, MAIN_PIVOT_Y, PIVOT);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.mainSwingX));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.mainSwingY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.mainSwingZ));
        poseStack.translate(-PIVOT, -MAIN_PIVOT_Y, -PIVOT);
        poseStack.translate(0.0F, MODEL_OFFSET_Y, 0.0F);
        collector.submitMultiLayerBlockModel(
                poseStack, state.hangingParts, state.hangingHasTranslucency, state.tints,
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public List<BlockStateModelPart> bodyParts = List.of();
        public List<BlockStateModelPart> hangingParts = List.of();
        public final int[] tints = new int[0];
        public WindChimeColors colors = WindChimeColors.DEFAULT;
        public boolean bodyHasTranslucency;
        public boolean hangingHasTranslucency;
        public float axisRotation;
        public float bodySwingX;
        public float bodySwingY;
        public float bodySwingZ;
        public float mainSwingX;
        public float mainSwingY;
        public float mainSwingZ;
        private long profilePosition = Long.MIN_VALUE;
        private float phaseX;
        private float phaseY;
        private float phaseZ;
        private float secondaryPhase;
        private float timeScale;
        private float bodyAmplitude;
        private float mainAmplitude;
        private float secondaryAmplitude;
    }

    private static void updateMotionProfile(State state, long position) {
        if (state.profilePosition == position) {
            return;
        }

        long phaseBits = mix64(position);
        long profileBits = mix64(phaseBits + PROFILE_SALT);
        state.profilePosition = position;
        state.phaseX = sample01(phaseBits, 0) * FULL_CIRCLE;
        state.phaseY = sample01(phaseBits, 16) * FULL_CIRCLE;
        state.phaseZ = sample01(phaseBits, 32) * FULL_CIRCLE;
        state.secondaryPhase = sample01(phaseBits, 48) * FULL_CIRCLE;
        state.timeScale = sampleRange(profileBits, 0, 0.94F, 1.06F);
        state.bodyAmplitude = sampleRange(profileBits, 16, 0.90F, 1.10F);
        state.mainAmplitude = sampleRange(profileBits, 32, 0.85F, 1.15F);
        state.secondaryAmplitude = sampleRange(profileBits, 48, 0.85F, 1.15F);
    }

    private static CachedParts collectParts(
            BlockAndTintGetter tintGetter, WindChimeBlockEntity blockEntity, BlockStateModel model
    ) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        PART_RANDOM.setSeed(42L);
        model.collectParts(tintGetter, blockEntity.getBlockPos(), blockEntity.getBlockState(), PART_RANDOM, parts);
        boolean hasTranslucency = model.hasMaterialFlag(
                tintGetter, blockEntity.getBlockPos(), blockEntity.getBlockState(), BakedQuad.FLAG_TRANSLUCENT);
        return new CachedParts(List.copyOf(parts), hasTranslucency);
    }

    private static CachedParts collectParts(
            BlockAndTintGetter tintGetter, WindChimeBlockEntity blockEntity,
            BlockStateModel firstModel, BlockStateModel secondModel
    ) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        PART_RANDOM.setSeed(42L);
        firstModel.collectParts(tintGetter, blockEntity.getBlockPos(), blockEntity.getBlockState(), PART_RANDOM, parts);
        PART_RANDOM.setSeed(42L);
        secondModel.collectParts(tintGetter, blockEntity.getBlockPos(), blockEntity.getBlockState(), PART_RANDOM, parts);
        boolean hasTranslucency = firstModel.hasMaterialFlag(
                tintGetter, blockEntity.getBlockPos(), blockEntity.getBlockState(), BakedQuad.FLAG_TRANSLUCENT)
                || secondModel.hasMaterialFlag(
                tintGetter, blockEntity.getBlockPos(), blockEntity.getBlockState(), BakedQuad.FLAG_TRANSLUCENT);
        return new CachedParts(List.copyOf(parts), hasTranslucency);
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;
        value *= 0xC4CEB9FE1A85EC53L;
        return value ^ value >>> 33;
    }

    private static float sample01(long bits, int shift) {
        return ((bits >>> shift) & 0xFFFFL) / 65535.0F;
    }

    private static float sampleRange(long bits, int shift, float min, float max) {
        return min + (max - min) * sample01(bits, shift);
    }

    private static float sampleWave(
            long gameTime, float partialTicks, float timeScale, float frequency, float phase
    ) {
        double angularSpeed = (double) timeScale * frequency;
        double angle = ((double) gameTime * angularSpeed + phase) % (Math.PI * 2.0D);
        angle += partialTicks * angularSpeed;
        return (float) Math.sin(angle);
    }

    private static float sampleWander(
            long gameTime, float partialTicks, long position, long salt, int intervalLength
    ) {
        long interval = Math.floorDiv(gameTime, intervalLength);
        float progress = (Math.floorMod(gameTime, intervalLength) + partialTicks) / intervalLength;
        float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
        float from = sampleSigned(mix64(position ^ salt ^ interval * WANDER_STEP));
        float to = sampleSigned(mix64(position ^ salt ^ (interval + 1L) * WANDER_STEP));
        return Mth.lerp(smoothProgress, from, to);
    }

    private static float sampleSigned(long bits) {
        return ((bits >>> 40) & 0xFFFFFFL) / 8388607.5F - 1.0F;
    }

    private record CachedParts(List<BlockStateModelPart> parts, boolean hasTranslucency) {
    }
}
