package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.sshakusora.shadowsandpetals.block.decoration.WoodenBarrelBlock;
import com.sshakusora.shadowsandpetals.blockentity.WoodenBarrelBlockEntity;
import com.sshakusora.shadowsandpetals.compat.transfer.fluid.FluidResource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class WoodenBarrelBlockEntityRenderer implements BlockEntityRenderer<WoodenBarrelBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;
    private final ClientFluidRenderInfo.Cache<WoodenBarrelBlockEntity> fluidCache = new ClientFluidRenderInfo.Cache<>();

    public WoodenBarrelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(WoodenBarrelBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        LegacyBlockEntityRenderSupport.renderBlock(
                blockRenderer, blockEntity.getBlockState(), poseStack, buffer, packedLight, packedOverlay);

        if (blockEntity.getLevel() == null) {
            return;
        }
        FluidResource resource = blockEntity.getFluidTank().getResource(0);
        int amount = blockEntity.getFluidTank().getAmountAsInt(0);
        if (resource.isEmpty() || amount <= 0 || resource.getFluid() == Fluids.EMPTY) {
            return;
        }
        ClientFluidRenderInfo.Info info = fluidCache.getSurface(
                blockEntity, resource.getFluid(), blockEntity.getLevel(), blockEntity.getBlockPos());
        TextureAtlasSprite sprite = info.sprite();
        if (sprite == null) {
            return;
        }
        float fill = Mth.clamp((float) amount / WoodenBarrelBlockEntity.FLUID_CAPACITY, 0.0F, 1.0F);
        float surface = Mth.lerp(fill,
                WoodenBarrelFluidGeometry.MIN_SURFACE_Y,
                WoodenBarrelFluidGeometry.MAX_SURFACE_Y);
        int light = ClientFluidRenderInfo.applyLightEmission(packedLight, info.lightEmission());
        poseStack.pushPose();
        WoodenBarrelFluidGeometry.renderSurface(
                buffer.getBuffer(RenderType.translucent()),
                poseStack.last(),
                sprite,
                surface,
                info.color(),
                light,
                blockEntity.getBlockState().getValue(WoodenBarrelBlock.AXIS)
        );
        poseStack.popPose();
    }
}
