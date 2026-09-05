package com.sshakusora.shadowsandpetals.item.barrel;

import com.mojang.serialization.DataResult;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Optional;

/**
 * Reads the fluid stored in a dropped wooden barrel item.
 */
public final class WoodenBarrelItemFluid {
    private WoodenBarrelItemFluid() {
    }

    /**
     * Writes a fluid stack to a wooden barrel item using the same serialized format as the barrel block entity.
     */
    public static ItemStack write(ItemStack stack, FluidStack fluid) {
        stack.applyComponents(fluidComponents(fluid));
        return stack;
    }

    /**
     * Returns the fluid stored in an item resource. Item capabilities receive an
     * {@link ItemResource} rather than an {@link ItemStack}, so this overload keeps
     * all barrel-content decoding in one place.
     */
    public static Optional<FluidStack> read(ItemResource resource) {
        return read(resource.toStack());
    }

    /**
     * Returns an item resource with the supplied fluid stored in its barrel data.
     */
    public static ItemResource withFluid(ItemResource resource, FluidStack fluid) {
        return resource.withMergedPatch(fluidComponents(fluid));
    }

    /**
     * Returns an item resource with the supplied NeoForge fluid resource and
     * amount stored in its barrel data.
     */
    public static ItemResource withFluid(ItemResource resource, FluidResource fluid, int amount) {
        return withFluid(resource, fluid.toStack(amount));
    }

    /**
     * Returns an item resource representing an empty wooden barrel.
     */
    public static ItemResource withoutFluid(ItemResource resource) {
        return resource.without(DataComponents.BLOCK_ENTITY_DATA);
    }

    /**
     * Builds the item component patch used to represent a filled wooden barrel.
     */
    public static DataComponentPatch fluidComponents(FluidStack fluid) {
        if (fluid.isEmpty()) {
            throw new IllegalArgumentException("Cannot write an empty fluid stack");
        }

        return fluidComponents(FluidStackTemplate.fromNonEmptyStack(fluid));
    }

    /**
     * Builds the item component patch from a datagen-safe fluid stack template.
     */
    public static DataComponentPatch fluidComponents(FluidStackTemplate fluid) {
        if (fluid.amount() <= 0) {
            throw new IllegalArgumentException("Cannot write an empty fluid stack");
        }

        var encodedStacks = FluidStackTemplate.CODEC
                .listOf()
                .encodeStart(NbtOps.INSTANCE, List.of(fluid))
                .getOrThrow();

        CompoundTag barrelData = new CompoundTag();
        barrelData.put(StacksResourceHandler.VALUE_IO_KEY, encodedStacks);
        return DataComponentPatch.builder()
                .set(
                        DataComponents.BLOCK_ENTITY_DATA,
                        TypedEntityData.of(BlockEntityRegistry.WOODEN_BARREL.get(), barrelData)
                )
                .build();
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
