package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.shadowsandpetals.block.decoration.BoxBlock;
import com.sshakusora.shadowsandpetals.client.model.box.BoxContentGeometry;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Draws the shared shell and the stacked voxelized content copies used by the inventory item
 * renderer.
 *
 * <p>Placed boxes no longer use the content path here: their content is baked into the chunk
 * mesh by {@code BoxBakedModel}. The layer pose itself lives in
 * {@link BoxContentGeometry}, which both paths share, so the item preview and the placed box
 * cannot drift apart.</p>
 */
public final class BoxContentRenderer {
    private BoxContentRenderer() {}

    public static void renderShell(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderShell(poseStack, buffer, packedLight, packedOverlay, SlabType.BOTTOM);
    }

    public static void renderShell(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            SlabType type
    ) {
        BlockState state = BlockRegistry.BOX.get().defaultBlockState()
                .setValue(BoxBlock.TYPE, type);
        // Let NeoForge pick the entity render type (cutout shader with normal-based diffuse
        // lighting) so the shell shades identically to the plain empty-box item in GUIs.
        // The block model is the dynamic wrapper, but ModelData.EMPTY carries no content, so
        // this draws the shell only.
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }

    public static void renderContents(ItemStack content, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderContents(content, poseStack, buffer, packedLight, packedOverlay, 0.0F);
    }

    public static void renderContents(
            ItemStack content,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float yOffset
    ) {
        if (content.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(content, null, null, 0);
        float depthScale = BoxContentGeometry.depthScale(model.isGui3d());

        for (int layer = 0; layer < BoxContentGeometry.LAYER_COUNT; layer++) {
            poseStack.pushPose();
            BoxContentGeometry.applyLayer(poseStack, layer, yOffset, depthScale);
            itemRenderer.render(content, ItemDisplayContext.NONE, false, poseStack, buffer, packedLight, packedOverlay, model);
            poseStack.popPose();
        }
    }
}
