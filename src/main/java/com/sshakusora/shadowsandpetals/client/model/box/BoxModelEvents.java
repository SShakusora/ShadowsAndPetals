package com.sshakusora.shadowsandpetals.client.model.box;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

/**
 * Installs the dynamic box wrapper around the placed box block variants.
 *
 * <p>Only block variants are wrapped: the {@code inventory} model is {@code builtin/entity}
 * and must keep going through {@code BoxItemRenderer} (BEWLR).</p>
 */
@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BoxModelEvents {
    private BoxModelEvents() {
    }

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ResourceLocation boxId = BlockRegistry.BOX.getId();
        Map<ModelResourceLocation, BakedModel> models = event.getModels();

        for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
            ModelResourceLocation key = entry.getKey();
            if (!key.id().equals(boxId) || key.variant().equals(ModelResourceLocation.INVENTORY_VARIANT)) {
                continue;
            }

            BakedModel model = entry.getValue();
            if (!(model instanceof BoxBakedModel)) {
                entry.setValue(new BoxBakedModel(model));
            }
        }
    }

    @SubscribeEvent
    public static void bakingCompleted(ModelEvent.BakingCompleted event) {
        // Baked quads and atlas sprites are replaced by the reload; never reuse the old ones.
        BoxContentQuadCache.invalidate();
    }
}
