package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Mod-bus event used to register Irori behavior extensions.
 *
 * <p>The event is dispatched after common setup listeners have completed. Subscribe on the mod
 * event bus and register common-side rules only.
 */
public final class RegisterIroriBehaviorsEvent extends Event implements IModBusEvent {
    public RegisterIroriBehaviorsEvent() {
    }

    public void registerGrillRule(Identifier id, IroriGrillRule rule) {
        IroriApi.registerGrillRule(id, rule);
    }

    public void registerFuelRule(Identifier id, IroriFuelRule rule) {
        IroriApi.registerFuelRule(id, rule);
    }

    public void registerFuelRule(Identifier id, int priority, IroriFuelRule rule) {
        IroriApi.registerFuelRule(id, priority, rule);
    }

    public void registerIgnitionBehavior(Identifier id, IroriIgnitionBehavior behavior) {
        IroriApi.registerIgnitionBehavior(id, behavior);
    }

    public void registerIgnitionBehavior(Identifier id, int priority, IroriIgnitionBehavior behavior) {
        IroriApi.registerIgnitionBehavior(id, priority, behavior);
    }

    public void registerAshDropProvider(Identifier id, IroriAshDropProvider provider) {
        IroriApi.registerAshDropProvider(id, provider);
    }
}
