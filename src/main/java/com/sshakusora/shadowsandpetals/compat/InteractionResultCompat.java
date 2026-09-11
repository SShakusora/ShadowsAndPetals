package com.sshakusora.shadowsandpetals.compat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

/** Bridges the 1.21.1 split between block/item interaction result types. */
public final class InteractionResultCompat {
    private InteractionResultCompat() {}

    public static ItemInteractionResult asItem(InteractionResult result) {
        return switch (result) {
            case SUCCESS -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
}
