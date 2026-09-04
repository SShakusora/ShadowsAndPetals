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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Map;
import java.util.WeakHashMap;

final class ShishiOdoshiFluidRenderInfo {
    private static final int STREAM_ALPHA = 208;

    private ShishiOdoshiFluidRenderInfo() {}

    static Info create(Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
        return create(fluid, level, pos, false);
    }

    static Info createSurface(Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
        return create(fluid, level, pos, true);
    }

    private static Info create(Fluid fluid, BlockAndTintGetter level, BlockPos pos, boolean surface) {
        var properties = ShishiOdoshiFluidRegistry.getRegisteredRenderProperties(fluid);
        FluidModel model = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.defaultFluidState());
        TextureAtlasSprite sprite;
        int tint;
        if (surface) {
            sprite = model.stillMaterial().sprite();
        } else if (properties != null) {
            Identifier texture = properties.flowingTexture();
            TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance()
                    .getTextureManager()
                    .getTexture(TextureAtlas.LOCATION_BLOCKS);
            sprite = atlas.getSprite(texture);
        } else {
            sprite = model.flowingMaterial().sprite();
        }
        if (properties != null) {
            tint = fluid == Fluids.WATER
                    ? BiomeColors.getAverageWaterColor(level, pos)
                    : properties.tintColor();
        } else {
            var tintSource = model.fluidTintSource();
            tint = tintSource == null
                    ? 0xFFFFFF
                    : tintSource.colorInWorld(
                    fluid.defaultFluidState(), level.getBlockState(pos), level, pos
            );
        }
        int lightEmission = fluid.defaultFluidState().createLegacyBlock().getLightEmission(level, pos);
        int alpha = surface ? 255 : STREAM_ALPHA;
        return new Info(sprite, ARGB.color(alpha, tint & 0x00FFFFFF), lightEmission);
    }

    static int applyLightEmission(int packedLight, int lightEmission) {
        return LightCoordsUtil.lightCoordsWithEmission(packedLight, lightEmission);
    }

    record Info(TextureAtlasSprite sprite, int color, int lightEmission) {}

    static final class Cache<K> {
        private final Map<K, CachedInfo> entries = new WeakHashMap<>();

        Info get(K owner, Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
            return get(owner, fluid, level, pos, false);
        }

        Info getSurface(K owner, Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
            return get(owner, fluid, level, pos, true);
        }

        private Info get(K owner, Fluid fluid, BlockAndTintGetter level, BlockPos pos, boolean surface) {
            long packedPos = pos.asLong();
            CachedInfo cached = entries.get(owner);
            if (cached != null
                    && cached.fluid() == fluid
                    && cached.packedPos() == packedPos
                    && cached.surface() == surface) {
                return cached.info();
            }

            Info info = surface ? createSurface(fluid, level, pos) : create(fluid, level, pos);
            if (info == null) {
                entries.remove(owner);
                return null;
            }
            entries.put(owner, new CachedInfo(fluid, packedPos, surface, info));
            return info;
        }
    }

    private record CachedInfo(Fluid fluid, long packedPos, boolean surface, Info info) {}
}
