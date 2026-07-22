package com.sshakusora.shadowsandpetals.client.model.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.model.registry.ClientModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.registry.StandaloneBlockModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Fluent builder for one standalone block-state model. */
public final class RegStandaloneBlockModelBuilder {
    private final String name;
    private Identifier modelId;
    private ModelState modelState = BlockModelRotation.IDENTITY;

    public RegStandaloneBlockModelBuilder(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public RegStandaloneBlockModelBuilder model(Identifier modelId) {
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        return this;
    }

    public RegStandaloneBlockModelBuilder rotation(ModelState modelState) {
        this.modelState = Objects.requireNonNull(modelState, "modelState");
        return this;
    }

    public StandaloneBlockModel register() {
        if (modelId == null) {
            throw new IllegalStateException("Standalone model resource is required for '" + name + "'");
        }
        return ClientModelRegistry.register(new StandaloneBlockModel(
                ShadowsAndPetals.asResource(name),
                modelId,
                modelState
        ));
    }
}
