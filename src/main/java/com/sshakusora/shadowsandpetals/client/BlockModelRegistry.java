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
    private static final Map<IroriFirewoodModel, StandaloneModelKey<BlockStateModel>> IRORI_FIREWOOD_MODEL_KEYS = createIroriFirewoodModelKeys();
    private static final Map<IroriFirewoodModel, BlockStateModel> IRORI_FIREWOOD_MODELS = new EnumMap<>(IroriFirewoodModel.class);

    private BlockModelRegistry() {
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        registerVanityModels(event);
        registerIroriFirewoodModels(event);
    }

    public static void cacheBakedModels(ModelEvent.BakingCompleted event) {
        cacheVanityModels(event);
        cacheIroriFirewoodModels(event);
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

    public static @Nullable BlockStateModel getIroriFirewoodModel(IroriFirewoodModel model) {
        BlockStateModel cachedModel = IRORI_FIREWOOD_MODELS.get(model);
        if (cachedModel != null) {
            return cachedModel;
        }

        return Minecraft.getInstance()
                .getModelManager()
                .getStandaloneModel(IRORI_FIREWOOD_MODEL_KEYS.get(model));
    }

    private static void registerVanityModels(ModelEvent.RegisterStandalone event) {
        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            event.register(
                    VANITY_DRAWER_MODEL_KEYS.get(woodType),
                    SimpleUnbakedStandaloneModel.blockStateModel(ShadowsAndPetals.asResource("block/vanity/" + woodType.getName() + "_drawer"))
            );
        }
    }

    private static void registerIroriFirewoodModels(ModelEvent.RegisterStandalone event) {
        for (IroriFirewoodModel model : IroriFirewoodModel.values()) {
            event.register(
                    IRORI_FIREWOOD_MODEL_KEYS.get(model),
                    SimpleUnbakedStandaloneModel.blockStateModel(ShadowsAndPetals.asResource("block/irori/firewood/" + model.modelName()))
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

    private static void cacheIroriFirewoodModels(ModelEvent.BakingCompleted event) {
        IRORI_FIREWOOD_MODELS.clear();
        for (IroriFirewoodModel model : IroriFirewoodModel.values()) {
            BlockStateModel bakedModel = event.getModelManager().getStandaloneModel(IRORI_FIREWOOD_MODEL_KEYS.get(model));
            if (bakedModel != null) {
                IRORI_FIREWOOD_MODELS.put(model, bakedModel);
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

    private static Map<IroriFirewoodModel, StandaloneModelKey<BlockStateModel>> createIroriFirewoodModelKeys() {
        Map<IroriFirewoodModel, StandaloneModelKey<BlockStateModel>> modelKeys = new EnumMap<>(IroriFirewoodModel.class);
        for (IroriFirewoodModel model : IroriFirewoodModel.values()) {
            Identifier id = ShadowsAndPetals.asResource("irori_firewood/" + model.modelName());
            modelKeys.put(model, new StandaloneModelKey<>(id::toString));
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

    public enum IroriFirewoodModel {
        UNLIT_7(false, 7),
        UNLIT_8(false, 8),
        UNLIT_9(false, 9),
        LIT_7(true, 7),
        LIT_8(true, 8),
        LIT_9(true, 9);

        private final boolean lit;
        private final int length;

        IroriFirewoodModel(boolean lit, int length) {
            this.lit = lit;
            this.length = length;
        }

        public boolean lit() {
            return this.lit;
        }

        public int length() {
            return this.length;
        }

        public String modelName() {
            return (this.lit ? "lit" : "unlit") + "_3_2_" + this.length;
        }

        public static IroriFirewoodModel byLitAndLength(boolean lit, int length) {
            for (IroriFirewoodModel model : values()) {
                if (model.lit == lit && model.length == length) {
                    return model;
                }
            }
            return lit ? LIT_7 : UNLIT_7;
        }
    }
}
