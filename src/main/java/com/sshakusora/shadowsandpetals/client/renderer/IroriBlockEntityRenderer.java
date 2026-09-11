package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;

import java.util.Random;

public class IroriBlockEntityRenderer implements BlockEntityRenderer<IroriBlockEntity> {
    private static final double FIREWOOD_Y = 10.0D / 16.0D;
    private static final double ITEM_Y = 21.2D / 16.0D;
    private final BlockRenderDispatcher blockRenderer;
    private final ItemRenderer itemRenderer;

    public IroriBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(IroriBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // The grill itself is a normal block model.  Only the moving/placed
        // contents need a block-entity pass on 1.21.1.
        LegacyBlockEntityRenderSupport.renderBlock(
                blockRenderer, blockEntity.getBlockState(), poseStack, buffer, packedLight, packedOverlay);

        if (blockEntity.shouldRenderFirewood() && blockEntity.getFirewoodModel() != null) {
            var offset = blockEntity.getFirewoodRenderOffset();
            poseStack.pushPose();
            poseStack.translate(0.5D + offset.x(), FIREWOOD_Y, 0.5D + offset.z());
            LegacyBlockEntityRenderSupport.renderStandalone(
                    blockRenderer,
                    ShadowsAndPetals.asResource("block/irori/firewood/" + blockEntity.getFirewoodModel().modelName()),
                    blockEntity.getBlockState(),
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
            poseStack.popPose();
        }

        for (IroriBlockEntity.CookingRenderItem item : blockEntity.getCookingRenderItems()) {
            if (item.stack().isEmpty()) {
                continue;
            }
            Random random = new Random(item.seed() ^ 0x49524F52494C4F4EL);
            double jitterX = (random.nextDouble() - random.nextDouble()) / 16.0D;
            double jitterZ = (random.nextDouble() - random.nextDouble()) / 16.0D;
            poseStack.pushPose();
            poseStack.translate(item.offsetX() + 0.5D + jitterX, ITEM_Y, item.offsetZ() + 0.5D + jitterZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            LegacyBlockEntityRenderSupport.renderItem(
                    itemRenderer, item.stack(), poseStack, buffer, blockEntity.getLevel(), packedLight, packedOverlay);
            poseStack.popPose();
        }
    }
}
