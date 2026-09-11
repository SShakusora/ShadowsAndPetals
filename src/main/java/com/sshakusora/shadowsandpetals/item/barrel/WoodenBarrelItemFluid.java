package com.sshakusora.shadowsandpetals.item.barrel;

import com.mojang.serialization.DataResult;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import com.sshakusora.shadowsandpetals.compat.transfer.StacksResourceHandler;
import com.sshakusora.shadowsandpetals.compat.transfer.fluid.FluidResource;
import com.sshakusora.shadowsandpetals.compat.transfer.item.ItemResource;

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

        if (fluid.getAmount() <= 0) {
            throw new IllegalArgumentException("Cannot write an empty fluid stack");
        }

        var encodedFluid = FluidStack.CODEC
                .encodeStart(NbtOps.INSTANCE, fluid)
                .getOrThrow();

        CompoundTag barrelData = new CompoundTag();
        barrelData.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityRegistry.WOODEN_BARREL.get()).toString());
        barrelData.put("Fluid", encodedFluid);
        return DataComponentPatch.builder()
                .set(
                        DataComponents.BLOCK_ENTITY_DATA,
                        CustomData.of(barrelData)
                )
                .build();
    }

    public static Optional<FluidStack> read(ItemStack stack) {
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null) {
            return Optional.empty();
        }

        CompoundTag barrelData = blockEntityData.copyTag();
        if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityRegistry.WOODEN_BARREL.get()).toString().equals(barrelData.getString("id"))) {
            return Optional.empty();
        }
        var serializedFluid = barrelData.get("Fluid");
        if (serializedFluid == null) return Optional.empty();
        return FluidStack.CODEC.parse(NbtOps.INSTANCE, serializedFluid).result();
    }
}
