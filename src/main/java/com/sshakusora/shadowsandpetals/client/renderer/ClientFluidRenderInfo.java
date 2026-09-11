package com.sshakusora.shadowsandpetals.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/** Resolves fluid sprites and colors using the 1.21.1 client fluid extension API. */
public final class ClientFluidRenderInfo {
    private ClientFluidRenderInfo() {
    }

    public static Info createSurface(Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
        return create(fluid, level, pos, true);
    }

    public static Info createItemSurface(
            FluidStack stack,
            @Nullable BlockAndTintGetter level,
            @Nullable BlockPos pos
    ) {
        Fluid fluid = stack.getFluid();
        IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = extension.getStillTexture(stack);
        if (texture == null) {
            texture = fallbackTexture(fluid, false);
        }
        int tint = extension.getTintColor(stack);
        if (fluid == Fluids.WATER && level != null && pos != null) {
            tint = BiomeColors.getAverageWaterColor(level, pos);
        }
        return new Info(sprite(texture), 0xFF000000 | tint & 0x00FFFFFF, fluidLight(fluid));
    }

    private static Info create(Fluid fluid, BlockAndTintGetter level, BlockPos pos, boolean surface) {
        IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = surface
                ? extension.getStillTexture(fluid.defaultFluidState(), level, pos)
                : extension.getFlowingTexture(fluid.defaultFluidState(), level, pos);
        if (texture == null) {
            texture = fallbackTexture(fluid, !surface);
        }
        int tint = extension.getTintColor(fluid.defaultFluidState(), level, pos);
        if (fluid == Fluids.WATER) {
            tint = BiomeColors.getAverageWaterColor(level, pos);
        }
        return new Info(sprite(texture), 0xFF000000 | tint & 0x00FFFFFF, fluidLight(fluid));
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return ((TextureAtlas) Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS)).getSprite(texture);
    }

    private static ResourceLocation fallbackTexture(Fluid fluid, boolean flowing) {
        if (fluid == Fluids.LAVA) {
            return ResourceLocation.withDefaultNamespace(flowing ? "block/lava_flow" : "block/lava_still");
        }
        return ResourceLocation.withDefaultNamespace(flowing ? "block/water_flow" : "block/water_still");
    }

    private static int fluidLight(Fluid fluid) {
        return fluid.defaultFluidState().createLegacyBlock().getLightEmission();
    }

    public static int applyLightEmission(int packedLight, int lightEmission) {
        if (lightEmission <= 0) {
            return packedLight;
        }
        int blockLight = packedLight & 0xFFFF;
        int emission = Math.min(15, lightEmission) << 4;
        return (packedLight & 0xFFFF0000) | Math.max(blockLight, emission);
    }

    public record Info(@Nullable TextureAtlasSprite sprite, int color, int lightEmission) {
    }

    public static final class Cache<T> {
        private final Map<T, CachedInfo> entries = new WeakHashMap<>();

        public Info getSurface(T owner, Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
            return get(owner, fluid, level, pos);
        }

        public Info get(T owner, Fluid fluid, BlockAndTintGetter level, BlockPos pos) {
            long packedPos = pos.asLong();
            CachedInfo cached = entries.get(owner);
            if (cached != null && cached.fluid == fluid && cached.pos == packedPos) {
                return cached.info;
            }
            Info info = create(fluid, level, pos, false);
            entries.put(owner, new CachedInfo(fluid, packedPos, info));
            return info;
        }

        public void invalidate() {
            entries.clear();
        }

        private record CachedInfo(Fluid fluid, long pos, Info info) {
        }
    }
}