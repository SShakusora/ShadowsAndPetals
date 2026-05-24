package com.sshakusora.shadowsandpetals.registries;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ParticleRegistry {
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GINKGO = SAPRegistries
            .particle("ginkgo")
            .register();

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MAPLE = SAPRegistries
            .particle("maple")
            .register();

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SAKURA = SAPRegistries
            .particle("sakura")
            .register();

    public static void init() {}
}
