package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import com.sshakusora.shadowsandpetals.blockentity.VanityBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

public class VanityBlockEntityRenderer implements BlockEntityRenderer<VanityBlockEntity> {
    private static final float DRAWER_TRAVEL = 7.0F / 16.0F;
    private final Map<VanityBlockEntity, DrawerAnimationState> animationStates = new WeakHashMap<>();

    public VanityBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(VanityBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (state.getValue(VanityBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        float progress = Mth.clamp(blockEntity.getDrawerProgress(partialTick), 0.0F, 1.0F);
        DrawerAnimationState animationState = animationStates.computeIfAbsent(blockEntity, ignored -> new DrawerAnimationState());
        if (!animationState.initialized) {
            animationState.initialized = true;
            animationState.travelScale = randomTravelScale(blockEntity.getBlockPos().asLong(), animationState.openCycle);
        } else if (animationState.previousProgress <= 0.001F && progress > animationState.previousProgress) {
            animationState.openCycle++;
            animationState.travelScale = randomTravelScale(blockEntity.getBlockPos().asLong(), animationState.openCycle);
        }
        animationState.previousProgress = progress;

        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel drawerModel = modelManager.getModel(ModelResourceLocation.standalone(drawerModelId(state)));
        if (drawerModel == modelManager.getMissingModel()) {
            drawerModel = modelManager.getModel(ModelResourceLocation.standalone(ShadowsAndPetals.asResource("block/vanity/oak_drawer")));
        }

        Direction facing = state.getValue(VanityBlock.FACING);
        float translation = easeOutCubic(progress) * DRAWER_TRAVEL * animationState.travelScale;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() - 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        poseStack.translate(0.0D, 0.0D, -translation);

        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.cutout()),
                state,
                drawerModel,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static net.minecraft.resources.ResourceLocation drawerModelId(BlockState state) {
        return ShadowsAndPetals.asResource("block/vanity/" + woodTypeFor(state).getName() + "_drawer");
    }

    private static WoodBlockList.WoodType woodTypeFor(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
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

    private static class DrawerAnimationState {
        private float previousProgress;
        private boolean initialized;
        private int openCycle;
        private float travelScale = 1.0F;
    }
}
