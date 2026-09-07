package com.sshakusora.shadowsandpetals.item.barrel;

import com.sshakusora.shadowsandpetals.blockentity.WoodenBarrelBlockEntity;
import com.sshakusora.shadowsandpetals.client.tooltip.TooltipHelper;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.tooltip.TooltipModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * Adds a wooden barrel's stored-fluid capacity directly below the item name.
 *
 * <p>The line is intentionally text-only. The fluid name and amount are read from the
 * block-entity data carried by the dropped item, so the tooltip does not need a
 * custom client tooltip component or a fluid texture.</p>
 */
public final class WoodenBarrelTooltipModifier implements TooltipModifier {
    @Override
    public void modify(ItemTooltipEvent event) {
        FluidStack fluid = WoodenBarrelItemFluid.read(event.getItemStack()).orElse(null);
        if (fluid == null) {
            return;
        }

        MutableComponent fluidName = fluid.getHoverName()
                .copy()
                .withStyle(TooltipHelper.HIGHLIGHT_STYLE);
        MutableComponent capacity = Component.literal(
                        fluid.getAmount() + " mB/" + WoodenBarrelBlockEntity.FLUID_CAPACITY + " mB"
                )
                .withStyle(TooltipHelper.PRIMARY_STYLE);
        event.getToolTip().addAll(1, List.of(
                Component.translatable(BuiltinLanguageKeys.WOODEN_BARREL_FLUID_HEADER.key())
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable(BuiltinLanguageKeys.WOODEN_BARREL_FLUID.key(), fluidName, capacity)
                        .withStyle(TooltipHelper.PRIMARY_STYLE),
                CommonComponents.EMPTY
        ));
    }

}
