package com.sshakusora.shadowsandpetals.client.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Text utility helpers for the tooltip system.
 * <p>
 * Primary feature is {@link #cutTextComponent(Component, Style, Style)} which
 * performs locale-aware word-wrapping at a fixed width and alternates between two
 * styles on underscore ({@code _}) boundaries for inline highlighting.
 */
public final class TooltipHelper {
    public static final int MAX_WIDTH_PER_LINE = 200;
    public static final Style PRIMARY_STYLE = Style.EMPTY.withColor(TextColor.parseColor("#61C3C7").getOrThrow());
    public static final Style HIGHLIGHT_STYLE = Style.EMPTY.withColor(TextColor.parseColor("#B8FFE0").getOrThrow());

    private TooltipHelper() {}

    /**
     * Build the "Hold [Shift] for Description" hint line.
     */
    public static MutableComponent holdShift() {
        return holdKey("shift");
    }

    /**
     * Build the "Hold [Ctrl] for Controls" hint line.
     */
    public static MutableComponent holdCtrl() {
        return holdKey("ctrl");
    }

    private static MutableComponent holdKey(String key) {
        String template = Component.translatable("tooltip.shadowsandpetals.holdKey." + key).getString();
        return Component.literal(template).withStyle(ChatFormatting.DARK_GRAY);
    }

    /**
     * Builds a hold-key hint using the shared tooltip colour scheme: dark-gray
     * surrounding text and a gray (or white when active) key name.
     */
    public static MutableComponent buildHint(String templateKey, MutableComponent keyName, boolean active) {
        String template = Component.translatable(templateKey).getString();
        String[] parts = template.split("%s", -1);
        MutableComponent result = Component.empty();
        result.append(Component.literal(parts[0]).withStyle(ChatFormatting.DARK_GRAY));
        result.append(keyName.plainCopy().withStyle(active ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        if (parts.length > 1) {
            result.append(Component.literal(parts[1]).withStyle(ChatFormatting.DARK_GRAY));
        }
        return result;
    }

    public static List<Component> cutTextComponent(Component text, Style primary, Style highlight) {
        return cutTextComponent(text, primary, highlight, 0);
    }

    public static List<Component> cutTextComponent(Component text, Style primary, Style highlight, int indent) {
        String s = text.getString();
        List<String> words = new LinkedList<>();
        BreakIterator iterator = BreakIterator.getLineInstance(Minecraft.getInstance().getLocale());
        iterator.setText(s);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            words.add(s.substring(start, end));
        }

        Font font = Minecraft.getInstance().font;
        List<String> lines = new LinkedList<>();
        StringBuilder currentLine = new StringBuilder();
        int width = 0;
        for (String word : words) {
            int newWidth = font.width(word.replace("_", ""));
            if (width + newWidth > MAX_WIDTH_PER_LINE) {
                if (width > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    width = 0;
                } else {
                    lines.add(word);
                    continue;
                }
            }
            currentLine.append(word);
            width += newWidth;
        }
        if (width > 0) {
            lines.add(currentLine.toString());
        }

        MutableComponent indentComponent = Component.literal(" ".repeat(indent));
        indentComponent.withStyle(primary);

        List<Component> formattedLines = new ArrayList<>(lines.size());
        boolean highlightOn = false;
        for (String line : lines) {
            MutableComponent lineComponent = indentComponent.plainCopy();
            for (String part : line.split("_", -1)) {
                lineComponent.append(Component.literal(part).withStyle(highlightOn ? highlight : primary));
                highlightOn = !highlightOn;
            }
            formattedLines.add(lineComponent);
            highlightOn = !highlightOn;
        }
        return formattedLines;
    }
}
