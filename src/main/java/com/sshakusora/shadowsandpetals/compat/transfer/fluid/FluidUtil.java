package com.sshakusora.shadowsandpetals.compat.transfer.fluid;

import com.sshakusora.shadowsandpetals.compat.transfer.ResourceHandler;
import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class FluidUtil {
    private FluidUtil(){}
    public static boolean interactWithFluidHandler(Player p,InteractionHand h,Level l,BlockPos pos,Direction side){return net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(p,h,l,pos,side);}
    private static IFluidHandler adapter(ResourceHandler<FluidResource> handler){return new IFluidHandler(){
        public int getTanks(){return handler.size();}
        public FluidStack getFluidInTank(int i){FluidResource r=handler.getResource(i);return r.toStack((int)handler.getAmountAsLong(i));}
        public int getTankCapacity(int i){return handler.getCapacityAsInt(i,handler.getResource(i));}
        public boolean isFluidValid(int i,FluidStack s){return handler.isValid(i,FluidResource.of(s));}
        public int fill(FluidStack s,FluidAction a){
            if (s == null || s.isEmpty()) return 0;
            FluidResource requested = FluidResource.of(s);
            FluidResource current = handler.getResource(0);
            if (!handler.isValid(0, requested)
                    || (!current.isEmpty() && !current.equals(requested))) return 0;
            int free = Math.max(0, handler.getCapacityAsInt(0, requested) - handler.getAmountAsInt(0));
            int amount = Math.min(s.getAmount(), free);
            return a.execute()
                    ? handler.insert(0, requested, amount, (TransactionContext)null)
                    : amount;
        }
        public FluidStack drain(FluidStack s,FluidAction a){
            if (s == null || s.isEmpty()) return FluidStack.EMPTY;
            FluidResource requested = FluidResource.of(s);
            FluidResource current = handler.getResource(0);
            if (current.isEmpty() || !current.equals(requested)) return FluidStack.EMPTY;
            int amount = Math.min(s.getAmount(), handler.getAmountAsInt(0));
            int extracted = a.execute()
                    ? handler.extract(0, requested, amount, (TransactionContext)null)
                    : amount;
            return requested.toStack(extracted);
        }
        public FluidStack drain(int n,FluidAction a){
            FluidResource current=handler.getResource(0);
            if(current.isEmpty()||n<=0)return FluidStack.EMPTY;
            int amount=Math.min(n,handler.getAmountAsInt(0));
            int extracted=a.execute()
                    ? handler.extract(0,current,amount,(TransactionContext)null)
                    : amount;
            return current.toStack(extracted);
        }
    };}
    public static boolean tryPlaceFluid(FluidResource resource,Player p,Level l,InteractionHand h,BlockPos pos){FluidTank tank=new FluidTank(Integer.MAX_VALUE);tank.setFluid(resource.toStack(Integer.MAX_VALUE));return net.neoforged.neoforge.fluids.FluidUtil.tryPlaceFluid(p,l,h,pos,tank,resource.toStack(Integer.MAX_VALUE));}
    public static boolean tryPlaceFluid(ResourceHandler<FluidResource> source,Player p,Level l,InteractionHand h,BlockPos pos){FluidResource r=source.getResource(0);return !r.isEmpty()&&net.neoforged.neoforge.fluids.FluidUtil.tryPlaceFluid(p,l,h,pos,adapter(source),r.toStack((int)source.getAmountAsLong(0)));}
    public static FluidStack tryPickupFluid(ResourceHandler<FluidResource> destination,Player p,Level l,BlockPos pos,Direction side){
        return tryPickupFluid(destination, p, l, p.getUsedItemHand(), pos, side);
    }
    public static FluidStack tryPickupFluid(ResourceHandler<FluidResource> destination,Player p,Level l,InteractionHand hand,BlockPos pos,Direction side){
        var source = l.getFluidState(pos).getType();
        if (source == net.minecraft.world.level.material.Fluids.EMPTY) return FluidStack.EMPTY;
        FluidActionResult result=net.neoforged.neoforge.fluids.FluidUtil.tryPickUpFluid(p.getItemInHand(hand),p,l,pos,side);
        return result.isSuccess()?new FluidStack(source,net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME):FluidStack.EMPTY;
    }
}