package com.sshakusora.shadowsandpetals.compat.transfer;

import com.sshakusora.shadowsandpetals.compat.transfer.fluid.FluidResource;
import com.sshakusora.shadowsandpetals.compat.transfer.item.ItemResource;
import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Adapters between the small transfer abstraction used by the port and the
 * native NeoForge 1.21.1 capability interfaces.
 */
public final class NativeCapabilityAdapters {
    private NativeCapabilityAdapters() {}

    public static IFluidHandler fluids(ResourceHandler<FluidResource> delegate) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return delegate.size();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                if (tank < 0 || tank >= delegate.size()) {
                    return FluidStack.EMPTY;
                }
                FluidResource resource = delegate.getResource(tank);
                int amount = delegate.getAmountAsInt(tank);
                return resource.isEmpty() || amount <= 0 ? FluidStack.EMPTY : resource.toStack(amount);
            }

            @Override
            public int getTankCapacity(int tank) {
                if (tank < 0 || tank >= delegate.size()) {
                    return 0;
                }
                return delegate.getCapacityAsInt(tank, delegate.getResource(tank));
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank >= 0 && tank < delegate.size()
                        && !stack.isEmpty()
                        && delegate.isValid(tank, FluidResource.of(stack));
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource == null || resource.isEmpty()) {
                    return 0;
                }
                FluidResource requested = FluidResource.of(resource);
                int remaining = resource.getAmount();
                int total = 0;
                for (int tank = 0; tank < delegate.size() && remaining > 0; tank++) {
                    if (!delegate.isValid(tank, requested)) {
                        continue;
                    }
                    FluidResource current = delegate.getResource(tank);
                    if (!current.isEmpty() && !current.equals(requested)) {
                        continue;
                    }
                    int free = Math.max(0, delegate.getCapacityAsInt(tank, requested)
                            - delegate.getAmountAsInt(tank));
                    int amount = Math.min(remaining, free);
                    if (amount > 0) {
                        int moved = action.execute()
                                ? delegate.insert(tank, requested, amount, (TransactionContext) null)
                                : amount;
                        total += moved;
                        remaining -= moved;
                    }
                }
                return total;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource == null || resource.isEmpty()) {
                    return FluidStack.EMPTY;
                }
                FluidResource requested = FluidResource.of(resource);
                for (int tank = 0; tank < delegate.size(); tank++) {
                    FluidResource current = delegate.getResource(tank);
                    if (current.isEmpty() || !current.equals(requested)) {
                        continue;
                    }
                    int amount = Math.min(resource.getAmount(), delegate.getAmountAsInt(tank));
                    if (amount > 0) {
                        if (action.execute()) {
                            delegate.extract(tank, requested, amount, (TransactionContext) null);
                        }
                        return requested.toStack(amount);
                    }
                }
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (maxDrain <= 0) {
                    return FluidStack.EMPTY;
                }
                for (int tank = 0; tank < delegate.size(); tank++) {
                    FluidResource current = delegate.getResource(tank);
                    if (current.isEmpty()) {
                        continue;
                    }
                    int amount = Math.min(maxDrain, delegate.getAmountAsInt(tank));
                    if (amount > 0) {
                        if (action.execute()) {
                            delegate.extract(tank, current, amount, (TransactionContext) null);
                        }
                        return current.toStack(amount);
                    }
                }
                return FluidStack.EMPTY;
            }
        };
    }

    public static IFluidHandlerItem fluidItem(ResourceHandler<FluidResource> delegate, ItemStack container) {
        IFluidHandler handler = fluids(delegate);
        return new IFluidHandlerItem() {
            @Override
            public ItemStack getContainer() {
                return container;
            }

            @Override
            public int getTanks() {
                return handler.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return handler.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return handler.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return handler.isFluidValid(tank, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return handler.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return handler.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return handler.drain(maxDrain, action);
            }
        };
    }

    public static IItemHandler items(ResourceHandler<ItemResource> delegate) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return delegate.size();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                if (slot < 0 || slot >= delegate.size()) {
                    return ItemStack.EMPTY;
                }
                return delegate.getResource(slot).toStack(delegate.getAmountAsInt(slot));
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack == null || stack.isEmpty() || slot < 0 || slot >= delegate.size()) {
                    return stack == null ? ItemStack.EMPTY : stack.copy();
                }
                ItemResource resource = ItemResource.of(stack);
                if (!delegate.isValid(slot, resource)) {
                    return stack.copy();
                }
                ItemResource current = delegate.getResource(slot);
                if (!current.isEmpty() && !current.equals(resource)) {
                    return stack.copy();
                }
                int free = Math.max(0, delegate.getCapacityAsInt(slot, resource)
                        - delegate.getAmountAsInt(slot));
                int inserted = Math.min(stack.getCount(), free);
                if (!simulate && inserted > 0) {
                    delegate.insert(slot, resource, inserted, (TransactionContext) null);
                }
                return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot < 0 || slot >= delegate.size() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                ItemResource resource = delegate.getResource(slot);
                int extracted = Math.min(amount, delegate.getAmountAsInt(slot));
                if (resource.isEmpty() || extracted <= 0) {
                    return ItemStack.EMPTY;
                }
                if (!simulate) {
                    delegate.extract(slot, resource, extracted, (TransactionContext) null);
                }
                return resource.toStack(extracted);
            }

            @Override
            public int getSlotLimit(int slot) {
                if (slot < 0 || slot >= delegate.size()) {
                    return 0;
                }
                return delegate.getCapacityAsInt(slot, delegate.getResource(slot));
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot >= 0 && slot < delegate.size() && stack != null && !stack.isEmpty()
                        && delegate.isValid(slot, ItemResource.of(stack));
            }
        };
    }
}
