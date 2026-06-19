package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.ShishiOdoshiBlock;
import com.sshakusora.shadowsandpetals.blockentity.ShishiOdoshiBlockEntity;
import net.minecraft.client.renderer.Sheets;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShishiOdoshiBlockEntityRenderer implements BlockEntityRenderer<ShishiOdoshiBlockEntity, ShishiOdoshiBlockEntityRenderer.State> {
    private static final RandomSource PART_COLLECT_RANDOM = RandomSource.create(42L);
    private static final float MAIN_PIVOT_X = 8.0F / 16.0F;
    private static final float MAIN_PIVOT_Y = 9.0F / 16.0F;
    private static final float MAIN_PIVOT_Z = 9.0F / 16.0F;
    private static final float MAIN_OUTLET_X = 8.0F;
    private static final float MAIN_OUTLET_Y = 10.881578F;
    private static final float MAIN_OUTLET_Z = 3.259766F;
    private static final float MAIN_INSIDE_Y = 8.111086F;
    private static final float MAIN_INSIDE_Z = 8.581831F;
    private static final float POUR_BOTTOM_Y = 3.02F / 16.0F;
    private static final float POUR_HALF_WIDTH = 0.48F / 16.0F;
    private static final float POUR_STRIP_LENGTH = 3.5F / 16.0F;
    private static final float POUR_FACE_OFFSET = 0.01F / 16.0F;
    private static final float TUBE_EXIT_DURATION = ShishiOdoshiBlockEntity.TIPPING_DURATION - ShishiOdoshiBlockEntity.POUR_START_TICK;
    private static final float FLOW_UV_SCALE = 0.5F;
    private static final float FLOW_U_CENTER = 0.5F;
    public ShishiOdoshiBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            ShishiOdoshiBlockEntity blockEntity, State state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        var blockState = blockEntity.getBlockState();
        state.facing = blockState.getValue(ShishiOdoshiBlock.FACING);
        state.tipAngle = blockEntity.getTipAngle(partialTicks);
        state.pourProgress = blockEntity.getPourProgress(partialTicks);
        if (blockEntity.getLevel() == null) {
            state.cachedMainModel = null;
            state.mainModelParts.clear();
            return;
        }

        BlockStateModel mainModel = BlockModelRegistry.getShishiOdoshiMainModel();
        if (mainModel == null) {
            state.cachedMainModel = null;
            state.mainModelParts.clear();
            return;
        }

        var tintGetter = (BlockAndTintGetter) blockEntity.getLevel();
        var fluidRenderInfo = ShishiOdoshiFluidRenderInfo.create(
                blockEntity.getFluid(), tintGetter, blockEntity.getBlockPos()
        );
        state.fluidSprite = fluidRenderInfo == null ? null : fluidRenderInfo.sprite();
        state.waterColor = fluidRenderInfo == null ? 0xD0FFFFFF : fluidRenderInfo.color();
        updatePourPath(state);

        if (state.cachedMainModel != mainModel) {
            state.mainModelParts.clear();
            PART_COLLECT_RANDOM.setSeed(42L);
            mainModel.collectParts(tintGetter, blockEntity.getBlockPos(), blockState, PART_COLLECT_RANDOM, state.mainModelParts);
            state.mainHasTranslucency = mainModel.hasMaterialFlag(tintGetter, blockEntity.getBlockPos(), blockState, BakedQuad.FLAG_TRANSLUCENT);
            state.cachedMainModel = mainModel;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.mainModelParts.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        poseStack.pushPose();
        poseStack.translate(MAIN_PIVOT_X, MAIN_PIVOT_Y, MAIN_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.tipAngle));
        poseStack.translate(-MAIN_PIVOT_X, -MAIN_PIVOT_Y, -MAIN_PIVOT_Z);
        submitNodeCollector.submitMultiLayerBlockModel(
                poseStack,
                state.mainModelParts,
                state.mainHasTranslucency,
                BlockModelRenderState.EMPTY_TINTS,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();

        if (state.pourProgress >= 0.0F && state.pourProgress < 1.0F) {
            TextureAtlasSprite sprite = state.fluidSprite;
            if (sprite == null) {
                poseStack.popPose();
                return;
            }
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    Sheets.translucentBlockSheet(),
                    (pose, buffer) -> renderPouringStream(buffer, pose, sprite, state)
            );
        }
        poseStack.popPose();
    }

    private static void updatePourPath(State state) {
        float elapsedTicks = Math.max(0.0F, state.pourProgress) * ShishiOdoshiBlockEntity.POUR_DURATION;
        float pathAngle = elapsedTicks > TUBE_EXIT_DURATION
                ? ShishiOdoshiBlockEntity.MAX_TIP_ANGLE
                : state.tipAngle;
        float radians = pathAngle * Mth.DEG_TO_RAD;
        float cos = Mth.cos(radians);
        float sin = Mth.sin(radians);
        state.pourX = MAIN_OUTLET_X / 16.0F;
        state.insideY = rotateY(MAIN_INSIDE_Y / 16.0F, MAIN_INSIDE_Z / 16.0F, cos, sin);
        state.insideZ = rotateZ(MAIN_INSIDE_Y / 16.0F, MAIN_INSIDE_Z / 16.0F, cos, sin);
        state.outletY = rotateY(MAIN_OUTLET_Y / 16.0F, MAIN_OUTLET_Z / 16.0F, cos, sin);
        state.outletZ = rotateZ(MAIN_OUTLET_Y / 16.0F, MAIN_OUTLET_Z / 16.0F, cos, sin);
    }

    private static float rotateY(float y, float z, float cos, float sin) {
        return MAIN_PIVOT_Y + cos * (y - MAIN_PIVOT_Y) - sin * (z - MAIN_PIVOT_Z);
    }

    private static float rotateZ(float y, float z, float cos, float sin) {
        return MAIN_PIVOT_Z + sin * (y - MAIN_PIVOT_Y) + cos * (z - MAIN_PIVOT_Z);
    }

    private static void renderPouringStream(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            State state
    ) {
        float minX = state.pourX - POUR_HALF_WIDTH;
        float maxX = state.pourX + POUR_HALF_WIDTH;
        float tubeLength = Mth.sqrt(
                Mth.square(state.outletY - state.insideY)
                        + Mth.square(state.outletZ - state.insideZ)
        );
        float fallingLength = state.outletY - POUR_BOTTOM_Y;
        float totalLength = tubeLength + fallingLength;
        if (tubeLength <= 0.0F || fallingLength <= 0.0F || totalLength <= POUR_STRIP_LENGTH) {
            return;
        }

        float halfU = (maxX - minX) * FLOW_UV_SCALE * 0.5F;
        float minU = FLOW_U_CENTER - halfU;
        float maxU = FLOW_U_CENTER + halfU;
        // At the start of the return motion the trailing edge has just cleared
        // the outlet, so the whole fixed-length strip is already outside.
        // Constant arc-length speed across both the tube and falling portions.
        // Four ticks move one full tube length, so the trailing edge clears the
        // outlet exactly when the main model starts returning.
        float flowSpeed = tubeLength / TUBE_EXIT_DURATION;
        float elapsedTicks = Math.max(0.0F, state.pourProgress) * ShishiOdoshiBlockEntity.POUR_DURATION;
        float headDistance = POUR_STRIP_LENGTH + elapsedTicks * flowSpeed;
        float tailDistance = headDistance - POUR_STRIP_LENGTH;

        float tubeStart = Math.max(0.0F, tailDistance);
        float tubeEnd = Math.min(tubeLength, headDistance);
        if (tubeEnd > tubeStart) {
            float startProgress = tubeStart / tubeLength;
            float endProgress = tubeEnd / tubeLength;
            renderWaterRibbonSegment(
                    buffer, pose, sprite, minX, maxX,
                    Mth.lerp(startProgress, state.insideY, state.outletY),
                    Mth.lerp(startProgress, state.insideZ, state.outletZ),
                    Mth.lerp(endProgress, state.insideY, state.outletY),
                    Mth.lerp(endProgress, state.insideZ, state.outletZ),
                    minU, maxU,
                    (tubeStart - tailDistance) * FLOW_UV_SCALE,
                    (tubeEnd - tailDistance) * FLOW_UV_SCALE,
                    state.waterColor, state.lightCoords,
                    true
            );
        }

        float fallingStart = Math.max(tubeLength, tailDistance);
        float fallingEnd = Math.min(totalLength, headDistance);
        if (fallingEnd > fallingStart) {
            renderWaterRibbonSegment(
                    buffer, pose, sprite, minX, maxX,
                    state.outletY - (fallingStart - tubeLength), state.outletZ,
                    state.outletY - (fallingEnd - tubeLength), state.outletZ,
                    minU, maxU,
                    (fallingStart - tailDistance) * FLOW_UV_SCALE,
                    (fallingEnd - tailDistance) * FLOW_UV_SCALE,
                    state.waterColor, state.lightCoords,
                    true
            );
        }
    }

    private static void renderWaterRibbonSegment(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float minX,
            float maxX,
            float startY,
            float startZ,
            float endY,
            float endZ,
            float minU,
            float maxU,
            float startV,
            float endV,
            int color,
            int lightCoords,
            boolean doubleSided
    ) {
        float deltaY = endY - startY;
        float deltaZ = endZ - startZ;
        float inverseLength = Mth.invSqrt(deltaY * deltaY + deltaZ * deltaZ);
        float normalY = -deltaZ * inverseLength;
        float normalZ = deltaY * inverseLength;

        float frontOffsetY = -normalY * POUR_FACE_OFFSET;
        float frontOffsetZ = -normalZ * POUR_FACE_OFFSET;

        addWaterVertex(buffer, pose, sprite, minX, startY + frontOffsetY, startZ + frontOffsetZ, color, minU, startV, lightCoords, -normalY, -normalZ);
        addWaterVertex(buffer, pose, sprite, minX, endY + frontOffsetY, endZ + frontOffsetZ, color, minU, endV, lightCoords, -normalY, -normalZ);
        addWaterVertex(buffer, pose, sprite, maxX, endY + frontOffsetY, endZ + frontOffsetZ, color, maxU, endV, lightCoords, -normalY, -normalZ);
        addWaterVertex(buffer, pose, sprite, maxX, startY + frontOffsetY, startZ + frontOffsetZ, color, maxU, startV, lightCoords, -normalY, -normalZ);

        if (!doubleSided) {
            return;
        }

        float backOffsetY = normalY * POUR_FACE_OFFSET;
        float backOffsetZ = normalZ * POUR_FACE_OFFSET;

        addWaterVertex(buffer, pose, sprite, maxX, startY + backOffsetY, startZ + backOffsetZ, color, maxU, startV, lightCoords, normalY, normalZ);
        addWaterVertex(buffer, pose, sprite, maxX, endY + backOffsetY, endZ + backOffsetZ, color, maxU, endV, lightCoords, normalY, normalZ);
        addWaterVertex(buffer, pose, sprite, minX, endY + backOffsetY, endZ + backOffsetZ, color, minU, endV, lightCoords, normalY, normalZ);
        addWaterVertex(buffer, pose, sprite, minX, startY + backOffsetY, startZ + backOffsetZ, color, minU, startV, lightCoords, normalY, normalZ);
    }

    private static void addWaterVertex(
            VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x, float y, float z, int color, float u, float v, int lightCoords,
            float normalY, float normalZ
    ) {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, normalY, normalZ);
    }

    public static class State extends BlockEntityRenderState {
        public final List<BlockStateModelPart> mainModelParts = new ArrayList<>();
        public Direction facing = Direction.NORTH;
        public float tipAngle;
        public float pourProgress = -1.0F;
        public float pourX;
        public float insideY;
        public float insideZ;
        public float outletY;
        public float outletZ;
        public int waterColor;
        public @Nullable TextureAtlasSprite fluidSprite;
        public boolean mainHasTranslucency;
        private @Nullable BlockStateModel cachedMainModel;
    }
}
