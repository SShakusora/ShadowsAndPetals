package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.item.hammer.HammerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.theme.Theme;
import snownee.jade.api.ui.*;
import snownee.jade.overlay.DisplayHelper;

/**
 * Renders Hammer progress in the same bottom-edge slot used by Jade's native
 * block-breaking progress indicator.
 */
public final class RockeryHammerProgressOverlay {
    private static final float FADE_SPEED = 0.1F;
    private static final float MAX_ALPHA = 0.6F;
    private static final float MAX_PREDICTED_PROGRESS = 0.99F;
    private static final long NANOS_PER_TICK = 50_000_000L;

    private static boolean active;
    private static float lastServerProgress = -1.0F;
    private static float anchorProgress;
    private static float durationTicks = 1.0F;
    private static float savedProgress;
    private static float progressAlpha;
    private static long anchorNanos;

    private RockeryHammerProgressOverlay() {}

    public static void onTooltipCollected(BoxElement root, Accessor<?> accessor) {
        boolean wasActive = active;
        active = false;

        if (!(accessor instanceof BlockAccessor blockAccessor)
                || !IWailaConfig.get().plugin().get(JadeIds.MC_BREAKING_PROGRESS)
                || !(accessor.getPlayer().getMainHandItem().getItem() instanceof HammerItem)
                || !accessor.getPlayer().isUsingItem()) {
            return;
        }

        int progressPercent = blockAccessor.getServerData()
                .getInt(RockeryHammerProgressServerDataProvider.PROGRESS_KEY)
                .orElse(-1);
        if (progressPercent < 0) {
            return;
        }

        float serverProgress = Mth.clamp(progressPercent / 100.0F, 0.0F, 1.0F);
        float newDuration = Math.max(1.0F, blockAccessor.getServerData()
                .getFloat(RockeryHammerProgressServerDataProvider.DURATION_KEY)
                .orElse(1.0F));
        long now = System.nanoTime();

        if (!wasActive || serverProgress < lastServerProgress) {
            anchorProgress = serverProgress;
            anchorNanos = now;
        } else if (serverProgress > lastServerProgress) {
            anchorProgress = Math.max(serverProgress, predictProgress(now));
            anchorNanos = now;
        }

        durationTicks = newDuration;
        lastServerProgress = serverProgress;
        savedProgress = predictProgress(now);
        active = true;

        // A non-NaN box progress tells Jade's built-in mining callback not to
        // draw a second progress bar for this tooltip.
        root.setBoxProgress(MessageType.NORMAL, savedProgress);
    }

    public static void afterRender(
            BoxElement root,
            TooltipAnimation animation,
            GuiGraphicsExtractor graphics,
            Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor)) {
            active = false;
        }
        if (!IWailaConfig.get().plugin().get(JadeIds.MC_BREAKING_PROGRESS)) {
            active = false;
            progressAlpha = 0.0F;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float deltaTicks = minecraft.getDeltaTracker().getGameTimeDeltaTicks();

        if (!active && minecraft.gameMode != null && minecraft.gameMode.isDestroying()) {
            progressAlpha = 0.0F;
            return;
        }

        progressAlpha = Mth.clamp(
                progressAlpha + deltaTicks * (active ? FADE_SPEED : -FADE_SPEED),
                0.0F,
                MAX_ALPHA);
        if (progressAlpha == 0.0F) {
            return;
        }

        if (active) {
            savedProgress = predictProgress(System.nanoTime());
        }

        Theme theme = IThemeHelper.get().theme();
        ColorPalette colors = theme.tooltipStyle.boxProgressColors;
        int color = IWailaConfig.Overlay.applyAlpha(colors.title(), progressAlpha * animation.alpha);
        float top = root.getY() + root.getHeight();
        float width = root.getWidth();
        float topOffset = theme.tooltipStyle.boxProgressOffset(ScreenDirection.UP);
        float rightOffset = theme.tooltipStyle.boxProgressOffset(ScreenDirection.RIGHT);
        float bottomOffset = theme.tooltipStyle.boxProgressOffset(ScreenDirection.DOWN);
        float leftOffset = theme.tooltipStyle.boxProgressOffset(ScreenDirection.LEFT);
        width += rightOffset - leftOffset;

        DisplayHelper.fill(
                graphics,
                leftOffset,
                top - 1.0F + topOffset,
                leftOffset + width * savedProgress,
                top + bottomOffset,
                color);
    }

    private static float predictProgress(long now) {
        if (anchorNanos == 0L) {
            return Mth.clamp(anchorProgress, 0.0F, MAX_PREDICTED_PROGRESS);
        }
        float elapsedTicks = (float) (now - anchorNanos) / NANOS_PER_TICK;
        return Mth.clamp(
                anchorProgress + elapsedTicks / Math.max(1.0F, durationTicks),
                0.0F,
                MAX_PREDICTED_PROGRESS);
    }
}
