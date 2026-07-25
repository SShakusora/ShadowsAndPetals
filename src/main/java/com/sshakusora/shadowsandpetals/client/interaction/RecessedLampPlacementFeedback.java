package com.sshakusora.shadowsandpetals.client.interaction;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampBlock;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT)
public final class RecessedLampPlacementFeedback {
    private RecessedLampPlacementFeedback() {
    }

    @SubscribeEvent
    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (!event.getLevel().isClientSide()
                || event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK
                || event.getPlayer() == null
                || !(event.getItemStack().getItem() instanceof BlockItem)) {
            return;
        }

        BlockPlaceContext context = new BlockPlaceContext(event.getUseOnContext());
        if (context.canPlace()) {
            return;
        }

        BlockPos placementPos = context.getClickedPos();
        BlockState placementState = event.getLevel().getBlockState(placementPos);
        BlockState clickedState = event.getLevel().getBlockState(event.getPos());
        if (isSlabMountedLamp(placementState) || isSlabMountedLamp(clickedState)) {
            event.getPlayer().sendOverlayMessage(
                    Component.translatable(BuiltinLanguageKeys.RECESSED_LAMP_SPACE_OCCUPIED.key())
            );
        }
    }

    private static boolean isSlabMountedLamp(BlockState state) {
        if (!(state.getBlock() instanceof RecessedLampBlock)) {
            return false;
        }

        RecessedLampBlock.Mount mount = state.getValue(RecessedLampBlock.MOUNT);
        return mount == RecessedLampBlock.Mount.FLOOR_SLAB || mount == RecessedLampBlock.Mount.CEILING_SLAB;
    }
}
