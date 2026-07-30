package com.sshakusora.shadowsandpetals.event;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampCompositeBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT)
public final class RecessedLampHarvestClientEvents {
    private RecessedLampHarvestClientEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!(event.getLevel() instanceof ClientLevel) || !(event.getTargetBlock().getBlock() instanceof RecessedLampCompositeBlock)) {
            return;
        }

        BlockState storedSlab = RecessedLampCompositeBlock.getEffectiveStoredSlab(
                event.getLevel(),
                event.getPos(),
                event.getTargetBlock()
        );
        if (storedSlab != null) {
            event.setCanHarvest(storedSlab.canHarvestBlock(
                    event.getLevel(),
                    event.getPos(),
                    event.getEntity()
            ));
        }
    }
}
