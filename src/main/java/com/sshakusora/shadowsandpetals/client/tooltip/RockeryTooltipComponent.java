package com.sshakusora.shadowsandpetals.client.tooltip;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Server-safe tooltip component carrying rockery block model preview data.
 * The actual rendering is handled by {@code ClientRockeryTooltip} on the client.
 */
public record RockeryTooltipComponent(RockeryBlock block, RockeryDimensions dimensions) implements TooltipComponent {
}
