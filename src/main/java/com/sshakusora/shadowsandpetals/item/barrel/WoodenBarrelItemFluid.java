package com.sshakusora.shadowsandpetals.item.barrel;

import com.mojang.serialization.DataResult;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.StacksResourceHandler;

import java.util.List;
import java.util.Optional;

/**
 * Reads the fluid stored in a dropped wooden barrel item.
 */
public final class WoodenBarrelItemFluid {
    private WoodenBarrelItemFluid() {
    }

    public static Optional<FluidStack> read(ItemStack stack) {
        TypedEntityData<?> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null || blockEntityData.type() != BlockEntityRegistry.WOODEN_BARREL.get()) {
            return Optional.empty();
        }

        var serializedStacks = blockEntityData.copyTagWithoutId().get(StacksResourceHandler.VALUE_IO_KEY);
        if (serializedStacks == null) {
            return Optional.empty();
        }

        DataResult<List<FluidStack>> decoded = FluidStack.OPTIONAL_CODEC
                .listOf()
                .parse(NbtOps.INSTANCE, serializedStacks);
        return decoded.result()
                .flatMap(stacks -> stacks.stream().filter(fluid -> !fluid.isEmpty()).findFirst());
    }
}
