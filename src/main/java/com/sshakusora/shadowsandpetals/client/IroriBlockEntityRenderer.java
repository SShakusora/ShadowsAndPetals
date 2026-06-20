package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.IroriBlockEntity;
import net.minecraft.client.Minecraft;
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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class IroriBlockEntityRenderer implements BlockEntityRenderer<IroriBlockEntity, IroriBlockEntityRenderer.State> {
    private static final long FIREWOOD_RENDER_SEED = 42L;
    private static final int FULL_ALPHA = 255;
    private static final double FIREWOOD_Y_OFFSET = 10.0D / 16.0D;
    private static final double BURNING_OVERLAY_Y_OFFSET = 10.01D / 16.0D;
    private static final double FIREWOOD_JITTER_MAX = 4.0D / 16.0D;
    private static final double FIREWOOD_APPEAR_FALL_DISTANCE = 5.0D / 16.0D;
    private static final float BREATHING_CYCLE_TICKS = 60.0F;
    private static final int BREATHING_LIGHT_MIN = 5;
    private static final int BREATHING_LIGHT_MAX = 13;
    private static final float BURNING_QUAD_MIN = 0;
    private static final float BURNING_QUAD_MAX = 1.0F;
    private static final Direction[] DIRECTIONS = Direction.values();
    private final RandomSource firewoodRandom = RandomSource.create(FIREWOOD_RENDER_SEED);
    private final RandomSource transformRandom = RandomSource.create(FIREWOOD_RENDER_SEED);
    private final Map<IroriBlockEntity.FirewoodModel, CachedFirewoodModel> firewoodModelCache =
            new EnumMap<>(IroriBlockEntity.FirewoodModel.class);
    private @Nullable TextureAtlasSprite burningSprite;

    public IroriBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            IroriBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        resetFrameState(state);

        if (!blockEntity.shouldRenderFirewood()) {
            clearFirewoodModel(state);
            return;
        }

        IroriBlockEntity.FirewoodModel firewoodModel = blockEntity.getFirewoodModel();
        if (firewoodModel == null) {
            clearFirewoodModel(state);
            return;
        }

        state.firewoodAppearProgress = blockEntity.getFirewoodAppearProgress(partialTicks);
        state.firewoodAlpha = computeFirewoodAlpha(state.firewoodAppearProgress);

        IroriBlockEntity.FirewoodRenderOffset renderOffset = blockEntity.getFirewoodRenderOffset();
        state.firewoodOffsetX = renderOffset.x();
        state.firewoodOffsetZ = renderOffset.z();

        updateFirewoodTransform(blockEntity, state);

        BlockStateModel model = BlockModelRegistry.getIroriFirewoodModel(firewoodModel);
        if (model == null) {
            clearFirewoodModel(state);
            return;
        }

        applyCachedFirewoodModel(blockEntity, state, firewoodModel, model);

        int burnTime = blockEntity.getBurnTime();
        state.burnTime = burnTime;
        if (burnTime > 0 && blockEntity.getLevel() != null) {
            state.burningSprite = ensureBurningSprite();
            float smoothTime = blockEntity.getLevel().getGameTime() + partialTicks;
            float breathPhase = (smoothTime % BREATHING_CYCLE_TICKS) / BREATHING_CYCLE_TICKS;
            float breathFactor = (float) ((Math.sin(breathPhase * Math.PI * 2.0) + 1.0) / 2.0);
            state.burningBreathLight = LightCoordsUtil.pack(
                    (int) (BREATHING_LIGHT_MIN + (BREATHING_LIGHT_MAX - BREATHING_LIGHT_MIN) * breathFactor),
                    0
            );
        } else {
            state.burningBreathLight = 0;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.burnTime > 0 && state.burningSprite != null && state.burningBreathLight > 0) {
            poseStack.pushPose();
            poseStack.translate(
                    state.firewoodOffsetX + state.firewoodJitterX,
                    BURNING_OVERLAY_Y_OFFSET,
                    state.firewoodOffsetZ + state.firewoodJitterZ
            );
            int lightCoords = state.burningBreathLight;
            TextureAtlasSprite sprite = state.burningSprite;
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    Sheets.translucentBlockSheet(),
                    (pose, buffer) -> {
                        VertexConsumer v = sprite.wrap(buffer);
                        v.addVertex(pose, BURNING_QUAD_MIN, 0.0F, BURNING_QUAD_MIN).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0F, 1.0F, 0.0F);
                        v.addVertex(pose, BURNING_QUAD_MIN, 0.0F, BURNING_QUAD_MAX).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0F, 1.0F, 0.0F);
                        v.addVertex(pose, BURNING_QUAD_MAX, 0.0F, BURNING_QUAD_MAX).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0F, 1.0F, 0.0F);
                        v.addVertex(pose, BURNING_QUAD_MAX, 0.0F, BURNING_QUAD_MIN).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0F, 1.0F, 0.0F);
                    }
            );
            poseStack.popPose();
        }

        if (state.firewoodModelParts.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(
                state.firewoodOffsetX + state.firewoodJitterX,
                FIREWOOD_Y_OFFSET + (1.0F - state.firewoodAppearProgress) * FIREWOOD_APPEAR_FALL_DISTANCE,
                state.firewoodOffsetZ + state.firewoodJitterZ
        );
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.firewoodRotationY));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        if (state.firewoodAlpha >= FULL_ALPHA) {
            submitNodeCollector.submitMultiLayerBlockModel(
                    poseStack,
                    state.firewoodModelParts,
                    state.firewoodHasTranslucency,
                    BlockModelRenderState.EMPTY_TINTS,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );
        } else if (state.firewoodAlpha > 0) {
            int color = ARGB.color(state.firewoodAlpha, 255, 255, 255);
            int lightCoords = state.lightCoords;
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    Sheets.translucentBlockSheet(),
                    (pose, buffer) -> renderFirewoodWithAlpha(pose, buffer, state.firewoodModelParts, color, lightCoords)
            );
        }
        poseStack.popPose();
    }

    private static int computeFirewoodAlpha(float progress) {
        float clampedProgress = Math.clamp(progress, 0.0F, 1.0F);
        return Math.clamp(Math.round(clampedProgress * FULL_ALPHA), 0, FULL_ALPHA);
    }

    private static void resetFrameState(State state) {
        state.firewoodOffsetX = 0.0D;
        state.firewoodOffsetZ = 0.0D;
        state.firewoodAppearProgress = 1.0F;
        state.firewoodAlpha = FULL_ALPHA;
    }

    private void updateFirewoodTransform(IroriBlockEntity blockEntity, State state) {
        long blockPosSeed = blockEntity.getBlockPos().asLong();
        transformRandom.setSeed(blockPosSeed ^ FIREWOOD_RENDER_SEED);
        state.firewoodRotationY = transformRandom.nextFloat() * 360.0F;
        state.firewoodJitterX = 0.0D;
        state.firewoodJitterZ = 0.0D;
        if (blockEntity.isComponentWideAndDeep()) {
            state.firewoodJitterX = (transformRandom.nextDouble() * 2.0D - 1.0D) * FIREWOOD_JITTER_MAX;
            state.firewoodJitterZ = (transformRandom.nextDouble() * 2.0D - 1.0D) * FIREWOOD_JITTER_MAX;
        }
    }

    private void applyCachedFirewoodModel(IroriBlockEntity blockEntity, State state,
                                          IroriBlockEntity.FirewoodModel firewoodModel, BlockStateModel model) {
        var level = blockEntity.getLevel();
        if (level == null) return;
        var tintGetter = (BlockAndTintGetter) level;
        var blockPos = blockEntity.getBlockPos();
        var blockState = blockEntity.getBlockState();

        CachedFirewoodModel cached = firewoodModelCache.get(firewoodModel);
        if (cached == null || cached.model() != model) {
            List<BlockStateModelPart> parts = new ArrayList<>();
            firewoodRandom.setSeed(FIREWOOD_RENDER_SEED);
            model.collectParts(tintGetter, blockPos, blockState, firewoodRandom, parts);
            cached = new CachedFirewoodModel(
                    model,
                    List.copyOf(parts),
                    model.hasMaterialFlag(tintGetter, blockPos, blockState, BakedQuad.FLAG_TRANSLUCENT)
            );
            firewoodModelCache.put(firewoodModel, cached);
        }

        state.firewoodModelParts = cached.parts();
        state.firewoodHasTranslucency = cached.hasTranslucency();
    }

    private static void clearFirewoodModel(State state) {
        state.firewoodModelParts = List.of();
        state.firewoodHasTranslucency = false;
    }

    private TextureAtlasSprite ensureBurningSprite() {
        if (burningSprite != null) {
            return burningSprite;
        }

        TextureAtlas blocksAtlas = (TextureAtlas) Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);
        burningSprite = blocksAtlas.getSprite(
                ShadowsAndPetals.asResource("block/irori/firewood/burning")
        );
        return burningSprite;
    }

    private static void renderFirewoodWithAlpha(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            List<BlockStateModelPart> modelParts,
            int color,
            int lightCoords
    ) {
        QuadInstance instance = new QuadInstance();
        instance.setColor(color);
        instance.setLightCoords(lightCoords);
        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        for (BlockStateModelPart part : modelParts) {
            for (Direction direction : DIRECTIONS) {
                for (BakedQuad quad : part.getQuads(direction)) {
                    buffer.putBakedQuad(pose, quad, instance);
                }
            }

            for (BakedQuad quad : part.getQuads(null)) {
                buffer.putBakedQuad(pose, quad, instance);
            }
        }
    }

    public static class State extends BlockEntityRenderState {
        public List<BlockStateModelPart> firewoodModelParts = List.of();
        public boolean firewoodHasTranslucency;
        public double firewoodOffsetX;
        public double firewoodOffsetZ;
        public double firewoodJitterX;
        public double firewoodJitterZ;
        public float firewoodAppearProgress = 1.0F;
        public float firewoodRotationY;
        public int firewoodAlpha = FULL_ALPHA;
        public int burnTime;
        public int burningBreathLight;
        public @Nullable TextureAtlasSprite burningSprite;
    }

    private record CachedFirewoodModel(
            BlockStateModel model,
            List<BlockStateModelPart> parts,
            boolean hasTranslucency
    ) {}
}
