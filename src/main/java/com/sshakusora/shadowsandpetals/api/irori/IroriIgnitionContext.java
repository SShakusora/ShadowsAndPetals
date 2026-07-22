package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;

/** Mutable server-side context supplied after a registered igniter successfully lights an Irori. */
public record IroriIgnitionContext(
        Level level,
        BlockPos interactionPos,
        BlockPos masterPos,
        Player player,
        InteractionHand hand,
        ItemStack stack
) {
    public IroriIgnitionContext {
        level = Objects.requireNonNull(level, "level");
        if (level.isClientSide()) {
            throw new IllegalArgumentException("Irori ignition effects must run on the logical server");
        }
        interactionPos = Objects.requireNonNull(interactionPos, "interactionPos").immutable();
        masterPos = Objects.requireNonNull(masterPos, "masterPos").immutable();
        player = Objects.requireNonNull(player, "player");
        hand = Objects.requireNonNull(hand, "hand");
        stack = Objects.requireNonNull(stack, "stack");
    }
}
