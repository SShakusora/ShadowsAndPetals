package com.sshakusora.shadowsandpetals.client.tooltip;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
public record RockeryTooltipComponent(RockeryBlock block, RockeryDimensions dimensions) implements TooltipComponent {}