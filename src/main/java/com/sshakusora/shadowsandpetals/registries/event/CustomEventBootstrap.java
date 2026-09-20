package com.sshakusora.shadowsandpetals.registries.event;

import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.api.shishiOdoshi.RegisterShishiOdoshiFluidsEvent;
import com.sshakusora.shadowsandpetals.api.shishiOdoshi.ShishiOdoshiFluidRegistry;
import com.sshakusora.shadowsandpetals.compat.CompatManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Wires custom mod events that still require a bootstrap dispatch.
 */
public final class CustomEventBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CREATE_SCHEMATIC_COMPAT_CLASS =
            "com.sshakusora.shadowsandpetals.compat.create.schematic.Bootstrap";

    private CustomEventBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ShishiOdoshiFluidBehaviorRegistry::register);
        modEventBus.addListener(CustomEventBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModLoader.postEvent(new RegisterShishiOdoshiFluidsEvent());
            registerCreateSchematicCompat();
            LOGGER.debug(
                    "Registered shishi-odoshi fluids: sources={}, animationSpeeds={}, renderProperties={}",
                    ShishiOdoshiFluidRegistry.registeredSourceCount(),
                    ShishiOdoshiFluidRegistry.registeredAnimationSpeedCount(),
                    ShishiOdoshiFluidRegistry.registeredRenderPropertiesCount()
            );
        });
    }

    private static void registerCreateSchematicCompat() {
        if (!CompatManager.isCreateLoaded()) {
            return;
        }

        try {
            Class<?> compatClass = Class.forName(CREATE_SCHEMATIC_COMPAT_CLASS);
            compatClass.getMethod("register").invoke(null);
            LOGGER.debug("Registered Create schematic compatibility");
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Failed to register Create schematic compatibility", exception);
        }
    }
}
