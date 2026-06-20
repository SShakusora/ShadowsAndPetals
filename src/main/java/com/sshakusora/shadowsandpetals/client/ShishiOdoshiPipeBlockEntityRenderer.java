package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.api.ShishiOdoshiFluidRegistry;
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
        return new AABB(blockEntity.getBlockPos()).expandTowards(0.0D, -1.0D, 0.0D);
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

        if (blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos().below()) instanceof ShishiOdoshiBlockEntity shishiOdoshi) {
            state.streamBottomY = getAnimatedOpeningY(shishiOdoshi.getTipAngle(partialTicks));
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
        float topV = 0.0F;
        float bottomV = (maxY - minY) * FLOW_UV_SCALE;
        float fadeStartY = Math.min(maxY, minY + STREAM_FADE_LENGTH);
        float fadeStartV = (maxY - fadeStartY) * FLOW_UV_SCALE;
        int transparentColor = color & 0x00FFFFFF;

        if (fadeStartY < maxY) {
            renderFallingStreamSegment(
                    buffer, pose, sprite, minX, maxX, maxY, fadeStartY, z,
                    minU, maxU, topV, fadeStartV, color, color, lightCoords
            );
        }
        renderFallingStreamSegment(
                buffer, pose, sprite, minX, maxX, fadeStartY, minY, z,
                minU, maxU, fadeStartV, bottomV, color, transparentColor, lightCoords
        );
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

    private static float getAnimatedOpeningY(float tipAngle) {
        float radians = tipAngle * Mth.DEG_TO_RAD;
        float relativeY = MAIN_OPENING_Y - MAIN_PIVOT_Y;
        float relativeZ = MAIN_OPENING_Z - MAIN_PIVOT_Z;
        float rotatedY = MAIN_PIVOT_Y
                + Mth.cos(radians) * relativeY
                - Mth.sin(radians) * relativeZ;
        return (rotatedY - 16.0F) / 16.0F;
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
            return switch (length) {
                case SHORT -> new PipeChannel(8.0F / 16.0F, 11.0F / 16.0F);
                case NORMAL, LONG -> new PipeChannel(8.0F / 16.0F, length == ShishiOdoshiPipeBlock.PipeLength.LONG ? 4.0F / 16.0F : 8.0F / 16.0F);
                case NORMAL_LEFT -> new PipeChannel(4.5F / 16.0F, 8.0F / 16.0F);
                case NORMAL_RIGHT -> new PipeChannel(11.5F / 16.0F, 8.0F / 16.0F);
            };
        }
    }
}
