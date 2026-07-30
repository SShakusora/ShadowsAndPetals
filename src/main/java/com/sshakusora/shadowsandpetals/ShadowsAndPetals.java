package com.sshakusora.shadowsandpetals;

import com.sshakusora.shadowsandpetals.registries.*;
import com.sshakusora.shadowsandpetals.registries.event.CustomEventBootstrap;
import com.sshakusora.shadowsandpetals.worldgen.SAPFeatures;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ShadowsAndPetals.MOD_ID)
public class ShadowsAndPetals {
    public static final String MOD_ID = "shadowsandpetals";

    public ShadowsAndPetals(IEventBus modEventBus) {
        SAPRegistries.register(modEventBus);
        CustomEventBootstrap.register(modEventBus);

        FluidRegistry.init();
        AttachmentRegistry.init();
        ItemRegistry.init();
        BlockRegistry.init();
        BlockEntityRegistry.init();
        MenuRegistry.init();
        EntityRegistry.init();
        ParticleRegistry.init();
        SoundRegistry.init();
        RecipeSerializerRegistry.init();
        CreativeTabRegistry.init();
        SAPFeatures.init();
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
