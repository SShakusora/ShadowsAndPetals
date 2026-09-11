package com.sshakusora.shadowsandpetals.compat.transfer;

import com.sshakusora.shadowsandpetals.compat.transfer.access.ItemAccess;
import com.sshakusora.shadowsandpetals.compat.transfer.item.ItemResource;
import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;

public abstract class ItemAccessResourceHandler<T> implements ResourceHandler<T> {
    protected final ItemAccess itemAccess;
    private final int slots;
    protected ItemAccessResourceHandler(ItemAccess access,int slots){this.itemAccess=access;this.slots=slots;}
    protected abstract T getResourceFrom(ItemResource accessResource,int index);
    protected int getAmountFrom(ItemResource accessResource,int index){return accessResource.isEmpty()?0:1;}
    protected abstract ItemResource update(ItemResource accessResource,int index,T newResource,int newAmount);
    protected abstract int getCapacity(int index,T resource);
    @Override public int size(){return slots;}
    @Override public T getResource(int index){return getResourceFrom(itemAccess.getResource(),index);}
    @Override public long getAmountAsLong(int index){return getAmountFrom(itemAccess.getResource(),index);}
    @Override public long getCapacityAsLong(int index,T resource){return getCapacity(index,resource);}
    @Override public int insert(int index,T resource,int amount,TransactionContext tx){if(amount<=0||!isValid(index,resource))return 0;int current=(int)getAmountAsLong(index),add=Math.min(amount,getCapacity(index,resource)-current);if(add>0){ItemResource previous=itemAccess.getResource();itemAccess.setResource(update(previous,index,resource,current+add));if(tx!=null)tx.addRollback(()->itemAccess.setResource(previous));}return add;}
    @Override public int extract(int index,T resource,int amount,TransactionContext tx){if(amount<=0||!isValid(index,resource))return 0;int current=(int)getAmountAsLong(index),out=Math.min(amount,current);if(out>0){ItemResource previous=itemAccess.getResource();itemAccess.setResource(update(previous,index,resource,current-out));if(tx!=null)tx.addRollback(()->itemAccess.setResource(previous));}return out;}
}