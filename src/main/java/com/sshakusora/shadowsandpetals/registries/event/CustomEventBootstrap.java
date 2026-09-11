package com.sshakusora.shadowsandpetals.registries.event;

import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.api.shishiOdoshi.RegisterShishiOdoshiFluidsEvent;
import com.sshakusora.shadowsandpetals.api.shishiOdoshi.ShishiOdoshiFluidRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/** Wires custom mod events that still require a bootstrap dispatch. */
public final class CustomEventBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CustomEventBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ShishiOdoshiFluidBehaviorRegistry::register);
        modEventBus.addListener(CustomEventBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
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