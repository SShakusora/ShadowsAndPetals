package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriComponentTopology;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState;
import com.sshakusora.shadowsandpetals.client.effect.IroriClientEffects;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
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
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
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
    private static final double FIREWOOD_APPEAR_FALL_DISTANCE = 5.0D / 16.0D;
    private static final float BREATHING_CYCLE_TICKS = 60.0F;
    private static final int BREATHING_LIGHT_MIN = 5;
    private static final int BREATHING_LIGHT_MAX = 13;
    private static final float BURNING_QUAD_MIN = 0;
    private static final float BURNING_QUAD_MAX = 1.0F;
    private static final double COOKING_ITEM_Y_OFFSET = 21.2D / 16.0D;
    private static final float COOKING_ITEM_SCALE = 0.5F;
    private static final double COOKING_ITEM_MAX_HORIZONTAL_JITTER = 1.0D / 16.0D;
    private static final long COOKING_ITEM_TRANSFORM_SALT = 0x49524F52494C4F4EL;
    private static final Direction[] DIRECTIONS = Direction.values();
    private final RandomSource firewoodRandom = RandomSource.create(FIREWOOD_RENDER_SEED);
    private final RandomSource transformRandom = RandomSource.create(FIREWOOD_RENDER_SEED);
    private final ItemModelResolver itemModelResolver;
    private final Map<IroriFuelState.FirewoodModel, CachedFirewoodModel> firewoodModelCache =
            new EnumMap<>(IroriFuelState.FirewoodModel.class);
    private final Map<IroriBlockEntity.GrillModel, CachedGrillModel> grillModelCache =
            new EnumMap<>(IroriBlockEntity.GrillModel.class);
    private @Nullable TextureAtlasSprite burningSprite;

    public IroriBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
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
        updateGrillRenderState(blockEntity, state);
        updateCookingItemRenderState(blockEntity, state);

        if (!blockEntity.shouldRenderFirewood()) {
            clearFirewoodModel(state);
            return;
        }

        IroriFuelState.FirewoodModel firewoodModel = blockEntity.getFirewoodModel();
        if (firewoodModel == null) {
            clearFirewoodModel(state);
            return;
        }

        state.firewoodAppearProgress = IroriClientEffects.getFirewoodAppearProgress(blockEntity, partialTicks);
        state.firewoodAlpha = computeFirewoodAlpha(state.firewoodAppearProgress);

        IroriBlockEntity.FirewoodRenderOffset renderOffset = blockEntity.getFirewoodRenderOffset();
        state.firewoodOffsetX = renderOffset.x();
        state.firewoodOffsetZ = renderOffset.z();

        updateFirewoodTransform(blockEntity, state);

        BlockStateModel model = BlockModelRegistry.IRORI_FIREWOOD.get(firewoodModel);
        if (model == null) {
            clearFirewoodModel(state);
            return;
        }

        applyCachedFirewoodModel(blockEntity, state, firewoodModel, model);

        int burnTime = blockEntity.getBurnTime();
        state.burnTime = burnTime;
        if (burnTime > 0 && blockEntity.getLevel() != null) {
            state.burningSprite = ensureBurningSprite();
            long gameTime = blockEntity.getLevel().getGameTime();
            float breathPhase = (Math.floorMod(gameTime, (long) BREATHING_CYCLE_TICKS) + partialTicks)
                    / BREATHING_CYCLE_TICKS;
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
        if (!state.grillModelParts.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(state.grillOffsetX, 10.0 / 16.0D, state.grillOffsetZ);
            if (state.grillRotated) {
                poseStack.translate(0.5D, 0.0D, 0.5D);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.translate(-0.5D, 0.0D, -0.5D);
            }
            submitNodeCollector.submitMultiLayerBlockModel(
                    poseStack,
                    state.grillModelParts,
                    state.grillHasTranslucency,
                    BlockModelRenderState.EMPTY_TINTS,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );
            poseStack.popPose();
        }

        for (CookingItemState cookingItem : state.cookingItems) {
            if (cookingItem.itemState().isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(
                    cookingItem.offsetX() + 0.5D,
                    COOKING_ITEM_Y_OFFSET,
                    cookingItem.offsetZ() + 0.5D
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(cookingItem.rotationY()));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(COOKING_ITEM_SCALE, COOKING_ITEM_SCALE, COOKING_ITEM_SCALE);
            cookingItem.itemState().submit(
                    poseStack,
                    submitNodeCollector,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );
            poseStack.popPose();
        }

        if (state.burnTime > 0 && state.burningSprite != null && state.burningBreathLight > 0) {
            poseStack.pushPose();
            poseStack.translate(
                    state.firewoodOffsetX,
                    BURNING_OVERLAY_Y_OFFSET,
                    state.firewoodOffsetZ
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
                state.firewoodOffsetX,
                FIREWOOD_Y_OFFSET + (1.0F - state.firewoodAppearProgress) * FIREWOOD_APPEAR_FALL_DISTANCE,
                state.firewoodOffsetZ
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
        state.grillModelParts = List.of();
        state.grillHasTranslucency = false;
        state.grillOffsetX = 0.0D;
        state.grillOffsetZ = 0.0D;
        state.grillRotated = false;
        state.firewoodOffsetX = 0.0D;
        state.firewoodOffsetZ = 0.0D;
        state.firewoodAppearProgress = 1.0F;
        state.firewoodAlpha = FULL_ALPHA;
        state.burnTime = 0;
        state.burningBreathLight = 0;
        state.burningSprite = null;
        state.cookingItems = List.of();
    }

    private void updateCookingItemRenderState(IroriBlockEntity blockEntity, State state) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        List<CookingItemState> cookingItems = new ArrayList<>();
        for (IroriBlockEntity.CookingRenderItem item : blockEntity.getCookingRenderItems()) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            int seed = (int) (item.seed() ^ item.seed() >>> 32);
            itemModelResolver.updateForTopItem(
                    itemState,
                    item.stack(),
                    ItemDisplayContext.FIXED,
                    blockEntity.getLevel(),
                    null,
                    seed
            );
            RandomSource random = RandomSource.create(item.seed() ^ COOKING_ITEM_TRANSFORM_SALT);
            double jitterX = random.triangle(0.0D, COOKING_ITEM_MAX_HORIZONTAL_JITTER);
            double jitterZ = random.triangle(0.0D, COOKING_ITEM_MAX_HORIZONTAL_JITTER);
            cookingItems.add(new CookingItemState(
                    itemState,
                    item.offsetX() + jitterX,
                    item.offsetZ() + jitterZ,
                    random.nextFloat() * 360.0F
            ));
        }
        state.cookingItems = List.copyOf(cookingItems);
    }

    private void updateGrillRenderState(IroriBlockEntity blockEntity, State state) {
        IroriBlockEntity.GrillRenderInfo grillInfo = blockEntity.getGrillRenderInfo();
        if (grillInfo == null || blockEntity.getLevel() == null) {
            return;
        }

        BlockStateModel model = BlockModelRegistry.IRORI_GRILL.get(grillInfo.model());
        if (model == null) {
            return;
        }

        BlockAndTintGetter tintGetter = (BlockAndTintGetter) blockEntity.getLevel();
        CachedGrillModel cached = grillModelCache.get(grillInfo.model());
        if (cached == null || cached.model() != model) {
            List<BlockStateModelPart> parts = new ArrayList<>();
            firewoodRandom.setSeed(FIREWOOD_RENDER_SEED);
            model.collectParts(
                    tintGetter,
                    blockEntity.getBlockPos(),
                    blockEntity.getBlockState(),
                    firewoodRandom,
                    parts
            );
            cached = new CachedGrillModel(
                    model,
                    List.copyOf(parts),
                    model.hasMaterialFlag(
                            tintGetter,
                            blockEntity.getBlockPos(),
                            blockEntity.getBlockState(),
                            BakedQuad.FLAG_TRANSLUCENT
                    )
            );
            grillModelCache.put(grillInfo.model(), cached);
        }

        state.grillModelParts = cached.parts();
        state.grillHasTranslucency = cached.hasTranslucency();
        state.grillOffsetX = grillInfo.offsetX();
        state.grillOffsetZ = grillInfo.offsetZ();
        state.grillRotated = grillInfo.rotated();
    }

    @Override
    public AABB getRenderBoundingBox(IroriBlockEntity blockEntity) {
        if (blockEntity.getLevel() == null || blockEntity.getMaster() != blockEntity) {
            return new AABB(blockEntity.getBlockPos());
        }

        IroriComponentTopology.Bounds component = IroriComponentTopology.bounds(
                blockEntity.getLevel(),
                blockEntity.getBlockPos()
        );
        return new AABB(
                component.minX(),
                blockEntity.getBlockPos().getY(),
                component.minZ(),
                component.maxX() + 1.0D,
                blockEntity.getBlockPos().getY() + 2.0D,
                component.maxZ() + 1.0D
        );
    }

    private void updateFirewoodTransform(IroriBlockEntity blockEntity, State state) {
        long blockPosSeed = blockEntity.getBlockPos().asLong();
        transformRandom.setSeed(blockPosSeed ^ FIREWOOD_RENDER_SEED);
        IroriBlockEntity.GrillLayoutInfo grillLayout = blockEntity.getGrillLayoutInfo();
        if (grillLayout == null) {
            state.firewoodRotationY = 0.0F;
        } else if (grillLayout.model() == IroriBlockEntity.GrillModel.ONE_BY_ONE) {
            state.firewoodRotationY = 45.0F + transformRandom.nextInt(4) * 90.0F;
        } else {
            state.firewoodRotationY = transformRandom.nextInt(16) * 22.5F;
            if (grillLayout.rotated()) {
                state.firewoodRotationY += 90.0F;
            }
        }
    }

    private void applyCachedFirewoodModel(IroriBlockEntity blockEntity, State state,
                                          IroriFuelState.FirewoodModel firewoodModel, BlockStateModel model) {
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
        public List<BlockStateModelPart> grillModelParts = List.of();
        public boolean grillHasTranslucency;
        public double grillOffsetX;
        public double grillOffsetZ;
        public boolean grillRotated;
        public List<BlockStateModelPart> firewoodModelParts = List.of();
        public boolean firewoodHasTranslucency;
        public double firewoodOffsetX;
        public double firewoodOffsetZ;
        public float firewoodAppearProgress = 1.0F;
        public float firewoodRotationY;
        public int firewoodAlpha = FULL_ALPHA;
        public int burnTime;
        public int burningBreathLight;
        public @Nullable TextureAtlasSprite burningSprite;
        public List<CookingItemState> cookingItems = List.of();
    }

    public record CookingItemState(
            ItemStackRenderState itemState,
            double offsetX,
            double offsetZ,
            float rotationY
    ) {
    }

    private record CachedFirewoodModel(
            BlockStateModel model,
            List<BlockStateModelPart> parts,
            boolean hasTranslucency
    ) {}

    private record CachedGrillModel(
            BlockStateModel model,
            List<BlockStateModelPart> parts,
            boolean hasTranslucency
    ) {}
}
