package com.sshakusora.shadowsandpetals.client.renderer;

import com.sshakusora.shadowsandpetals.api.shishiOdoshi.ShishiOdoshiFluidRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolves the client-side sprite, tint and light emission for a fluid used by
 * the world fluid renderers.
 */
@SuppressWarnings("deprecation")
public final class ClientFluidRenderInfo {
    private static final int STREAM_ALPHA = 208;

    private ClientFluidRenderInfo() {}

    public static Info create(Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
        return create(fluid, level, pos, false);
    }

    public static Info createSurface(Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
        return create(fluid, level, pos, true);
    }

    public static Info createItemSurface(
            Fluid fluid,
            @Nullable ClientLevel level,
            @Nullable BlockPos pos
    ) {
        return createItemSurface(fluid, null, level, pos);
    }

    public static Info createItemSurface(
            FluidStack fluidStack,
            @Nullable ClientLevel level,
            @Nullable BlockPos pos
    ) {
        return createItemSurface(fluidStack.getFluid(), fluidStack, level, pos);
    }

    private static Info createItemSurface(
            Fluid fluid,
            @Nullable FluidStack fluidStack,
            @Nullable ClientLevel level,
            @Nullable BlockPos pos
    ) {
        var properties = ShishiOdoshiFluidRegistry.getRegisteredRenderProperties(fluid);
        FluidModel model = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.defaultFluidState());
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int tint;
        if (properties != null) {
            tint = fluid == Fluids.WATER && level != null && pos != null
                    ? BiomeColors.getAverageWaterColor(level, pos)
                    : properties.tintColor();
        } else {
            var tintSource = model.fluidTintSource();
            tint = tintSource == null
                    ? fluid == Fluids.WATER ? 0x3F76E4 : 0xFFFFFF
                    : level != null && pos != null
                    ? tintSource.colorInWorld(fluid.defaultFluidState(), level.getBlockState(pos), level, pos)
                    : fluidStack != null
                    ? tintSource.colorAsStack(fluidStack)
                    : tintSource.color(fluid.defaultFluidState());
        }
        int lightEmission = fluid.defaultFluidState().createLegacyBlock().getLightEmission();
        return new Info(sprite, ARGB.color(255, tint & 0x00FFFFFF), lightEmission);
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

    public static int applyLightEmission(int packedLight, int lightEmission) {
        return LightCoordsUtil.lightCoordsWithEmission(packedLight, lightEmission);
    }

    public record Info(TextureAtlasSprite sprite, int color, int lightEmission) {}

    public static final class Cache<K> {
        private final Map<K, CachedInfo> entries = new WeakHashMap<>();

        public Info get(K owner, Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
            return get(owner, fluid, level, pos, false);
        }

        public Info getSurface(K owner, Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
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
            entries.put(owner, new CachedInfo(fluid, packedPos, surface, info));
            return info;
        }
    }

    private record CachedInfo(Fluid fluid, long packedPos, boolean surface, Info info) {}
}
