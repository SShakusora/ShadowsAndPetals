package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Contributes item drops produced when Irori ash is cleared or its component is removed. */
@FunctionalInterface
public interface IroriAshDropProvider {
    List<ItemStack> getDrops(IroriAshDropContext context);
}
