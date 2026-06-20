package com.sshakusora.shadowsandpetals.client.tooltip;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Client-side tooltip component that renders a rockery block model
 * preview via the Picture-in-Picture system.
 * <p>
 * The preview only appears when the player holds {@code Shift};
 * otherwise a hint line is shown.
 */
public class ClientRockeryTooltip implements ClientTooltipComponent {

    private static final int PREVIEW_SIZE = 64;
    private static final int PADDING = 4;

    private final RockeryBlock block;
    private final RockeryDimensions dimensions;

    public ClientRockeryTooltip(RockeryTooltipComponent component) {
        this.block = component.block();
        this.dimensions = component.dimensions();
    }

    @Override
    public int getHeight(Font font) {
        if (!Minecraft.getInstance().hasShiftDown()) {
            return font.lineHeight + PADDING;
        }
        return font.lineHeight * 2 + PREVIEW_SIZE + PADDING * 2;
    }

    @Override
    public int getWidth(Font font) {
        int hintWidth = font.width(hintText(Minecraft.getInstance().hasShiftDown()));
        if (!Minecraft.getInstance().hasShiftDown()) {
            return hintWidth + PADDING * 2;
        }
        int textWidth = font.width(dimensionLabel());
        return Math.max(hintWidth, Math.max(PREVIEW_SIZE, textWidth)) + PADDING * 2;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        if (!Minecraft.getInstance().hasShiftDown()) {
            return;
        }

        int previewX = x + (w - PREVIEW_SIZE) / 2;
        int previewY = y + font.lineHeight + PADDING;

        var scissor = graphics.peekScissorStack();

        graphics.submitPictureInPictureRenderState(
            new RockeryPreviewState(block, dimensions,
                previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE, scissor)
        );

        Component label = dimensionLabel();
        int labelX = x + w / 2 - font.width(label) / 2;
        int labelY = previewY + PREVIEW_SIZE + PADDING;
        graphics.text(font, label, labelX, labelY, 0xFFFFFFFF);
    }

    @Override
    public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        boolean active = Minecraft.getInstance().hasShiftDown();
        graphics.text(font, hintText(active), x, y, 0xFF_AAAAAA);
    }

    private Component hintText(boolean active) {
        return TooltipHelper.buildHint(
                BuiltinLanguageKeys.ROCKERY_HOLD_FOR_PREVIEW.key(),
                Component.translatable(BuiltinLanguageKeys.TOOLTIP_HOLD_KEY_SHIFT.key()),
                active);
    }

    private MutableComponent dimensionLabel() {
        return Component.empty()
                .append(Component.translatable(BuiltinLanguageKeys.ROCKERY_DIMENSIONS_LABEL.key()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Integer.toString(dimensions.width())).withStyle(ChatFormatting.RED))
                .append(Component.literal("×").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(Integer.toString(dimensions.height())).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("×").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(Integer.toString(dimensions.depth())).withStyle(ChatFormatting.BLUE));
    }
}
