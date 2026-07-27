package com.sshakusora.shadowsandpetals.tooltip;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A composable modifier pipeline for item tooltips.
 * <p>
 * Each registered modifier receives the {@link ItemTooltipEvent} and may append,
 * insert, or rearrange the tooltip lines. Modifiers can be chained via
 * {@link #andThen(TooltipModifier)}.
 * <p>
 * Registration is keyed by item registry id ({@link Identifier}). Use
 * {@link #register(Identifier, TooltipModifier)} to attach modifiers;
 * existing entries are merged with {@code andThen}.
 */
@FunctionalInterface
public interface TooltipModifier {
    Map<Identifier, TooltipModifier> REGISTRY = new ConcurrentHashMap<>();

    void modify(ItemTooltipEvent event);

    default TooltipModifier andThen(TooltipModifier after) {
        return event -> {
            modify(event);
            after.modify(event);
        };
    }

    static void register(Identifier itemId, TooltipModifier modifier) {
        REGISTRY.merge(itemId, modifier, TooltipModifier::andThen);
    }

    static void applyIfPresent(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        TooltipModifier modifier = REGISTRY.get(id);
        if (modifier != null) {
            modifier.modify(event);
        }
    }
}
