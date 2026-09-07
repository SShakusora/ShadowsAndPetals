package com.sshakusora.shadowsandpetals.item.barrel;

import com.sshakusora.shadowsandpetals.blockentity.WoodenBarrelBlockEntity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * NeoForge fluid capability for a wooden barrel item.
 *
 * <p>The item uses the same typed block-entity component as a dropped barrel
 * block, which means transfer operations and block placement share one format.</p>
 */
public final class WoodenBarrelItemFluidHandler extends ItemAccessResourceHandler<FluidResource> {
    private final Item validItem;

    public WoodenBarrelItemFluidHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
        this.validItem = itemAccess.getResource().getItem();
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        if (index != 0 || !accessResource.is(validItem)) {
            return FluidResource.EMPTY;
        }

        return WoodenBarrelItemFluid.read(accessResource)
                .map(FluidResource::of)
                .orElse(FluidResource.EMPTY);
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        if (index != 0 || !accessResource.is(validItem)) {
            return 0;
        }

        return WoodenBarrelItemFluid.read(accessResource)
                .map(FluidStack::getAmount)
                .orElse(0);
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (index != 0 || !accessResource.is(validItem)) {
            return ItemResource.EMPTY;
        }

        if (newAmount == 0) {
            return WoodenBarrelItemFluid.withoutFluid(accessResource);
        }

        return WoodenBarrelItemFluid.withFluid(accessResource, newResource, newAmount);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (index != 0 || resource.isEmpty() || !itemAccess.getResource().is(validItem)) {
            return false;
        }

        FluidResource current = getResource(index);
        return current.isEmpty() || current.equals(resource);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return WoodenBarrelBlockEntity.FLUID_CAPACITY;
    }
}
