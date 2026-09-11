package com.sshakusora.shadowsandpetals.client.tooltip;
import com.sshakusora.shadowsandpetals.tooltip.TooltipModifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import java.util.function.Supplier;
public final class ItemDescription {
 private ItemDescription() {}
 public static final class Modifier implements TooltipModifier {
   private final Supplier<Item> item;
   public Modifier(Supplier<Item> item){this.item=item;}
   @Override public void modify(ItemTooltipEvent event) {}
 }
}