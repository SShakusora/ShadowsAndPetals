package com.sshakusora.shadowsandpetals.worldgen;

import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import com.sshakusora.shadowsandpetals.worldgen.feature.PrefabTreeFeature;
import com.sshakusora.shadowsandpetals.worldgen.feature.config.PrefabTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class SAPFeatures {
    public static final DeferredHolder<Feature<?>, Feature<PrefabTreeConfiguration>> PREFAB_TREE = SAPRegistries.FEATURES.register(
            "prefab_tree",
            () -> new PrefabTreeFeature(PrefabTreeConfiguration.CODEC)
    );

    private SAPFeatures() {
    }

    public static void init() {
    }
}
