package com.sshakusora.shadowsandpetals.compat.transfer.access;

import com.sshakusora.shadowsandpetals.compat.transfer.ResourceHandler;
import com.sshakusora.shadowsandpetals.compat.transfer.item.ItemResource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import com.sshakusora.shadowsandpetals.compat.transfer.fluid.FluidResource;
import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;
import java.util.function.Consumer;

public final class ItemAccess {
    private ItemStack stack;
    private final Consumer<ItemStack> setter;
    private final ResourceHandler<ItemResource> delegatedHandler;
    private final int delegatedIndex;
    private ItemAccess(ItemStack stack, Consumer<ItemStack> setter){this(stack,setter,null,-1);}
    private ItemAccess(ItemStack stack, Consumer<ItemStack> setter, ResourceHandler<ItemResource> delegatedHandler, int delegatedIndex){this.stack=stack;this.setter=setter;this.delegatedHandler=delegatedHandler;this.delegatedIndex=delegatedIndex;}
    public static ItemAccess forStack(ItemStack stack){return new ItemAccess(stack, s->{ stack.setCount(s.getCount()); stack.applyComponents(s.getComponentsPatch()); });}
    public static ItemAccess forPlayerInteraction(Player player, InteractionHand hand){return new ItemAccess(player.getItemInHand(hand), s->player.setItemInHand(hand,s));}
    public static ItemAccess forHandlerIndexStrict(ResourceHandler<ItemResource> handler, int index){return new ItemAccess(handler.getResource(index).toStack(), s->handler.extract(index,handler.getResource(index),Integer.MAX_VALUE,null),handler,index);}
    public ItemAccess oneByOne(){ if(stack.getCount()!=1) stack=stack.copyWithCount(1); return this; }
    public ItemResource getResource(){return delegatedHandler == null ? ItemResource.of(stack) : delegatedHandler.getResource(delegatedIndex);}
    public void setResource(ItemResource resource){if(delegatedHandler != null){ItemResource current=delegatedHandler.getResource(delegatedIndex);if(!current.isEmpty())delegatedHandler.extract(delegatedIndex,current,(int)delegatedHandler.getAmountAsLong(delegatedIndex),null);if(!resource.isEmpty())delegatedHandler.insert(delegatedIndex,resource,(int)resource.getAmount(),null);}else{stack=resource.toStack();setter.accept(stack);}}
    @SuppressWarnings("unchecked")
    public <T> ResourceHandler<T> getCapability(Object capability){
        if (capability == Capabilities.FluidHandler.ITEM) {
            IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (handler == null) return null;
            return (ResourceHandler<T>) new ResourceHandler<FluidResource>() {
                public int size(){ return handler.getTanks(); }
                public FluidResource getResource(int i){ FluidStack fluid=handler.getFluidInTank(i); return FluidResource.of(fluid); }
                public long getAmountAsLong(int i){ return handler.getFluidInTank(i).getAmount(); }
                public long getCapacityAsLong(int i,FluidResource r){ return handler.getTankCapacity(i); }
                public boolean isValid(int i,FluidResource r){ return handler.isFluidValid(i,r.toStack(1)); }
                public int insert(int i,FluidResource r,int n,TransactionContext tx){ return handler.fill(r.toStack(n), IFluidHandler.FluidAction.EXECUTE); }
                public int extract(int i,FluidResource r,int n,TransactionContext tx){ return handler.drain(r.toStack(n), IFluidHandler.FluidAction.EXECUTE).getAmount(); }
            };
        }
        if (capability == Capabilities.ItemHandler.ITEM) {
            IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
            if (handler == null) return null;
            return (ResourceHandler<T>) new ResourceHandler<ItemResource>() {
                public int size(){ return handler.getSlots(); }
                public ItemResource getResource(int i){ return ItemResource.of(handler.getStackInSlot(i)); }
                public long getAmountAsLong(int i){ return handler.getStackInSlot(i).getCount(); }
                public long getCapacityAsLong(int i,ItemResource r){ return handler.getSlotLimit(i); }
                public boolean isValid(int i,ItemResource r){ return handler.isItemValid(i,r.toStack()); }
                public int insert(int i,ItemResource r,int n,TransactionContext tx){ return n-handler.insertItem(i,r.toStack(n),false).getCount(); }
                public int extract(int i,ItemResource r,int n,TransactionContext tx){ return handler.extractItem(i,n,false).getCount(); }
            };
        }
        return null;
    }
}
