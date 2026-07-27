package com.sshakusora.shadowsandpetals.tooltip;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Registry for custom tooltip components keyed by item id.
 * Component creation stays independent of client rendering events, allowing
 * builders to register components alongside ordinary tooltip descriptions.
 */
public final class TooltipComponentRegistry {
    private static final Map<Identifier, List<Provider>> REGISTRY = new ConcurrentHashMap<>();

    private TooltipComponentRegistry() {}

    public static void register(
            Identifier itemId,
            Function<ItemStack, @Nullable TooltipComponent> factory,
            int minimumWidth
    ) {
        REGISTRY.computeIfAbsent(itemId, ignored -> new CopyOnWriteArrayList<>())
                .add(new Provider(factory, Math.max(0, minimumWidth)));
    }

    public static List<Entry> gather(ItemStack stack) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<Provider> providers = REGISTRY.get(itemId);
        if (providers == null || providers.isEmpty()) {
            return List.of();
        }

        List<Entry> entries = new ArrayList<>(providers.size());
        for (Provider provider : providers) {
            TooltipComponent component = provider.factory().apply(stack);
            if (component != null) {
                entries.add(new Entry(component, provider.minimumWidth()));
            }
        }
        return List.copyOf(entries);
    }

    public record Entry(TooltipComponent component, int minimumWidth) {}

    private record Provider(
            Function<ItemStack, @Nullable TooltipComponent> factory,
            int minimumWidth
    ) {}
}
