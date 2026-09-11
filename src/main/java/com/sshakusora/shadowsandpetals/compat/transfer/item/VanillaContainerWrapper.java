package com.sshakusora.shadowsandpetals.compat.transfer.item;

import com.sshakusora.shadowsandpetals.compat.transfer.ResourceHandler;
import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class VanillaContainerWrapper {
    private VanillaContainerWrapper(){}
    public static ResourceHandler<ItemResource> of(Container container){ return new ResourceHandler<>() {
        public int size(){return container.getContainerSize();}
        public ItemResource getResource(int i){return ItemResource.of(container.getItem(i));}
        public long getAmountAsLong(int i){return container.getItem(i).getCount();}
        public long getCapacityAsLong(int i,ItemResource r){return container.getMaxStackSize();}
        public boolean isValid(int i,ItemResource r){return container.canPlaceItem(i,r.toStack());}
        public int insert(int i,ItemResource r,int amount,TransactionContext tx){if(amount<=0||!isValid(i,r))return 0;ItemStack cur=container.getItem(i),previous=cur.copy();int room=cur.isEmpty()?container.getMaxStackSize():Math.max(0,container.getMaxStackSize()-cur.getCount());int n=Math.min(amount,room);if(n>0){container.setItem(i,cur.isEmpty()?r.toStack(n):cur.copyWithCount(cur.getCount()+n));if(tx!=null)tx.addRollback(()->container.setItem(i,previous));}return n;}
        public int extract(int i,ItemResource r,int amount,TransactionContext tx){ItemStack cur=container.getItem(i);if(cur.isEmpty()||amount<=0||r==null||r.isEmpty()||!ItemStack.isSameItemSameComponents(cur,r.toStack(1)))return 0;ItemStack previous=cur.copy();int n=Math.min(amount,cur.getCount());container.setItem(i,cur.copyWithCount(cur.getCount()-n));if(tx!=null)tx.addRollback(()->container.setItem(i,previous));return n;}
    }; }
}