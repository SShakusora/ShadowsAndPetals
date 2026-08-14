package com.sshakusora.shadowsandpetals.event;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriComponentTopology;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.registries.TriggerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Set;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class IroriAdvancementEvents {
    private IroriAdvancementEvents() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getPlacedBlock().is(BlockRegistry.IRORI.get())) {
            return;
        }

        if (formsComplete2x2(serverLevel, event.getPos())) {
            TriggerRegistry.IRORI_2X2_FORMED.get().trigger(player);
        }
    }

    private static boolean formsComplete2x2(ServerLevel level, BlockPos placedPos) {
        Set<BlockPos> component = IroriComponentTopology.collectConnectedComponent(level, placedPos);
        if (component.size() != 4) {
            return false;
        }

        IroriComponentTopology.Bounds bounds = IroriComponentTopology.bounds(component);
        return bounds.width() == 2 && bounds.depth() == 2;
    }
}
