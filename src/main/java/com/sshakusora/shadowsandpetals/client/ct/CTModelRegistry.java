package com.sshakusora.shadowsandpetals.client.ct;

import com.sshakusora.shadowsandpetals.client.ct.CTRegistry.CTEntry;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Hooks into {@link ModelEvent.ModifyBakingResult} to replace the standard
 * {@link BlockStateModel} of
 * connected-texture blocks with {@link CTBlockStateModel}.
 * <p>
 * Call {@link #wrapModels(ModelEvent.ModifyBakingResult)} from the client
 * event listener (e.g. {@code ClientRenderEvents}).
 */
public final class CTModelRegistry {

    private CTModelRegistry() {}

    /**
     * Iterates every baked block-state model and wraps those whose block has a
     * {@link CTEntry} registered via {@link CTRegistry}.
     */
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        event.getBakingResult().blockStateModels().replaceAll((state, model) -> {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            CTEntry entry = CTRegistry.entries().get(blockId);
            if (entry != null) {
                return new CTBlockStateModel(model, entry);
            }
            return model;
        });
    }
}
