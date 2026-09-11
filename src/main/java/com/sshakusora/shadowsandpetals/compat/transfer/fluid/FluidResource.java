package com.sshakusora.shadowsandpetals.compat.transfer.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class FluidResource {
    public static final FluidResource EMPTY = new FluidResource(FluidStack.EMPTY);
    private final FluidStack stack;
    private FluidResource(FluidStack stack) { this.stack = stack.isEmpty() ? FluidStack.EMPTY : stack.copy(); }
    public static FluidResource of(Fluid fluid) { return fluid == Fluids.EMPTY ? EMPTY : new FluidResource(new FluidStack(fluid, 1)); }
    public static FluidResource of(FluidStack stack) { return stack == null || stack.isEmpty() ? EMPTY : new FluidResource(stack); }
    public boolean isEmpty() { return stack.isEmpty(); }
    public boolean is(Fluid fluid) { return !isEmpty() && stack.getFluid() == fluid; }
    public Fluid getFluid() { return stack.isEmpty() ? Fluids.EMPTY : stack.getFluid(); }
    public FluidStack toStack(int amount) { return stack.isEmpty() || amount <= 0 ? FluidStack.EMPTY : stack.copyWithAmount(amount); }
    public FluidStack toStack() { return toStack(1); }
    public FluidStack asStack() { return stack.copy(); }
    @Override public boolean equals(Object obj) { return obj instanceof FluidResource other && (isEmpty() ? other.isEmpty() : !other.isEmpty() && FluidStack.isSameFluidSameComponents(stack, other.stack)); }
    @Override public int hashCode() { return getFluid().hashCode(); }
    @Override public String toString() { return isEmpty() ? "empty" : stack.toString(); }
}