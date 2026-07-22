package com.sshakusora.shadowsandpetals.client.model.registry;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lazily materialized family of standalone block-state models sharing one id prefix.
 */
public final class StandaloneBlockModelSet<K> implements ClientModelEntry {
    private final String idPrefix;
    private final Supplier<? extends Iterable<K>> keys;
    private final Function<? super K, String> keyPathFactory;
    private final Function<? super K, Identifier> modelFactory;
    private final Function<? super K, ModelState> modelStateFactory;
    private final Map<K, StandaloneBlockModel> models = new HashMap<>();

    public StandaloneBlockModelSet(
            String idPrefix,
            Supplier<? extends Iterable<K>> keys,
            Function<? super K, String> keyPathFactory,
            Function<? super K, Identifier> modelFactory,
            Function<? super K, ModelState> modelStateFactory
    ) {
        this.idPrefix = idPrefix;
        this.keys = keys;
        this.keyPathFactory = keyPathFactory;
        this.modelFactory = modelFactory;
        this.modelStateFactory = modelStateFactory;
    }

    public @Nullable BlockStateModel get(K key) {
        StandaloneBlockModel model = models.get(key);
        return model == null ? null : model.get();
    }

    @Override
    public void registerModels(ModelEvent.RegisterStandalone event, Set<Identifier> registeredIds) {
        models.clear();
        for (K key : keys.get()) {
            String keyPath = keyPathFactory.apply(key);
            Identifier id = ShadowsAndPetals.asResource(idPrefix + "/" + keyPath);
            StandaloneBlockModel model = new StandaloneBlockModel(
                    id,
                    modelFactory.apply(key),
                    modelStateFactory.apply(key)
            );
            if (models.putIfAbsent(key, model) != null) {
                throw new IllegalStateException("Duplicate standalone model-set key in '" + idPrefix + "': " + key);
            }
            model.registerModels(event, registeredIds);
        }
    }

    @Override
    public void cacheModels(ModelEvent.BakingCompleted event) {
        for (StandaloneBlockModel model : models.values()) {
            model.cacheModels(event);
        }
    }
}
