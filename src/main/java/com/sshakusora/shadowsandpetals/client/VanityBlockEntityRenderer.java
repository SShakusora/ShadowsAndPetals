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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.NotNull;

public class VanityBlockEntityRenderer implements BlockEntityRenderer<VanityBlockEntity> {
    private static final float DRAWER_TRAVEL = 7.0F / 16.0F;

    public VanityBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(VanityBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (state.getValue(VanityBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        float progress = easeOutCubic(blockEntity.getDrawerProgress(partialTick));
        float travelScale = blockEntity.getDrawerTravelScale();

        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel drawerModel = modelManager.getModel(ModelResourceLocation.standalone(drawerModelId(state)));
        if (drawerModel == modelManager.getMissingModel()) {
            drawerModel = modelManager.getModel(ModelResourceLocation.standalone(ShadowsAndPetals.asResource("block/vanity/oak_drawer")));
        }

        Direction facing = state.getValue(VanityBlock.FACING);
        float translation = progress * DRAWER_TRAVEL * travelScale;

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

    private static ResourceLocation drawerModelId(BlockState state) {
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
}
