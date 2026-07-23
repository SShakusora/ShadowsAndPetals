package com.sshakusora.shadowsandpetals.api.shishiOdoshi;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Mod-bus event used to register fluids supported by the shishi-odoshi.
 *
 * <p>The event is dispatched after common setup listeners have completed. Subscribe on the mod
 * event bus and register common-side behavior only. Source rules registered later take precedence
 * over earlier, broader rules.
 */
public final class RegisterShishiOdoshiFluidsEvent extends Event implements IModBusEvent {
    public RegisterShishiOdoshiFluidsEvent() {
    }

    public void registerSource(Block block, Fluid fluid) {
        ShishiOdoshiFluidRegistry.registerSource(block, fluid);
    }

    public void registerSource(Supplier<? extends Block> block, Supplier<? extends Fluid> fluid) {
        ShishiOdoshiFluidRegistry.registerSource(block, fluid);
    }

    public void registerSource(Predicate<BlockState> sourcePredicate, Fluid fluid) {
        ShishiOdoshiFluidRegistry.registerSource(sourcePredicate, fluid);
    }

    public void registerSource(
            Predicate<BlockState> sourcePredicate,
            Supplier<? extends Fluid> fluid
    ) {
        ShishiOdoshiFluidRegistry.registerSource(sourcePredicate, fluid);
    }

    /**
     * Registers the animation-speed multiplier used by the shishi-odoshi motion,
     * pouring strip and pipe fluid-texture scrolling.
     */
    public void registerAnimationSpeed(Fluid fluid, float speed) {
        ShishiOdoshiFluidRegistry.registerAnimationSpeed(fluid, speed);
    }

    public void registerRenderProperties(Fluid fluid, Identifier flowingTexture, int tintColor) {
        ShishiOdoshiFluidRegistry.registerRenderProperties(fluid, flowingTexture, tintColor);
    }

    public void registerFluid(Fluid fluid, float animationSpeed, Identifier flowingTexture, int tintColor) {
        ShishiOdoshiFluidRegistry.registerFluid(fluid, animationSpeed, flowingTexture, tintColor);
    }
}
