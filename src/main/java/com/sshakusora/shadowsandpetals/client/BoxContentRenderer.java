package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the shared shell and the stacked voxelized content copies used by both the placed
 * block entity renderer and the inventory item renderer.
 */
public final class BoxContentRenderer {
    private static final float[] LAYER_ROTATIONS = {
            -4.941F, 174.814F, 7.488F, 175.921F, 7.488F, 172.883F, -2.474F, 175.697F
    };

    private static final float[][] LAYER_TRANSLATIONS = {
            {0.06878F, 0.0625F, 0.01436F},
            {-0.02615F, 0.1125F, -0.03102F},
            {0.0785F, 0.1625F, 0.03709F},
            {-0.02078F, 0.2125F, -0.02095F},
            {0.11643F, 0.2625F, 0.00902F},
            {-0.03524F, 0.3125F, -0.08755F},
            {0.04715F, 0.3625F, 0.11614F},
            {-0.06111F, 0.4125F, -0.01407F}
    };

    private BoxContentRenderer() {}

    public static void renderShell(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = BlockRegistry.RAW_PORKCHOP_BOX.get().defaultBlockState();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
    }

    public static void renderContents(ItemStack content, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (content.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(content, null, null, 0);

        // Generated item models are one pixel deep, so keep roughly 0.8 pixel of thickness.
        // Full 3D block/item models need normalising to the same thickness to stay inside the box.
        float depthScale = model.isGui3d() ? 0.05F : 0.8F;

        for (int i = 0; i < LAYER_ROTATIONS.length; i++) {
            float[] translation = LAYER_TRANSLATIONS[i];

            poseStack.pushPose();
            poseStack.translate(0.5D + translation[0], 0.5D + translation[1], 0.5D + translation[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees(LAYER_ROTATIONS[i]));
            poseStack.translate(0.025D, -0.475D, 0.025D);
            poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
            poseStack.scale(0.65F, 0.65F, depthScale);
            itemRenderer.render(content, ItemDisplayContext.NONE, false, poseStack, buffer, packedLight, packedOverlay, model);
            poseStack.popPose();
        }
    }
}
