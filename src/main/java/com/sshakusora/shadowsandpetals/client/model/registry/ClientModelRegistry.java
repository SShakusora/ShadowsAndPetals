package com.sshakusora.shadowsandpetals.client.model.registry;

import com.sshakusora.shadowsandpetals.client.model.builder.RegStandaloneBlockModelBuilder;
import com.sshakusora.shadowsandpetals.client.model.builder.RegStandaloneBlockModelSetBuilder;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-only registry for standalone models declared through fluent builders.
 */
public final class ClientModelRegistry {
    private static final List<ClientModelEntry> ENTRIES = new ArrayList<>();

    private ClientModelRegistry() {
    }

    public static RegStandaloneBlockModelBuilder blockState(String name) {
        return new RegStandaloneBlockModelBuilder(name);
    }

    public static <K> RegStandaloneBlockModelSetBuilder<K> blockStateSet(String name) {
        return new RegStandaloneBlockModelSetBuilder<>(name);
    }

    public static <E extends Enum<E>> RegStandaloneBlockModelSetBuilder<E> enumBlockStateSet(
            String name,
            Class<E> enumType
    ) {
        return ClientModelRegistry.<E>blockStateSet(name).keys(() -> List.of(enumType.getEnumConstants()));
    }

    public static StandaloneBlockModel register(StandaloneBlockModel model) {
        ENTRIES.add(model);
        return model;
    }

    public static <K> StandaloneBlockModelSet<K> register(StandaloneBlockModelSet<K> models) {
        ENTRIES.add(models);
        return models;
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        Set<Identifier> registeredIds = new HashSet<>();
        for (ClientModelEntry entry : ENTRIES) {
            entry.registerModels(event, registeredIds);
        }
    }

    public static void cacheBakedModels(ModelEvent.BakingCompleted event) {
        for (ClientModelEntry entry : ENTRIES) {
            entry.cacheModels(event);
        }
    }
}
