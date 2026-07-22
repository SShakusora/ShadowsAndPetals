package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.registries.builder.RegFluidBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class FluidRegistry {
    private static final RegFluidBuilder.RegisteredFluid TEA_FLUID = SAPRegistries.fluid("tea")
            .typeProperties(properties -> properties
                    .density(1000)
                    .viscosity(1000))
            .properties(properties -> properties.bucket(() -> ItemRegistry.TEA_BUCKET.get()))
            .liquidBlock()
            .clientModel(
                    Identifier.withDefaultNamespace("block/water_still"),
                    Identifier.withDefaultNamespace("block/water_flow"),
                    0xFFC16D3D
            )
            .lang("Tea")
            .lang("zh_cn", "茶")
            .register();

    public static final DeferredHolder<FluidType, FluidType> TEA_TYPE = TEA_FLUID.type();
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> TEA = TEA_FLUID.source();
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_TEA = TEA_FLUID.flowing();
    public static final DeferredBlock<LiquidBlock> TEA_BLOCK = TEA_FLUID.block().orElseThrow();

    private FluidRegistry() {
    }

    public static void init() {
    }
}
