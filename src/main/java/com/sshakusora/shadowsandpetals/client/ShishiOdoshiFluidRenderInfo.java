package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.api.ShishiOdoshiFluidRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

final class ShishiOdoshiFluidRenderInfo {
    private static final int STREAM_ALPHA = 208;

    private ShishiOdoshiFluidRenderInfo() {}

    static @Nullable Info create(Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
        var properties = ShishiOdoshiFluidRegistry.getRenderProperties(fluid);
        Identifier texture = properties.flowingTexture();

        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite sprite = atlas.getSprite(texture);
        int tint = fluid == Fluids.WATER
                ? BiomeColors.getAverageWaterColor(level, pos)
                : properties.tintColor();
        return new Info(sprite, ARGB.color(STREAM_ALPHA, tint & 0x00FFFFFF));
    }

    record Info(TextureAtlasSprite sprite, int color) {}
}
