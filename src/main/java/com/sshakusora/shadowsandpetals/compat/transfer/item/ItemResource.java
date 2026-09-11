package com.sshakusora.shadowsandpetals.compat.transfer.item;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemResource {
    public static final ItemResource EMPTY = new ItemResource(ItemStack.EMPTY);
    private final ItemStack stack;
    private ItemResource(ItemStack stack) { this.stack=stack==null?ItemStack.EMPTY:stack.copy(); }
    public static ItemResource of(ItemStack stack) { return stack==null||stack.isEmpty()?EMPTY:new ItemResource(stack); }
    public ItemStack toStack() { return stack.copy(); }
    public ItemStack toStack(int amount) { return stack.copyWithCount(Math.max(0, amount)); }
    public Item getItem() { return stack.getItem(); }
    public int getAmount() { return stack.getCount(); }
    public boolean isEmpty() { return stack.isEmpty(); }
    public boolean is(Item item) { return !stack.isEmpty() && stack.is(item); }
    public ItemResource withMergedPatch(DataComponentPatch patch) { ItemStack copy=stack.copy(); copy.applyComponents(patch); return of(copy); }
    public ItemResource without(DataComponentType<?> type) { ItemStack copy=stack.copy(); copy.remove(type); return of(copy); }
    @Override public boolean equals(Object obj) { return obj instanceof ItemResource other && ItemStack.isSameItemSameComponents(stack,other.stack); }
    @Override public int hashCode() { return stack.getItem().hashCode(); }
}