package com.sshakusora.shadowsandpetals.worldgen.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record PrefabTreeConfiguration(
        List<Identifier> templates,
        boolean allowRotation,
        boolean allowMirror,
        int trunkBaseExtensionMax,
        boolean updateLeafDistance,
        int leafCoreRadius,
        float leafSurfaceErosion,
        float leafNoiseScale
) implements FeatureConfiguration {
    public static final Codec<PrefabTreeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.listOf().fieldOf("templates").forGetter(PrefabTreeConfiguration::templates),
            Codec.BOOL.optionalFieldOf("allow_rotation", true).forGetter(PrefabTreeConfiguration::allowRotation),
            Codec.BOOL.optionalFieldOf("allow_mirror", true).forGetter(PrefabTreeConfiguration::allowMirror),
            Codec.intRange(0, 8).optionalFieldOf("trunk_base_extension_max", 1).forGetter(PrefabTreeConfiguration::trunkBaseExtensionMax),
            Codec.BOOL.optionalFieldOf("update_leaf_distance", true).forGetter(PrefabTreeConfiguration::updateLeafDistance),
            Codec.intRange(0, 6).optionalFieldOf("leaf_core_radius", 1).forGetter(PrefabTreeConfiguration::leafCoreRadius),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("leaf_surface_erosion", 0.1F).forGetter(PrefabTreeConfiguration::leafSurfaceErosion),
            Codec.floatRange(0.05F, 1.0F).optionalFieldOf("leaf_noise_scale", 0.3F).forGetter(PrefabTreeConfiguration::leafNoiseScale)
    ).apply(instance, PrefabTreeConfiguration::new));
}
