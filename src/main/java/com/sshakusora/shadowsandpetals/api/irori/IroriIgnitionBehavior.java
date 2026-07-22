package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.world.item.ItemStack;

/**
 * Describes an item that can ignite an Irori and its successful server-side side effects.
 */
public interface IroriIgnitionBehavior {
    /** Returns whether this behavior handles the supplied stack. This method must be read-only. */
    boolean matches(ItemStack stack);

    /**
     * Applies sound, durability, consumption, or other effects after the Irori was ignited.
     * This callback is invoked only on the logical server.
     */
    void onIgnited(IroriIgnitionContext context);
}
