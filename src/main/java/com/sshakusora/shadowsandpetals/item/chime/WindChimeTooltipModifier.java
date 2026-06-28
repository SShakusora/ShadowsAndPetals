package com.sshakusora.shadowsandpetals.item.chime;

import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipHelper;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public class WindChimeTooltipModifier implements TooltipModifier {
    @Override
    public void modify(ItemTooltipEvent event) {
        if (!Minecraft.getInstance().hasShiftDown()) {
            return;
        }
        WindChimeColors colors = WindChimeColors.fromStack(event.getItemStack());
        event.getToolTip().addAll(1, List.of(
                CommonComponents.EMPTY,
                Component.translatable(BuiltinLanguageKeys.WIND_CHIME_COLORS.key())
                        .withStyle(ChatFormatting.GRAY),
                colorLine(BuiltinLanguageKeys.WIND_CHIME_RIBBON_COLOR.key(), colors.ribbon()),
                colorLine(BuiltinLanguageKeys.WIND_CHIME_VANE_COLOR.key(), colors.vane())
        ));
    }

    private static Component colorLine(String labelKey, DyeColor color) {
        MutableComponent colorName = Component.translatable("color.minecraft." + color.getName())
                .withStyle(TooltipHelper.HIGHLIGHT_STYLE);
        return Component.literal(" ")
                .withStyle(TooltipHelper.PRIMARY_STYLE)
                .append(Component.translatable(labelKey, colorName)
                        .withStyle(TooltipHelper.PRIMARY_STYLE));
    }
}
