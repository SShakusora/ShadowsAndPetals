package com.sshakusora.shadowsandpetals.api.excavation;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class SandExcavationDataMaps {
    public static final DataMapType<Item, SandExcavationDropData> DROPS = DataMapType.builder(
            ShadowsAndPetals.asResource("sand_excavation_drops"),
            Registries.ITEM,
            SandExcavationDropData.CODEC
    ).build();

    private SandExcavationDataMaps() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SandExcavationDataMaps::registerDataMapTypes);
    }

    private static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(DROPS);
    }
}
