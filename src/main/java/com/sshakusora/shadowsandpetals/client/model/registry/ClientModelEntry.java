package com.sshakusora.shadowsandpetals.client.model.registry;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Set;

interface ClientModelEntry {
    void registerModels(ModelEvent.RegisterStandalone event, Set<Identifier> registeredIds);

    void cacheModels(ModelEvent.BakingCompleted event);
}
