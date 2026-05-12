package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.CafeChairBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT)
public final class CafeChairDyeHintHandler {
    private static final int HOVER_TICKS_REQUIRED = 50;
    private static final int FADE_IN_TICKS = 8;

    @Nullable
    private static BlockPos hoveredTargetPos;
    @Nullable
    private static BlockState hoveredTargetState;
    @Nullable
    private static DyeColor hoveredDyeColor;
    @Nullable
    private static BlockState displayedTargetState;
    @Nullable
    private static DyeColor displayedDyeColor;
    private static int hoverTicks;
    private static int displayTicks;

    private CafeChairDyeHintHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.level == null || minecraft.player == null) {
            clearHoverTarget();
            tickFadeOut();
            return;
        }

        DyeItem dyeItem = getHeldDye(minecraft.player.getMainHandItem(), minecraft.player.getOffhandItem());
        if (dyeItem == null || !(minecraft.hitResult instanceof BlockHitResult hitResult)) {
            clearHoverTarget();
            tickFadeOut();
            return;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        BlockState state = minecraft.level.getBlockState(targetPos);
        DyeColor dyeColor = dyeItem.getDefaultInstance().get(DataComponents.DYE);
        if (dyeColor == null) {
            clearHoverTarget();
            tickFadeOut();
            return;
        }
        if (!(state.getBlock() instanceof CafeChairBlock) || !CafeChairBlock.canApplyDye(state, dyeColor)) {
            clearHoverTarget();
            tickFadeOut();
            return;
        }

        if (!targetPos.equals(hoveredTargetPos) || !state.equals(hoveredTargetState) || dyeColor != hoveredDyeColor) {
            hoveredTargetPos = targetPos.immutable();
            hoveredTargetState = state;
            hoveredDyeColor = dyeColor;
            hoverTicks = 1;
        } else {
            hoverTicks++;
        }

        if (displayedTargetState == null && hoverTicks >= HOVER_TICKS_REQUIRED) {
            displayedTargetState = hoveredTargetState;
            displayedDyeColor = hoveredDyeColor;
            displayTicks = 1;
        } else if (displayedTargetState != null) {
            if (shouldKeepDisplayedHint()) {
                displayTicks = Math.min(displayTicks + 1, FADE_IN_TICKS);
            } else {
                tickFadeOut();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || displayedTargetState == null || displayedDyeColor == null || displayTicks <= 0) {
            return;
        }

        float alphaProgress = Mth.clamp(displayTicks / (float) FADE_IN_TICKS, 0.0F, 1.0F);
        int alpha = Math.max(4, Mth.ceil(alphaProgress * 255.0F));
        renderHint(event.getGuiGraphics(), minecraft, alpha);
    }

    private static void renderHint(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int alpha) {
        String prefix = I18n.get(CafeChairBlock.DYE_HINT_PREFIX_KEY, displayedTargetState.getBlock().getName().getString());
        String colorName = I18n.get("color.minecraft." + displayedDyeColor.getName());

        int prefixColor = withAlpha(0xFFFFFF, alpha);
        int dyeColor = withAlpha(displayedDyeColor.getTextColor(), alpha);
        int y = guiGraphics.guiHeight() - 68;
        int totalWidth = minecraft.font.width(prefix) + minecraft.font.width(colorName);
        int x = (guiGraphics.guiWidth() - totalWidth) / 2;

        guiGraphics.text(minecraft.font, prefix, x, y, prefixColor, true);
        guiGraphics.text(minecraft.font, colorName, x + minecraft.font.width(prefix), y, dyeColor, true);
    }

    @Nullable
    private static DyeItem getHeldDye(ItemStack mainHandItem, ItemStack offhandItem) {
        if (mainHandItem.getItem() instanceof DyeItem dyeItem) {
            return dyeItem;
        }
        return offhandItem.getItem() instanceof DyeItem dyeItem ? dyeItem : null;
    }

    private static void clearHoverTarget() {
        hoveredTargetPos = null;
        hoveredTargetState = null;
        hoveredDyeColor = null;
        hoverTicks = 0;
    }

    private static boolean shouldKeepDisplayedHint() {
        return hoverTicks >= HOVER_TICKS_REQUIRED
                && displayedTargetState != null
                && displayedTargetState.equals(hoveredTargetState)
                && displayedDyeColor == hoveredDyeColor;
    }

    private static void tickFadeOut() {
        if (displayTicks > 0) {
            displayTicks--;
            if (displayTicks == 0) {
                displayedTargetState = null;
                displayedDyeColor = null;
            }
        }
    }

    private static int withAlpha(int color, int alpha) {
        return alpha << 24 | color & 0xFFFFFF;
    }
}
