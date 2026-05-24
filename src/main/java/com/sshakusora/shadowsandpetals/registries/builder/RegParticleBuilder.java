package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fluent builder for {@link ParticleType} registration.
 * <p>
 * This builder supports both plain {@link SimpleParticleType} entries and custom particle-type
 * implementations, while also handling optional registry aliases for compatibility.
 *
 * @param <P> registered particle type
 */
public class RegParticleBuilder<P extends ParticleType<?>> {
    private final DeferredRegister<ParticleType<?>> registry;
    private final String name;
    private Supplier<P> particleFactory;
    private boolean overrideLimiter;
    private final List<Identifier> aliases = new ArrayList<>();

    public RegParticleBuilder(DeferredRegister<ParticleType<?>> registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    /**
     * Registers a custom particle type supplier.
     */
    public RegParticleBuilder<P> particle(Supplier<P> factory) {
        this.particleFactory = factory;
        return this;
    }

    /**
     * Configures this builder to create a {@link SimpleParticleType}.
     * <p>
     * When {@code overrideLimiter} is {@code true}, the particle ignores the client's reduced
     * particle setting in the same way as vanilla "always show" particles.
     */
    @SuppressWarnings("unchecked")
    public RegParticleBuilder<SimpleParticleType> simple(boolean overrideLimiter) {
        this.overrideLimiter = overrideLimiter;
        this.particleFactory = () -> (P) new SimpleParticleType(overrideLimiter);
        return (RegParticleBuilder<SimpleParticleType>) this;
    }

    /**
     * Configures this builder to create a limiter-respecting {@link SimpleParticleType}.
     */
    public RegParticleBuilder<SimpleParticleType> simple() {
        return simple(false);
    }

    /**
     * Adds a same-namespace registry alias for this particle type.
     */
    public RegParticleBuilder<P> alias(String oldPath) {
        this.aliases.add(ShadowsAndPetals.asResource(oldPath));
        return this;
    }

    /**
     * Adds a cross-namespace registry alias for this particle type.
     */
    public RegParticleBuilder<P> alias(String oldNamespace, String oldPath) {
        this.aliases.add(Identifier.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    /**
     * Finalizes particle-type registration and applies aliases.
     */
    public DeferredHolder<ParticleType<?>, P> register() {
        if (particleFactory == null) {
            throw new IllegalStateException("Particle factory is required for '" + name + "'");
        }

        DeferredHolder<ParticleType<?>, P> particle = registry.register(name, key -> particleFactory.get());
        for (Identifier alias : aliases) {
            registry.addAlias(alias, particle.getId());
        }
        return particle;
    }
}
