package com.sshakusora.shadowsandpetals.event;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockTagRegistry;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class IroriSurfacePlacementEvents {
    private IroriSurfacePlacementEvents() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!event.getPlacedBlock().is(BlockTagRegistry.REQUIRES_IRORI_GRILL)) {
            return;
        }

        BlockPos placementPos = event.getPos();
        BlockPos cookingPos = placementPos.below();
        if (event.getLevel().getBlockEntity(cookingPos) instanceof IroriBlockEntity irori && irori.hasCookingItem(cookingPos)) {
            event.setCanceled(true);
        }
    }
}
