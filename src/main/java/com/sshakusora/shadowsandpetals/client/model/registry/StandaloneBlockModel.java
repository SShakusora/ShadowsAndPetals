package com.sshakusora.shadowsandpetals.client.model.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Handle for one standalone block-state model and its resource-reload-aware baked cache.
 */
public final class StandaloneBlockModel implements ClientModelEntry {
    private final Identifier id;
    private final Identifier modelId;
    private final ModelState modelState;
    private final StandaloneModelKey<BlockStateModel> key;
    /** Published after model baking so chunk-meshing workers can read it safely. */
    private volatile @Nullable BlockStateModel cachedModel;

    public StandaloneBlockModel(Identifier id, Identifier modelId, ModelState modelState) {
        this.id = id;
        this.modelId = modelId;
        this.modelState = modelState;
        this.key = new StandaloneModelKey<>(id::toString);
    }

    public Identifier id() {
        return id;
    }

    public @Nullable BlockStateModel get() {
        if (cachedModel != null) {
            return cachedModel;
        }
        return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
    }

    @Override
    public void registerModels(ModelEvent.RegisterStandalone event, Set<Identifier> registeredIds) {
        if (!registeredIds.add(id)) {
            throw new IllegalStateException("Duplicate standalone model id: " + id);
        }
        event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(modelId, modelState));
    }

    @Override
    public void cacheModels(ModelEvent.BakingCompleted event) {
        cachedModel = event.getModelManager().getStandaloneModel(key);
    }
}
