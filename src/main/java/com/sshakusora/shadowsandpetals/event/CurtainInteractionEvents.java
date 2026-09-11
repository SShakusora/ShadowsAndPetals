package com.sshakusora.shadowsandpetals.event;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainBlock;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Keeps an in-flight curtain transition from being re-triggered or bypassed
 * by an item interaction.
 */
@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class CurtainInteractionEvents {
    private CurtainInteractionEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!CurtainBlock.isAnimating(event.getLevel().getBlockState(event.getPos()))) {
            return;
        }

        // A cancelled RightClickBlock normally returns PASS and lets vanilla
        // continue with RightClickItem. CONSUME makes the animation lock apply
        // to empty-hand, held-item, and sneak interactions alike.
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }
}
