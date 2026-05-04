package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.CafeChairBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT)
public final class CafeChairDyeHintHandler {
    private static final int HOVER_TICKS_REQUIRED = 60;
    private static final int MESSAGE_INTERVAL_TICKS = 10;

    private static BlockPos lastTargetPos;
    private static DyeColor lastDyeColor;
    private static int hoverTicks;

    private CafeChairDyeHintHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.level == null || minecraft.player == null) {
            reset();
            return;
        }

        DyeItem dyeItem = getHeldDye(minecraft.player.getMainHandItem(), minecraft.player.getOffhandItem());
        if (dyeItem == null || !(minecraft.hitResult instanceof BlockHitResult hitResult)) {
            reset();
            return;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        BlockState state = minecraft.level.getBlockState(targetPos);
        DyeColor dyeColor = dyeItem.getDyeColor();
        if (!(state.getBlock() instanceof CafeChairBlock) || !CafeChairBlock.canApplyDye(state, dyeColor)) {
            reset();
            return;
        }

        if (!targetPos.equals(lastTargetPos) || dyeColor != lastDyeColor) {
            lastTargetPos = targetPos.immutable();
            lastDyeColor = dyeColor;
            hoverTicks = 0;
        }

        hoverTicks++;
        if (hoverTicks >= HOVER_TICKS_REQUIRED && (hoverTicks - HOVER_TICKS_REQUIRED) % MESSAGE_INTERVAL_TICKS == 0) {
            minecraft.player.displayClientMessage(CafeChairBlock.createDyeHintMessage(state, dyeColor), true);
        }
    }

    private static DyeItem getHeldDye(ItemStack mainHandItem, ItemStack offhandItem) {
        if (mainHandItem.getItem() instanceof DyeItem dyeItem) {
            return dyeItem;
        }
        return offhandItem.getItem() instanceof DyeItem dyeItem ? dyeItem : null;
    }

    private static void reset() {
        lastTargetPos = null;
        lastDyeColor = null;
        hoverTicks = 0;
    }
}
