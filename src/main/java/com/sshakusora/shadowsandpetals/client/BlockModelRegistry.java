package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class BlockModelRegistry {
    private static final Map<WoodBlockList.WoodType, StandaloneModelKey<BlockStateModel>> VANITY_DRAWER_MODEL_KEYS = createVanityDrawerModelKeys();
    private static final Map<WoodBlockList.WoodType, BlockStateModel> VANITY_DRAWER_MODELS = new EnumMap<>(WoodBlockList.WoodType.class);

    private BlockModelRegistry() {
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        registerVanityModels(event);
    }

    public static void cacheBakedModels(ModelEvent.BakingCompleted event) {
        cacheVanityModels(event);
    }

    public static @Nullable BlockStateModel getVanityDrawerModel(Block vanityBlock) {
        WoodBlockList.WoodType woodType = vanityWoodTypeFor(vanityBlock);
        BlockStateModel cachedModel = VANITY_DRAWER_MODELS.get(woodType);
        if (cachedModel != null) {
            return cachedModel;
        }

        return Minecraft.getInstance()
                .getModelManager()
                .getStandaloneModel(VANITY_DRAWER_MODEL_KEYS.get(woodType));
    }

    private static void registerVanityModels(ModelEvent.RegisterStandalone event) {
        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            event.register(
                    VANITY_DRAWER_MODEL_KEYS.get(woodType),
                    SimpleUnbakedStandaloneModel.blockStateModel(ShadowsAndPetals.asResource("block/vanity/" + woodType.getName() + "_drawer"))
            );
        }
    }

    private static void cacheVanityModels(ModelEvent.BakingCompleted event) {
        VANITY_DRAWER_MODELS.clear();
        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            BlockStateModel model = event.getModelManager().getStandaloneModel(VANITY_DRAWER_MODEL_KEYS.get(woodType));
            if (model != null) {
                VANITY_DRAWER_MODELS.put(woodType, model);
            }
        }
    }

    private static Map<WoodBlockList.WoodType, StandaloneModelKey<BlockStateModel>> createVanityDrawerModelKeys() {
        Map<WoodBlockList.WoodType, StandaloneModelKey<BlockStateModel>> modelKeys = new EnumMap<>(WoodBlockList.WoodType.class);
        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            Identifier id = ShadowsAndPetals.asResource("vanity_drawer/" + woodType.getName());
            modelKeys.put(woodType, new StandaloneModelKey<>(id::toString));
        }
        return modelKeys;
    }

    private static WoodBlockList.WoodType vanityWoodTypeFor(Block vanityBlock) {
        String path = BuiltInRegistries.BLOCK.getKey(vanityBlock).getPath();
        String woodName = path.endsWith("_vanity") ? path.substring(0, path.length() - "_vanity".length()) : "oak";

        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            if (woodType.getName().equals(woodName)) {
                return woodType;
            }
        }

        return WoodBlockList.WoodType.OAK;
    }
}
