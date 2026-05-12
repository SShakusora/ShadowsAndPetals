package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import com.sshakusora.shadowsandpetals.blockentity.VanityBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class VanityBlockEntityRenderer implements BlockEntityRenderer<VanityBlockEntity, VanityBlockEntityRenderer.State> {
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final RandomSource DRAWER_RANDOM = RandomSource.create(42L);
    private static final float DRAWER_TRAVEL = 7.0F / 16.0F;
    private static final Map<WoodBlockList.WoodType, StandaloneModelKey<BlockStateModel>> DRAWER_MODEL_KEYS = createDrawerModelKeys();

    public VanityBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VanityBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        float previousProgress = state.progress;
        float currentProgress = blockEntity.getDrawerProgress(partialTicks);

        state.facing = blockEntity.getBlockState().getValue(VanityBlock.FACING);
        state.progress = currentProgress;
        state.drawerModelParts.clear();

        if (!state.initialized) {
            state.initialized = true;
            state.drawerTravelScale = randomTravelScale(blockEntity.getBlockPos().asLong(), state.openCycle);
        } else if (previousProgress <= 0.001F && currentProgress > previousProgress) {
            state.openCycle++;
            state.drawerTravelScale = randomTravelScale(blockEntity.getBlockPos().asLong(), state.openCycle);
        }

        BlockStateModel drawerModel = Minecraft.getInstance()
                .getModelManager()
                .getStandaloneModel(DRAWER_MODEL_KEYS.get(woodTypeFor(blockEntity.getBlockState().getBlock())));
        if (drawerModel == null) {
            state.drawerHasTranslucency = false;
            return;
        }

        DRAWER_RANDOM.setSeed(42L);
        drawerModel.collectParts(DRAWER_RANDOM, state.drawerModelParts);
        state.drawerHasTranslucency = drawerModel.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() - 180));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        poseStack.pushPose();
        poseStack.translate(0, 0, -easeOutCubic(state.progress) * DRAWER_TRAVEL * state.drawerTravelScale);
        submitNodeCollector.submitMultiLayerBlockModel(
                poseStack,
                state.drawerModelParts,
                state.drawerHasTranslucency,
                BlockModelRenderState.EMPTY_TINTS,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();

        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public final List<BlockStateModelPart> drawerModelParts = new ArrayList<>();
        public Direction facing = Direction.NORTH;
        public float progress;
        public boolean drawerHasTranslucency;
        public boolean initialized;
        public int openCycle;
        public float drawerTravelScale = 1.0F;
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            event.register(
                    DRAWER_MODEL_KEYS.get(woodType),
                    SimpleUnbakedStandaloneModel.blockStateModel(ShadowsAndPetals.asResource("block/vanity/" + woodType.getName() + "_drawer"))
            );
        }
    }

    private static Map<WoodBlockList.WoodType, StandaloneModelKey<BlockStateModel>> createDrawerModelKeys() {
        Map<WoodBlockList.WoodType, StandaloneModelKey<BlockStateModel>> modelKeys = new EnumMap<>(WoodBlockList.WoodType.class);
        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            Identifier id = ShadowsAndPetals.asResource("vanity_drawer/" + woodType.getName());
            modelKeys.put(woodType, new StandaloneModelKey<>(() -> id.toString()));
        }
        return modelKeys;
    }

    private static WoodBlockList.WoodType woodTypeFor(Block vanityBlock) {
        String path = BuiltInRegistries.BLOCK.getKey(vanityBlock).getPath();
        String woodName = path.endsWith("_vanity") ? path.substring(0, path.length() - "_vanity".length()) : "oak";

        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            if (woodType.getName().equals(woodName)) {
                return woodType;
            }
        }

        return WoodBlockList.WoodType.OAK;
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1.0F - progress;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float randomTravelScale(long blockSeed, int openCycle) {
        long mixed = blockSeed * 31L + openCycle * 0x9E3779B97F4A7C15L;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        float normalized = (float) ((mixed >>> 40) & 0xFFFFFFL) / 0xFFFFFFL;
        return 0.94F + normalized * 0.12F;
    }
}
