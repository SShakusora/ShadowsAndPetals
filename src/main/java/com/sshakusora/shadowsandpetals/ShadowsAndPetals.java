package com.sshakusora.shadowsandpetals;

import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ShadowsAndPetals.MOD_ID)
public class ShadowsAndPetals {
    public static final String MOD_ID = "shadowsandpetals";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ShadowsAndPetals(IEventBus modEventBus) {
        SAPRegistries.register(modEventBus);

        ItemRegistry.init();
        BlockRegistry.init();
        BlockEntityRegistry.init();
        EntityRegistry.init();
        CreativeTabRegistry.init();
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
