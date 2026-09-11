package com.sshakusora.shadowsandpetals.compat.transfer.item;

import com.sshakusora.shadowsandpetals.compat.transfer.ResourceHandler;
import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

public abstract class ItemStackResourceHandler implements ResourceHandler<ItemResource> {
    protected abstract ItemStack getStack();
    protected abstract void setStack(ItemStack stack);
    protected abstract boolean isValid(ItemResource resource);
    protected abstract int getCapacity(ItemResource resource);
    @Override public int size(){return 1;}
    @Override public ItemResource getResource(int index){return index==0?ItemResource.of(getStack()):ItemResource.EMPTY;}
    @Override public long getAmountAsLong(int index){return index==0?getStack().getCount():0;}
    @Override public long getCapacityAsLong(int index, ItemResource resource){return index==0?getCapacity(resource):0;}
    @Override public boolean isValid(int index, ItemResource resource){return index==0 && isValid(resource);}
    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (index != 0 || amount <= 0 || resource == null || resource.isEmpty() || !isValid(resource)) {
            return 0;
        }

        ItemStack current = getStack();
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, resource.toStack(1))) {
            return 0;
        }

        int free = Math.max(0, getCapacity(resource) - current.getCount());
        int inserted = Math.min(amount, free);
        if (inserted <= 0) {
            return 0;
        }

        ItemStack updated = current.isEmpty()
                ? resource.toStack(inserted)
                : current.copyWithCount(current.getCount() + inserted);
        setStack(updated);
        if (tx != null) {
            ItemStack previous = current.copy();
            tx.addRollback(() -> setStack(previous));
        }
        return inserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        if (index != 0 || amount <= 0 || resource == null || resource.isEmpty()) {
            return 0;
        }

        ItemStack current = getStack();
        if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, resource.toStack(1))) {
            return 0;
        }

        int extracted = Math.min(amount, current.getCount());
        if (extracted <= 0) {
            return 0;
        }
        ItemStack previous = current.copy();
        setStack(extracted >= current.getCount()
                ? ItemStack.EMPTY
                : current.copyWithCount(current.getCount() - extracted));
        if (tx != null) {
            tx.addRollback(() -> setStack(previous));
        }
        return extracted;
    }
}