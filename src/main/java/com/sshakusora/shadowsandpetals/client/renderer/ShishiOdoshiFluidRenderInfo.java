package com.sshakusora.shadowsandpetals.client.renderer;

import com.sshakusora.shadowsandpetals.api.shishiOdoshi.ShishiOdoshiFluidRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Map;
import java.util.WeakHashMap;

final class ShishiOdoshiFluidRenderInfo {
    private static final int STREAM_ALPHA = 208;

    private ShishiOdoshiFluidRenderInfo() {}

    static Info create(Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
        var properties = ShishiOdoshiFluidRegistry.getRegisteredRenderProperties(fluid);
        TextureAtlasSprite sprite;
        int tint;
        if (properties != null) {
            Identifier texture = properties.flowingTexture();
            TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance()
                    .getTextureManager()
                    .getTexture(TextureAtlas.LOCATION_BLOCKS);
            sprite = atlas.getSprite(texture);
            tint = fluid == Fluids.WATER
                    ? BiomeColors.getAverageWaterColor(level, pos)
                    : properties.tintColor();
        } else {
            FluidModel model = Minecraft.getInstance()
                    .getModelManager()
                    .getFluidStateModelSet()
                    .get(fluid.defaultFluidState());
            sprite = model.flowingMaterial().sprite();
            var tintSource = model.fluidTintSource();
            tint = tintSource == null
                    ? 0xFFFFFF
                    : tintSource.colorInWorld(
                            fluid.defaultFluidState(), level.getBlockState(pos), level, pos
                    );
        }
        return new Info(sprite, ARGB.color(STREAM_ALPHA, tint & 0x00FFFFFF));
    }

    record Info(TextureAtlasSprite sprite, int color) {}

    static final class Cache<K> {
        private final Map<K, CachedInfo> entries = new WeakHashMap<>();

        Info get(K owner, Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
            long packedPos = pos.asLong();
            CachedInfo cached = entries.get(owner);
            if (cached != null && cached.fluid() == fluid && cached.packedPos() == packedPos) {
                return cached.info();
            }

            Info info = create(fluid, level, pos);
            if (info == null) {
                entries.remove(owner);
                return null;
            }
            entries.put(owner, new CachedInfo(fluid, packedPos, info));
            return info;
        }
    }

    private record CachedInfo(Fluid fluid, long packedPos, Info info) {}
}
