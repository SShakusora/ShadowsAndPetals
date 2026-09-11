package com.sshakusora.shadowsandpetals.compat.transfer.fluid;

import com.sshakusora.shadowsandpetals.compat.transfer.ResourceHandler;
import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.fluids.FluidStack;

public abstract class FluidStacksResourceHandler implements ResourceHandler<FluidResource> {
    public static final String VALUE_IO_KEY = "Stacks";
    protected final FluidStack[] stacks;
    protected final int capacity;
    protected FluidStacksResourceHandler(int size, int capacity) {
        this.stacks = new FluidStack[size];
        java.util.Arrays.fill(this.stacks, FluidStack.EMPTY);
        this.capacity = capacity;
    }
    protected void onContentsChanged(int index, FluidStack previousContents) {}
    @Override public FluidResource getResource(int index) { check(index); return stacks[index].isEmpty() ? FluidResource.EMPTY : FluidResource.of(stacks[index]); }
    @Override public long getAmountAsLong(int index) { check(index); return stacks[index].isEmpty() ? 0 : stacks[index].getAmount(); }
    @Override public long getCapacityAsLong(int index, FluidResource resource) { check(index); return capacity; }
    @Override public int size() { return stacks.length; }
    @Override public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        check(index); if (amount <= 0 || !isValid(index, resource) || resource.isEmpty()) return 0;
        FluidStack current=stacks[index]; int free=capacity-(current.isEmpty()?0:current.getAmount());
        if (!current.isEmpty() && !FluidStack.isSameFluidSameComponents(current, resource.asStack())) return 0;
        int inserted=Math.min(free, amount);
        if (inserted>0) {
            FluidStack previous=current.copy();
            stacks[index]=current.isEmpty()?resource.toStack(inserted):current.copyWithAmount(current.getAmount()+inserted);
            onContentsChanged(index, previous);
            if (tx != null) {
                tx.addRollback(() -> {
                    FluidStack currentContents = stacks[index];
                    stacks[index] = previous.copy();
                    onContentsChanged(index, currentContents);
                });
            }
        }
        return inserted;
    }
    @Override public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        check(index); if (amount<=0 || resource.isEmpty() || stacks[index].isEmpty() || !FluidStack.isSameFluidSameComponents(stacks[index],resource.asStack())) return 0;
        int extracted=Math.min(amount, stacks[index].getAmount());
        FluidStack previous=stacks[index].copy();
        stacks[index]=extracted>=previous.getAmount()?FluidStack.EMPTY:previous.copyWithAmount(previous.getAmount()-extracted);
        onContentsChanged(index,previous);
        if (tx != null) {
            tx.addRollback(() -> {
                FluidStack currentContents = stacks[index];
                stacks[index] = previous.copy();
                onContentsChanged(index, currentContents);
            });
        }
        return extracted;
    }
    public void serialize(CompoundTag output, HolderLookup.Provider registries) {
        if (!stacks[0].isEmpty()) output.put("Fluid", stacks[0].save(registries));
    }
    public void deserialize(CompoundTag input, HolderLookup.Provider registries) {
        stacks[0]=input.contains("Fluid",10)?FluidStack.parseOptional(registries,input.getCompound("Fluid")):FluidStack.EMPTY;
    }
    private void check(int index) { if(index<0||index>=stacks.length) throw new IndexOutOfBoundsException(index); }
}