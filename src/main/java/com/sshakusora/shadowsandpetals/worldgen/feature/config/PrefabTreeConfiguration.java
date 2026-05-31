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
        boolean updateLeafDistance
) implements FeatureConfiguration {
    public static final Codec<PrefabTreeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.listOf().fieldOf("templates").forGetter(PrefabTreeConfiguration::templates),
            Codec.BOOL.optionalFieldOf("allow_rotation", true).forGetter(PrefabTreeConfiguration::allowRotation),
            Codec.BOOL.optionalFieldOf("allow_mirror", true).forGetter(PrefabTreeConfiguration::allowMirror),
            Codec.intRange(0, 8).optionalFieldOf("trunk_base_extension_max", 1).forGetter(PrefabTreeConfiguration::trunkBaseExtensionMax),
            Codec.BOOL.optionalFieldOf("update_leaf_distance", true).forGetter(PrefabTreeConfiguration::updateLeafDistance)
    ).apply(instance, PrefabTreeConfiguration::new));
}
