package com.sshakusora.shadowsandpetals.registries.event;

import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.api.irori.IroriApi;
import com.sshakusora.shadowsandpetals.api.irori.RegisterIroriBehaviorsEvent;
import com.sshakusora.shadowsandpetals.api.shishiOdoshi.RegisterShishiOdoshiFluidsEvent;
import com.sshakusora.shadowsandpetals.api.shishiOdoshi.ShishiOdoshiFluidRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Wires built-in behavior listeners and dispatches the public behavior registration events.
 */
public final class BehaviorEventBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BehaviorEventBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(IroriBehaviorRegistry::register);
        modEventBus.addListener(ShishiOdoshiFluidBehaviorRegistry::register);
        modEventBus.addListener(BehaviorEventBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModLoader.postEvent(new RegisterIroriBehaviorsEvent());
            LOGGER.debug(
                    "Registered Irori behaviors: grill={}, fuel={}, ignition={}, ashDrops={}",
                    IroriApi.registeredGrillRuleIds(),
                    IroriApi.registeredFuelRuleIds(),
                    IroriApi.registeredIgnitionBehaviorIds(),
                    IroriApi.registeredAshDropProviderIds()
            );

            ModLoader.postEvent(new RegisterShishiOdoshiFluidsEvent());
            LOGGER.debug(
                    "Registered shishi-odoshi fluids: sources={}, animationSpeeds={}, renderProperties={}",
                    ShishiOdoshiFluidRegistry.registeredSourceCount(),
                    ShishiOdoshiFluidRegistry.registeredAnimationSpeedCount(),
                    ShishiOdoshiFluidRegistry.registeredRenderPropertiesCount()
            );
        });
    }
}
