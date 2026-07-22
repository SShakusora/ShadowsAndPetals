package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.api.shishiOdoshi.ShishiOdoshiFluidRegistry;
import com.sshakusora.shadowsandpetals.block.decoration.ShishiOdoshiPipeBlock;
import com.sshakusora.shadowsandpetals.blockentity.ShishiOdoshiBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.ShishiOdoshiPipeBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ShishiOdoshiPipeBlockEntityRenderer implements BlockEntityRenderer<ShishiOdoshiPipeBlockEntity, ShishiOdoshiPipeBlockEntityRenderer.State> {
    private static final float WATER_SURFACE_Y = 1.02F / 16.0F;
    private static final float STREAM_Y_TOP = WATER_SURFACE_Y;
    private static final float MAIN_OPENING_Y = 10.863334F;
    private static final float MAIN_OPENING_Z = 4.377651F;
    private static final float MAIN_PIVOT_Y = 9.0F;
    private static final float MAIN_PIVOT_Z = 9.0F;
    private static final float DEFAULT_STREAM_Y_BOTTOM = (MAIN_OPENING_Y - 16.0F) / 16.0F;
    private static final float SOURCE_Z = 15.98F / 16.0F;
    private static final float SURFACE_OFFSET = 0.02F / 16.0F;
    private static final float INNER_STREAM_HALF_WIDTH = 0.48F / 16.0F;
    private static final float FALLING_STREAM_HALF_WIDTH = 0.48F / 16.0F;
    private static final float STREAM_FADE_LENGTH = 2.0F / 16.0F;
    private static final float FLOW_UV_SCALE = 0.5F;
    private static final float FALLING_STREAM_SEGMENT_LENGTH = 1.0F / FLOW_UV_SCALE;
    private static final float FLOW_U_CENTER = 0.5F;
    private final ShishiOdoshiFluidRenderInfo.Cache<ShishiOdoshiPipeBlockEntity> fluidRenderInfoCache =
            new ShishiOdoshiFluidRenderInfo.Cache<>();

    public ShishiOdoshiPipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(ShishiOdoshiPipeBlockEntity blockEntity) {
        ShishiOdoshiBlockEntity shishiOdoshi = blockEntity.getConnectedShishiOdoshi();
        double streamBottomY;
        if (shishiOdoshi != null) {
            streamBottomY = shishiOdoshi.getBlockPos().getY();
        } else {
            Vec3 impactPosition = blockEntity.getFallbackImpactPosition();
            streamBottomY = impactPosition == null
                    ? blockEntity.getBlockPos().getY() - 1.0D
                    : impactPosition.y;
        }
        double downwardExpansion = Math.max(1.0D, blockEntity.getBlockPos().getY() - streamBottomY);
        return new AABB(blockEntity.getBlockPos()).expandTowards(0.0D, -downwardExpansion, 0.0D);
    }

    @Override
    public boolean shouldRender(ShishiOdoshiPipeBlockEntity blockEntity, Vec3 cameraPosition) {
        double viewDistance = getViewDistance();
        return getRenderBoundingBox(blockEntity).distanceToSqr(cameraPosition) < viewDistance * viewDistance;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            ShishiOdoshiPipeBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.getValue(ShishiOdoshiPipeBlock.FACING);
        state.length = blockState.getValue(ShishiOdoshiPipeBlock.LENGTH);
        state.shouldRenderWater = false;
        state.fluidSprite = null;
        state.streamBottomY = DEFAULT_STREAM_Y_BOTTOM;

        if (blockEntity.getLevel() == null) {
            return;
        }

        ShishiOdoshiBlockEntity shishiOdoshi = blockEntity.getConnectedShishiOdoshi();
        if (shishiOdoshi != null) {
            int verticalDistance = blockEntity.getBlockPos().getY() - shishiOdoshi.getBlockPos().getY();
            state.streamBottomY = getAnimatedOpeningY(
                    shishiOdoshi.getTipAngle(partialTicks), verticalDistance
            );
        } else {
            Vec3 impactPosition = blockEntity.getFallbackImpactPosition();
            if (impactPosition != null) {
                state.streamBottomY = (float) (impactPosition.y - blockEntity.getBlockPos().getY());
            }
        }

        BlockPos sourcePos = blockEntity.getBlockPos().relative(state.facing.getOpposite());
        var fluid = ShishiOdoshiFluidRegistry.findSourceFluid(blockEntity.getLevel(), sourcePos);
        if (fluid == null) {
            return;
        }

        var renderInfo = fluidRenderInfoCache.get(
                blockEntity,
                fluid, (BlockAndTintGetter) blockEntity.getLevel(), sourcePos
        );
        if (renderInfo != null) {
            state.shouldRenderWater = true;
            state.fluidSprite = renderInfo.sprite();
            state.waterColor = renderInfo.color();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.shouldRenderWater) {
            return;
        }

        TextureAtlasSprite sprite = state.fluidSprite;
        if (sprite == null) {
            return;
        }

        PipeChannel channel = PipeChannel.forLength(state.length);
        int lightCoords = state.lightCoords;
        int waterColor = state.waterColor;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        // The unrotated model is the north-facing blockstate variant.
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        submitNodeCollector.submitCustomGeometry(
                poseStack,
                Sheets.translucentBlockSheet(),
                (pose, buffer) -> {
                    renderInnerStreamQuad(buffer, pose, sprite, lightCoords, waterColor, channel);
                    renderFallingStreamQuad(buffer, pose, sprite, lightCoords, waterColor, channel, state.streamBottomY);
                }
        );

        poseStack.popPose();
    }

    private static void renderInnerStreamQuad(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int lightCoords,
            int color,
            PipeChannel channel
    ) {
        float minX = channel.centerX - INNER_STREAM_HALF_WIDTH;
        float maxX = channel.centerX + INNER_STREAM_HALF_WIDTH;
        // Meet the falling sheet on the same edge so no gap is visible at the lip.
        float minZ = channel.outletZ - SURFACE_OFFSET;
        float maxZ = SOURCE_Z;
        float halfU = (maxX - minX) * FLOW_UV_SCALE * 0.5F;
        float minU = FLOW_U_CENTER - halfU;
        float maxU = FLOW_U_CENTER + halfU;
        float sourceV = 0.0F;
        float mouthV = (maxZ - minZ) * FLOW_UV_SCALE;

        addVertex(buffer, pose, sprite, minX, WATER_SURFACE_Y, minZ, color, minU, mouthV, lightCoords, 0.0F, 1.0F, 0.0F);
        addVertex(buffer, pose, sprite, minX, WATER_SURFACE_Y, maxZ, color, minU, sourceV, lightCoords, 0.0F, 1.0F, 0.0F);
        addVertex(buffer, pose, sprite, maxX, WATER_SURFACE_Y, maxZ, color, maxU, sourceV, lightCoords, 0.0F, 1.0F, 0.0F);
        addVertex(buffer, pose, sprite, maxX, WATER_SURFACE_Y, minZ, color, maxU, mouthV, lightCoords, 0.0F, 1.0F, 0.0F);

        addVertex(buffer, pose, sprite, maxX, WATER_SURFACE_Y, minZ, color, maxU, mouthV, lightCoords, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, pose, sprite, maxX, WATER_SURFACE_Y, maxZ, color, maxU, sourceV, lightCoords, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, pose, sprite, minX, WATER_SURFACE_Y, maxZ, color, minU, sourceV, lightCoords, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, pose, sprite, minX, WATER_SURFACE_Y, minZ, color, minU, mouthV, lightCoords, 0.0F, -1.0F, 0.0F);
    }

    private static void renderFallingStreamQuad(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int lightCoords,
            int color,
            PipeChannel channel,
            float streamBottomY
    ) {
        float minX = channel.centerX - FALLING_STREAM_HALF_WIDTH;
        float maxX = channel.centerX + FALLING_STREAM_HALF_WIDTH;
        float minY = streamBottomY;
        float maxY = STREAM_Y_TOP;
        float z = channel.outletZ - SURFACE_OFFSET;
        float halfU = (maxX - minX) * FLOW_UV_SCALE * 0.5F;
        float minU = FLOW_U_CENTER - halfU;
        float maxU = FLOW_U_CENTER + halfU;
        float fadeStartY = Math.min(maxY, minY + STREAM_FADE_LENGTH);
        float segmentTopY = maxY;
        while (segmentTopY > minY) {
            float segmentBottomY = Math.max(minY, segmentTopY - FALLING_STREAM_SEGMENT_LENGTH);
            renderFallingStreamSegment(
                    buffer, pose, sprite, minX, maxX, segmentTopY, segmentBottomY, z,
                    minU, maxU, 0.0F, (segmentTopY - segmentBottomY) * FLOW_UV_SCALE,
                    getFadedStreamColor(color, segmentTopY, minY, fadeStartY),
                    getFadedStreamColor(color, segmentBottomY, minY, fadeStartY),
                    lightCoords
            );
            segmentTopY = segmentBottomY;
        }
    }

    private static int getFadedStreamColor(int color, float y, float bottomY, float fadeStartY) {
        if (y >= fadeStartY) {
            return color;
        }
        float alphaScale = fadeStartY == bottomY
                ? 0.0F
                : Mth.clamp((y - bottomY) / (fadeStartY - bottomY), 0.0F, 1.0F);
        int alpha = Math.round((color >>> 24) * alphaScale);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static void renderFallingStreamSegment(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float minX,
            float maxX,
            float topY,
            float bottomY,
            float z,
            float minU,
            float maxU,
            float topV,
            float bottomV,
            int topColor,
            int bottomColor,
            int lightCoords
    ) {
        addVertex(buffer, pose, sprite, minX, topY, z, topColor, minU, topV, lightCoords, 0.0F, 0.0F, 1.0F);
        addVertex(buffer, pose, sprite, minX, bottomY, z, bottomColor, minU, bottomV, lightCoords, 0.0F, 0.0F, 1.0F);
        addVertex(buffer, pose, sprite, maxX, bottomY, z, bottomColor, maxU, bottomV, lightCoords, 0.0F, 0.0F, 1.0F);
        addVertex(buffer, pose, sprite, maxX, topY, z, topColor, maxU, topV, lightCoords, 0.0F, 0.0F, 1.0F);

        addVertex(buffer, pose, sprite, maxX, topY, z, topColor, maxU, topV, lightCoords, 0.0F, 0.0F, -1.0F);
        addVertex(buffer, pose, sprite, maxX, bottomY, z, bottomColor, maxU, bottomV, lightCoords, 0.0F, 0.0F, -1.0F);
        addVertex(buffer, pose, sprite, minX, bottomY, z, bottomColor, minU, bottomV, lightCoords, 0.0F, 0.0F, -1.0F);
        addVertex(buffer, pose, sprite, minX, topY, z, topColor, minU, topV, lightCoords, 0.0F, 0.0F, -1.0F);
    }

    private static void addVertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int lightCoords,
            float normalX,
            float normalY,
            float normalZ
    ) {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private static float getAnimatedOpeningY(float tipAngle, int verticalDistance) {
        float radians = tipAngle * Mth.DEG_TO_RAD;
        float relativeY = MAIN_OPENING_Y - MAIN_PIVOT_Y;
        float relativeZ = MAIN_OPENING_Z - MAIN_PIVOT_Z;
        float rotatedY = MAIN_PIVOT_Y
                + Mth.cos(radians) * relativeY
                - Mth.sin(radians) * relativeZ;
        return rotatedY / 16.0F - verticalDistance;
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public ShishiOdoshiPipeBlock.PipeLength length = ShishiOdoshiPipeBlock.PipeLength.NORMAL;
        public boolean shouldRenderWater;
        public int waterColor;
        public @Nullable TextureAtlasSprite fluidSprite;
        public float streamBottomY = DEFAULT_STREAM_Y_BOTTOM;
    }

    private record PipeChannel(float centerX, float outletZ) {
        private static PipeChannel forLength(ShishiOdoshiPipeBlock.PipeLength length) {
            return new PipeChannel(length.outletX(), length.outletZ());
        }
    }
}
