package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * Read-only description of content placed on an Irori surface.
 *
 * <p>The built-in implementation currently exposes blocks above the center of the Irori. Item
 * content is part of the API contract so future placed-food and placed-item mechanics can use the
 * same rules without changing the grill API.
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
