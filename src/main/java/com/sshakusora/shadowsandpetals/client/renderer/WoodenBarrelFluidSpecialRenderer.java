package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public final class WoodenBarrelFluidSpecialRenderer implements SpecialModelRenderer<WoodenBarrelFluidSpecialRenderer.RenderData> {
    public static final WoodenBarrelFluidSpecialRenderer INSTANCE = new WoodenBarrelFluidSpecialRenderer();

    private WoodenBarrelFluidSpecialRenderer() {
    }

    @Override
    public void submit(
            @Nullable RenderData data,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        if (data == null || data.amount() <= 0 || data.capacity() <= 0) {
            return;
        }

        float fillRatio = Mth.clamp(data.amount() / (float) data.capacity(), 0.0F, 1.0F);
        float surfaceY = Mth.lerp(
                fillRatio,
                WoodenBarrelFluidGeometry.MIN_SURFACE_Y,
                WoodenBarrelFluidGeometry.MAX_SURFACE_Y
        ) + WoodenBarrelFluidGeometry.ITEM_SURFACE_EPSILON;
        int light = ClientFluidRenderInfo.applyLightEmission(lightCoords, data.lightEmission());
        TextureAtlasSprite sprite = data.sprite();

        poseStack.pushPose();
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                renderType(sprite),
                (pose, buffer) -> WoodenBarrelFluidGeometry.renderSurface(
                        buffer,
                        pose,
                        sprite,
                        surfaceY,
                        data.color(),
                        light
                )
        );
        poseStack.popPose();
    }

    private static RenderType renderType(TextureAtlasSprite sprite) {
        boolean translucent = sprite.transparency().hasTranslucent();
        if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
            return translucent ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet();
        }
        return translucent ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        WoodenBarrelFluidGeometry.addExtents(output);
    }

    @Override
    public @Nullable RenderData extractArgument(ItemStack stack) {
        return null;
    }

    public record RenderData(
            TextureAtlasSprite sprite,
            int color,
            int amount,
            int capacity,
            int lightEmission
    ) {
    }
}
