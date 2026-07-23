package com.sshakusora.shadowsandpetals.api.shishiOdoshi;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Public integration API for shishi-odoshi fluid sources, animation speeds and rendering.
 *
 * <p>Mods should normally register their fluid behavior from
 * {@link RegisterShishiOdoshiFluidsEvent}. Direct registration remains available for backwards
 * compatibility and initialization code that cannot subscribe to the mod event bus.
 */
public final class ShishiOdoshiFluidRegistry {
    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();
    private static final Map<Fluid, Float> ANIMATION_SPEEDS = new ConcurrentHashMap<>();
    private static final Map<Fluid, RenderProperties> RENDER_PROPERTIES = new ConcurrentHashMap<>();
    private static final RenderProperties DEFAULT_RENDER_PROPERTIES = new RenderProperties(
            Identifier.withDefaultNamespace("block/water_flow"),
            0xFFFFFF
    );

    private ShishiOdoshiFluidRegistry() {}

    public static void registerSource(Block block, Fluid fluid) {
        Objects.requireNonNull(block, "block");
        registerSource(state -> state.is(block), fluid);
    }

    /** Deferred-holder-friendly overload. */
    public static void registerSource(Supplier<? extends Block> block, Supplier<? extends Fluid> fluid) {
        Objects.requireNonNull(block, "block");
        registerSource(state -> state.is(block.get()), fluid);
    }

    public static void registerSource(Predicate<BlockState> sourcePredicate, Fluid fluid) {
        registerSource(sourcePredicate, () -> fluid);
    }

    /** Supplier overload allows safe references to deferred fluids. */
    public static void registerSource(Predicate<BlockState> sourcePredicate, Supplier<? extends Fluid> fluid) {
        SOURCES.add(new Source(
                Objects.requireNonNull(sourcePredicate, "sourcePredicate"),
                Objects.requireNonNull(fluid, "fluid")
        ));
    }

    /**
     * Sets the speed of the tipping animation and the pouring strip moving
     * through and out of the bamboo tube, as well as the supplied fluid texture
     * scrolling through a shishi-odoshi pipe. Filling, returning and bouncing
     * always retain their original speed.
     * A value of 2.0 is twice as fast; 0.5 is half speed.
     */
    public static void registerAnimationSpeed(Fluid fluid, float speed) {
        Objects.requireNonNull(fluid, "fluid");
        if (!Float.isFinite(speed) || speed <= 0.0F) {
            throw new IllegalArgumentException("Shishi-odoshi animation speed must be finite and greater than zero");
        }
        ANIMATION_SPEEDS.put(fluid, speed);
    }

    public static float getAnimationSpeed(Fluid fluid) {
        return ANIMATION_SPEEDS.getOrDefault(fluid, 1.0F);
    }

    /** Convenience registration for all fluid-specific behavior used by the system. */
    public static void registerFluid(Fluid fluid, float animationSpeed, Identifier flowingTexture, int tintColor) {
        registerAnimationSpeed(fluid, animationSpeed);
        registerRenderProperties(fluid, flowingTexture, tintColor);
    }

    /** tintColor uses the low 24 bits as RGB; stream transparency is applied by the renderer. */
    public static void registerRenderProperties(Fluid fluid, Identifier flowingTexture, int tintColor) {
        RENDER_PROPERTIES.put(
                Objects.requireNonNull(fluid, "fluid"),
                new RenderProperties(Objects.requireNonNull(flowingTexture, "flowingTexture"), tintColor & 0x00FFFFFF)
        );
    }

    public static RenderProperties getRenderProperties(Fluid fluid) {
        return RENDER_PROPERTIES.getOrDefault(
                fluid,
                RENDER_PROPERTIES.getOrDefault(Fluids.WATER, DEFAULT_RENDER_PROPERTIES)
        );
    }

    /** Returns an explicit rendering override, or {@code null} to use the fluid's baked model. */
    public static @Nullable RenderProperties getRegisteredRenderProperties(Fluid fluid) {
        return RENDER_PROPERTIES.get(fluid);
    }

    public static @Nullable Fluid findSourceFluid(LevelReader level, BlockPos sourcePos) {
        return findSourceFluid((BlockGetter) level, sourcePos);
    }

    public static @Nullable Fluid findSourceFluid(BlockGetter level, BlockPos sourcePos) {
        BlockState state = level.getBlockState(sourcePos);
        // Later registrations override broad predicates such as the waterlogged default.
        for (int i = SOURCES.size() - 1; i >= 0; i--) {
            Source source = SOURCES.get(i);
            if (source.predicate().test(state)) {
                Fluid fluid = source.fluid().get();
                return fluid == Fluids.EMPTY ? null : fluid;
            }
        }

        CauldronFluidContent cauldronContent = CauldronFluidContent.getForBlock(state.getBlock());
        if (cauldronContent == null || cauldronContent.currentLevel(state) == 0) {
            return null;
        }
        return cauldronContent.fluid == Fluids.EMPTY ? null : cauldronContent.fluid;
    }

    /** Number of explicitly registered source rules, primarily useful for diagnostics. */
    public static int registeredSourceCount() {
        return SOURCES.size();
    }

    /** Number of fluids with an explicitly configured animation speed. */
    public static int registeredAnimationSpeedCount() {
        return ANIMATION_SPEEDS.size();
    }

    /** Number of fluids with explicitly configured render properties. */
    public static int registeredRenderPropertiesCount() {
        return RENDER_PROPERTIES.size();
    }

    private record Source(Predicate<BlockState> predicate, Supplier<? extends Fluid> fluid) {}

    public record RenderProperties(Identifier flowingTexture, int tintColor) {}
}
