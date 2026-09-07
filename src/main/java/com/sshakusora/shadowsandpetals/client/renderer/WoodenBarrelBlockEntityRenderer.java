package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.shadowsandpetals.block.decoration.WoodenBarrelBlock;
import com.sshakusora.shadowsandpetals.blockentity.WoodenBarrelBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public class WoodenBarrelBlockEntityRenderer implements BlockEntityRenderer<WoodenBarrelBlockEntity, WoodenBarrelBlockEntityRenderer.State> {
    private static final float FLUID_LEVEL_ANIMATION_RATE = 0.32F;
    private static final float FLUID_LEVEL_SNAP_EPSILON = 0.5F;
    private static final double MAX_ANIMATION_GAP_TICKS = 20.0D;

    private final ClientFluidRenderInfo.Cache<WoodenBarrelBlockEntity> fluidRenderInfoCache =
            new ClientFluidRenderInfo.Cache<>();
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
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.fluidSprite = null;
        state.displayedAmount = 0.0F;
        state.capacity = WoodenBarrelBlockEntity.FLUID_CAPACITY;
        state.fluidColor = 0;
        state.fluidLightEmission = 0;
        state.axis = blockEntity.getBlockState().getValue(WoodenBarrelBlock.AXIS);

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
        float surfaceY = Mth.lerp(
                fillRatio,
                WoodenBarrelFluidGeometry.MIN_SURFACE_Y,
                WoodenBarrelFluidGeometry.MAX_SURFACE_Y
        );
        int lightCoords = ClientFluidRenderInfo.applyLightEmission(
                state.lightCoords,
                state.fluidLightEmission
        );

        TextureAtlasSprite sprite = state.fluidSprite;
        poseStack.pushPose();
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                Sheets.translucentBlockSheet(),
                (pose, buffer) -> WoodenBarrelFluidGeometry.renderSurface(
                        buffer,
                        pose,
                        sprite,
                        surfaceY,
                        state.fluidColor,
                        lightCoords,
                        state.axis
                )
        );
        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public float displayedAmount;
        public int capacity;
        public int fluidColor;
        public int fluidLightEmission;
        public Direction.Axis axis = Direction.Axis.Z;
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
