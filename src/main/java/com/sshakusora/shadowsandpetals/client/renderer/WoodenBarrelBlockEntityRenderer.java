package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sshakusora.shadowsandpetals.blockentity.WoodenBarrelBlockEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public class WoodenBarrelBlockEntityRenderer implements BlockEntityRenderer<WoodenBarrelBlockEntity, WoodenBarrelBlockEntityRenderer.State> {
    private static final float FLUID_LEVEL_ANIMATION_RATE = 0.32F;
    private static final float FLUID_LEVEL_SNAP_EPSILON = 0.5F;
    private static final double MAX_ANIMATION_GAP_TICKS = 20.0D;

    private static final float MIN_X = 4.5F / 16.0F;
    private static final float MAX_X = 11.5F / 16.0F;
    private static final float MIN_Z = 4.5F / 16.0F;
    private static final float MAX_Z = 11.5F / 16.0F;
    private static final float MIN_SURFACE_Y = 1.05F / 16.0F;
    private static final float MAX_SURFACE_Y = 8.45F / 16.0F;
    private static final float SURFACE_TEXTURE_MAX_U = 7.0F / 16.0F;
    private static final float SURFACE_TEXTURE_MAX_V = 7.0F / 16.0F;

    private final ShishiOdoshiFluidRenderInfo.Cache<WoodenBarrelBlockEntity> fluidRenderInfoCache =
            new ShishiOdoshiFluidRenderInfo.Cache<>();
    private final Map<WoodenBarrelBlockEntity, FluidLevelAnimation> fluidLevelAnimations = new WeakHashMap<>();

    public WoodenBarrelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            WoodenBarrelBlockEntity blockEntity,
            State state,
            float partialTicks,
            net.minecraft.world.phys.Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.fluidSprite = null;
        state.displayedAmount = 0.0F;
        state.capacity = WoodenBarrelBlockEntity.FLUID_CAPACITY;
        state.fluidColor = 0;
        state.fluidLightEmission = 0;

        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        FluidResource resource = blockEntity.getFluidTank().getResource(0);
        int amount = Math.max(0, blockEntity.getFluidTank().getAmountAsInt(0));
        Fluid fluid = resource.isEmpty() || amount == 0 ? Fluids.EMPTY : resource.getFluid();
        double currentTime = level.getGameTime() + partialTicks;
        FluidLevelAnimation animation = fluidLevelAnimations.computeIfAbsent(
                blockEntity,
                ignored -> new FluidLevelAnimation()
        );
        FluidLevelAnimation.Sample sample = animation.update(fluid, amount, currentTime);
        if (sample.fluid() == Fluids.EMPTY || sample.displayedAmount() <= 0.0F) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        var renderInfo = fluidRenderInfoCache.getSurface(
                blockEntity,
                sample.fluid(),
                (BlockAndTintGetter) level,
                pos
        );

        state.displayedAmount = sample.displayedAmount();
        state.fluidSprite = renderInfo.sprite();
        state.fluidColor = renderInfo.color();
        state.fluidLightEmission = renderInfo.lightEmission();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (state.fluidSprite == null || state.displayedAmount <= 0.0F) {
            return;
        }

        float fillRatio = Mth.clamp(
                state.displayedAmount / (float) state.capacity,
                0.0F,
                1.0F
        );
        float surfaceY = Mth.lerp(fillRatio, MIN_SURFACE_Y, MAX_SURFACE_Y);
        int lightCoords = ShishiOdoshiFluidRenderInfo.applyLightEmission(
                state.lightCoords,
                state.fluidLightEmission
        );

        TextureAtlasSprite sprite = state.fluidSprite;
        poseStack.pushPose();
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                Sheets.translucentBlockSheet(),
                (pose, buffer) -> renderSurface(
                        buffer,
                        pose,
                        sprite,
                        surfaceY,
                        state.fluidColor,
                        lightCoords
                )
        );
        poseStack.popPose();
    }

    private static void renderSurface(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float surfaceY,
            int color,
            int lightCoords
    ) {
        addVertex(buffer, pose, sprite, MIN_X, surfaceY, MIN_Z, color, 0.0F, 0.0F, lightCoords, 0.0F, 1.0F, 0.0F);
        addVertex(buffer, pose, sprite, MIN_X, surfaceY, MAX_Z, color, 0.0F, SURFACE_TEXTURE_MAX_V, lightCoords, 0.0F, 1.0F, 0.0F);
        addVertex(buffer, pose, sprite, MAX_X, surfaceY, MAX_Z, color, SURFACE_TEXTURE_MAX_U, SURFACE_TEXTURE_MAX_V, lightCoords, 0.0F, 1.0F, 0.0F);
        addVertex(buffer, pose, sprite, MAX_X, surfaceY, MIN_Z, color, SURFACE_TEXTURE_MAX_U, 0.0F, lightCoords, 0.0F, 1.0F, 0.0F);

        addVertex(buffer, pose, sprite, MAX_X, surfaceY, MIN_Z, color, SURFACE_TEXTURE_MAX_U, 0.0F, lightCoords, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, pose, sprite, MAX_X, surfaceY, MAX_Z, color, SURFACE_TEXTURE_MAX_U, SURFACE_TEXTURE_MAX_V, lightCoords, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, pose, sprite, MIN_X, surfaceY, MAX_Z, color, 0.0F, SURFACE_TEXTURE_MAX_V, lightCoords, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, pose, sprite, MIN_X, surfaceY, MIN_Z, color, 0.0F, 0.0F, lightCoords, 0.0F, -1.0F, 0.0F);
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

    public static class State extends BlockEntityRenderState {
        public float displayedAmount;
        public int capacity;
        public int fluidColor;
        public int fluidLightEmission;
        public @Nullable TextureAtlasSprite fluidSprite;
    }

    /**
     * Client-only visual state. The server amount remains the source of truth;
     * this cache only smooths the surface between synchronized amounts.
     */
    private static final class FluidLevelAnimation {
        private Fluid visualFluid = Fluids.EMPTY;
        private float displayedAmount;
        private double lastUpdateTime;
        private boolean initialized;

        private Sample update(Fluid targetFluid, int targetAmount, double currentTime) {
            if (!initialized) {
                initialized = true;
                lastUpdateTime = currentTime;
                visualFluid = targetAmount > 0 ? targetFluid : Fluids.EMPTY;
                displayedAmount = targetAmount;
                return new Sample(visualFluid, displayedAmount);
            }

            double deltaTicks = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
            if (deltaTicks < 0.0D || deltaTicks > MAX_ANIMATION_GAP_TICKS) {
                visualFluid = targetAmount > 0 ? targetFluid : Fluids.EMPTY;
                displayedAmount = targetAmount;
                return new Sample(visualFluid, displayedAmount);
            }

            if (targetAmount > 0 && visualFluid != targetFluid) {
                visualFluid = targetFluid;
                displayedAmount = 0.0F;
            }

            float alpha = 1.0F - (float) Math.exp(-FLUID_LEVEL_ANIMATION_RATE * deltaTicks);
            displayedAmount += (targetAmount - displayedAmount) * alpha;
            if (Math.abs(displayedAmount - targetAmount) < FLUID_LEVEL_SNAP_EPSILON) {
                displayedAmount = targetAmount;
            }

            if (targetAmount == 0 && displayedAmount <= FLUID_LEVEL_SNAP_EPSILON) {
                displayedAmount = 0.0F;
                visualFluid = Fluids.EMPTY;
            }

            return new Sample(visualFluid, displayedAmount);
        }

        private record Sample(Fluid fluid, float displayedAmount) {
        }
    }
}
