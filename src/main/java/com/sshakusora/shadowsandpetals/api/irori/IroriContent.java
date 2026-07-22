package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * Read-only description of content placed on an Irori surface.
 *
 * <p>The built-in implementation exposes blocks above the center of the Irori and items placed on
 * its cooking surface, allowing both kinds of content to participate in the same grill rules.
 */
public interface IroriContent {
    BlockPos position();

    record BlockContent(BlockPos position, BlockState state) implements IroriContent {
        public BlockContent {
            position = Objects.requireNonNull(position, "position").immutable();
            state = Objects.requireNonNull(state, "state");
        }
    }

    record ItemContent(BlockPos position, ItemStack stack) implements IroriContent {
        public ItemContent {
            position = Objects.requireNonNull(position, "position").immutable();
            stack = Objects.requireNonNull(stack, "stack").copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
