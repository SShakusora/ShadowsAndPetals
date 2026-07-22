package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.OptionalInt;

/**
 * Resolves the burn time of an item used as Irori fuel.
 *
 * <p>Return an empty result when the rule does not handle the stack. Rules are evaluated by
 * descending priority, then registration order. Implementations must not mutate the supplied
 * stack or level.
 */
@FunctionalInterface
public interface IroriFuelRule {
    OptionalInt getBurnTime(ItemStack stack, Level level);
}
