package com.sshakusora.shadowsandpetals.registries.event;

import com.sshakusora.shadowsandpetals.api.shishiOdoshi.RegisterShishiOdoshiFluidsEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/** Registers the built-in fluid behavior exposed through the public shishi-odoshi API. */
public final class ShishiOdoshiFluidBehaviorRegistry {
    private ShishiOdoshiFluidBehaviorRegistry() {
    }

    public static void register(RegisterShishiOdoshiFluidsEvent event) {
        event.registerSource(
                state -> state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED),
                () -> Fluids.WATER
        );
        event.registerRenderProperties(
                Fluids.WATER,
                Identifier.withDefaultNamespace("block/water_flow"),
                0xFFFFFF
        );

        event.registerSource(Blocks.MAGMA_BLOCK, Fluids.LAVA);
        event.registerFluid(
                Fluids.LAVA,
                0.25F,
                Identifier.withDefaultNamespace("block/lava_flow"),
                0xFFFFFF
        );
    }
}
