package com.sshakusora.shadowsandpetals;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

@Mod(ShadowsAndPetals.MOD_ID)
public class ShadowsAndPetals {
    public static final String MOD_ID = "shadowsandpetals";

    public ShadowsAndPetals() {
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
