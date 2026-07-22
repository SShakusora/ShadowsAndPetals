package com.sshakusora.shadowsandpetals.client.model;

import com.mojang.math.Quadrant;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class BlockModelRegistry {
    private static final Map<IroriFuelState.FirewoodModel, StandaloneModelKey<BlockStateModel>> IRORI_FIREWOOD_MODEL_KEYS = createIroriFirewoodModelKeys();
    private static final Map<IroriFuelState.FirewoodModel, BlockStateModel> IRORI_FIREWOOD_MODELS = new EnumMap<>(IroriFuelState.FirewoodModel.class);
    private static final Map<IroriBlockEntity.GrillModel, StandaloneModelKey<BlockStateModel>> IRORI_GRILL_MODEL_KEYS = createIroriGrillModelKeys();
    private static final Map<IroriBlockEntity.GrillModel, BlockStateModel> IRORI_GRILL_MODELS = new EnumMap<>(IroriBlockEntity.GrillModel.class);
    private static final Map<WoodBlockList.WoodType, StandaloneModelKey<BlockStateModel>> VANITY_DRAWER_MODEL_KEYS = createVanityDrawerModelKeys();
    private static final Map<WoodBlockList.WoodType, BlockStateModel> VANITY_DRAWER_MODELS = new EnumMap<>(WoodBlockList.WoodType.class);
    private static final Map<WoodPostChainModelKey, StandaloneModelKey<BlockStateModel>> WOOD_POST_CHAIN_MODEL_KEYS = new HashMap<>();
    private static final Map<WoodPostChainModelKey, BlockStateModel> WOOD_POST_CHAIN_MODELS = new HashMap<>();
    private static final Map<WoodPostLinkModelKey, StandaloneModelKey<BlockStateModel>> WOOD_POST_LINK_MODEL_KEYS = new HashMap<>();
    private static final Map<WoodPostLinkModelKey, BlockStateModel> WOOD_POST_LINK_MODELS = new HashMap<>();
    private static final StandaloneModelKey<BlockStateModel> SHISHI_ODOSHI_MAIN_KEY = new StandaloneModelKey<>(
            () -> ShadowsAndPetals.asResource("shishi_odoshi_main").toString());
    private static BlockStateModel shishiOdoshiMainModel;
    private static final Map<DyeColor, StandaloneModelKey<BlockStateModel>> WIND_CHIME_BODY_KEYS = createWindChimeBodyKeys();
    private static final Map<DyeColor, StandaloneModelKey<BlockStateModel>> WIND_CHIME_MAIN_RIBBON_KEYS = createWindChimeMainRibbonKeys();
    private static final Map<DyeColor, StandaloneModelKey<BlockStateModel>> WIND_CHIME_VANE_KEYS = createWindChimeVaneKeys();
    private static final Map<DyeColor, BlockStateModel> WIND_CHIME_BODY_MODELS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, BlockStateModel> WIND_CHIME_MAIN_RIBBON_MODELS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, BlockStateModel> WIND_CHIME_VANE_MODELS = new EnumMap<>(DyeColor.class);
    private static final StandaloneModelKey<BlockStateModel> COPPER_TEAPOT_LID_KEY = new StandaloneModelKey<>(
            () -> ShadowsAndPetals.asResource("copper_teapot_lid").toString());
    private static BlockStateModel copperTeapotLidModel;

    private BlockModelRegistry() {
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        registerIroriFirewoodModels(event);
        registerVanityModels(event);
        registerWoodPostConnectionModels(event);
        registerShishiOdoshiModels(event);
        registerWindChimeModels(event);
        registerCopperTeapotModels(event);
    }

    public static void cacheBakedModels(ModelEvent.BakingCompleted event) {
        cacheIroriFirewoodModels(event);
        cacheVanityModels(event);
        cacheWoodPostConnectionModels(event);
        cacheShishiOdoshiModels(event);
        cacheWindChimeModels(event);
        cacheCopperTeapotModels(event);
    }

    public static void wrapBlockStateModels(ModelEvent.ModifyBakingResult event) {
        event.getBakingResult().blockStateModels().replaceAll((state, model) -> {
            if (state.getBlock() instanceof IroriBlock) {
                return new IroriBlockStateModel(model);
            }
            if (state.getBlock() instanceof WoodPostBlock woodPost) {
                return new WoodPostBlockStateModel(woodPost, model);
            }
            return model;
        });
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

    public static @Nullable BlockStateModel getIroriFirewoodModel(IroriFuelState.FirewoodModel firewoodModel) {
        BlockStateModel cachedModel = IRORI_FIREWOOD_MODELS.get(firewoodModel);
        if (cachedModel != null) {
            return cachedModel;
        }

        return Minecraft.getInstance()
                .getModelManager()
                .getStandaloneModel(IRORI_FIREWOOD_MODEL_KEYS.get(firewoodModel));
    }

    public static @Nullable BlockStateModel getIroriGrillModel(IroriBlockEntity.GrillModel grillModel) {
        BlockStateModel cachedModel = IRORI_GRILL_MODELS.get(grillModel);
        if (cachedModel != null) {
            return cachedModel;
        }

        return Minecraft.getInstance()
                .getModelManager()
                .getStandaloneModel(IRORI_GRILL_MODEL_KEYS.get(grillModel));
    }

    public static @Nullable BlockStateModel getWoodPostConnectionModel(Block block, WoodPostBlock.ConnectionType type, Direction direction) {
        if (type == WoodPostBlock.ConnectionType.OTHER_POST) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            WoodPostLinkModelKey key = new WoodPostLinkModelKey(blockId.getPath(), direction);
            BlockStateModel cachedModel = WOOD_POST_LINK_MODELS.get(key);
            if (cachedModel != null) {
                return cachedModel;
            }

            StandaloneModelKey<BlockStateModel> standaloneKey = WOOD_POST_LINK_MODEL_KEYS.get(key);
            return standaloneKey == null ? null : Minecraft.getInstance().getModelManager().getStandaloneModel(standaloneKey);
        }

        if (!type.isChain()) {
            return null;
        }

        WoodPostChainModelKey key = new WoodPostChainModelKey(type, direction);
        BlockStateModel cachedModel = WOOD_POST_CHAIN_MODELS.get(key);
        if (cachedModel != null) {
            return cachedModel;
        }

        StandaloneModelKey<BlockStateModel> standaloneKey = WOOD_POST_CHAIN_MODEL_KEYS.get(key);
        return standaloneKey == null ? null : Minecraft.getInstance().getModelManager().getStandaloneModel(standaloneKey);
    }

    private static void registerShishiOdoshiModels(ModelEvent.RegisterStandalone event) {
        event.register(
                SHISHI_ODOSHI_MAIN_KEY,
                SimpleUnbakedStandaloneModel.blockStateModel(ShadowsAndPetals.asResource("block/shishi_odoshi/main"))
        );
    }

    private static void cacheShishiOdoshiModels(ModelEvent.BakingCompleted event) {
        shishiOdoshiMainModel = event.getModelManager().getStandaloneModel(SHISHI_ODOSHI_MAIN_KEY);
    }

    public static @Nullable BlockStateModel getShishiOdoshiMainModel() {
        if (shishiOdoshiMainModel != null) {
            return shishiOdoshiMainModel;
        }
        return Minecraft.getInstance().getModelManager().getStandaloneModel(SHISHI_ODOSHI_MAIN_KEY);
    }

    public static @Nullable BlockStateModel getWindChimeBodyModel(DyeColor ribbon) {
        BlockStateModel cachedModel = WIND_CHIME_BODY_MODELS.get(ribbon);
        if (cachedModel != null) {
            return cachedModel;
        }
        return Minecraft.getInstance().getModelManager().getStandaloneModel(WIND_CHIME_BODY_KEYS.get(ribbon));
    }

    public static @Nullable BlockStateModel getWindChimeMainRibbonModel(DyeColor ribbon) {
        BlockStateModel cachedModel = WIND_CHIME_MAIN_RIBBON_MODELS.get(ribbon);
        if (cachedModel != null) {
            return cachedModel;
        }
        return Minecraft.getInstance().getModelManager().getStandaloneModel(WIND_CHIME_MAIN_RIBBON_KEYS.get(ribbon));
    }

    public static @Nullable BlockStateModel getWindChimeVaneModel(DyeColor vane) {
        BlockStateModel cachedModel = WIND_CHIME_VANE_MODELS.get(vane);
        if (cachedModel != null) {
            return cachedModel;
        }
        return Minecraft.getInstance().getModelManager().getStandaloneModel(WIND_CHIME_VANE_KEYS.get(vane));
    }

    public static @Nullable BlockStateModel getCopperTeapotLidModel() {
        if (copperTeapotLidModel != null) {
            return copperTeapotLidModel;
        }
        return Minecraft.getInstance().getModelManager().getStandaloneModel(COPPER_TEAPOT_LID_KEY);
    }

    private static void registerCopperTeapotModels(ModelEvent.RegisterStandalone event) {
        event.register(
                COPPER_TEAPOT_LID_KEY,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ShadowsAndPetals.asResource("block/teapot/copper/lid"))
        );
    }

    private static void cacheCopperTeapotModels(ModelEvent.BakingCompleted event) {
        copperTeapotLidModel = event.getModelManager().getStandaloneModel(COPPER_TEAPOT_LID_KEY);
    }

    private static void registerWindChimeModels(ModelEvent.RegisterStandalone event) {
        for (DyeColor ribbon : DyeColor.values()) {
            event.register(
                    WIND_CHIME_BODY_KEYS.get(ribbon),
                    SimpleUnbakedStandaloneModel.blockStateModel(WindChimeColors.blockBodyModelId(ribbon))
            );
        }
        for (DyeColor ribbon : DyeColor.values()) {
            event.register(
                    WIND_CHIME_MAIN_RIBBON_KEYS.get(ribbon),
                    SimpleUnbakedStandaloneModel.blockStateModel(WindChimeColors.blockMainRibbonModelId(ribbon))
            );
        }
        for (DyeColor vane : DyeColor.values()) {
            event.register(
                    WIND_CHIME_VANE_KEYS.get(vane),
                    SimpleUnbakedStandaloneModel.blockStateModel(WindChimeColors.blockVaneModelId(vane))
            );
        }
    }

    private static void cacheWindChimeModels(ModelEvent.BakingCompleted event) {
        WIND_CHIME_BODY_MODELS.clear();
        for (Map.Entry<DyeColor, StandaloneModelKey<BlockStateModel>> entry : WIND_CHIME_BODY_KEYS.entrySet()) {
            BlockStateModel model = event.getModelManager().getStandaloneModel(entry.getValue());
            if (model != null) {
                WIND_CHIME_BODY_MODELS.put(entry.getKey(), model);
            }
        }

        WIND_CHIME_MAIN_RIBBON_MODELS.clear();
        for (Map.Entry<DyeColor, StandaloneModelKey<BlockStateModel>> entry : WIND_CHIME_MAIN_RIBBON_KEYS.entrySet()) {
            BlockStateModel model = event.getModelManager().getStandaloneModel(entry.getValue());
            if (model != null) {
                WIND_CHIME_MAIN_RIBBON_MODELS.put(entry.getKey(), model);
            }
        }

        WIND_CHIME_VANE_MODELS.clear();
        for (Map.Entry<DyeColor, StandaloneModelKey<BlockStateModel>> entry : WIND_CHIME_VANE_KEYS.entrySet()) {
            BlockStateModel model = event.getModelManager().getStandaloneModel(entry.getValue());
            if (model != null) {
                WIND_CHIME_VANE_MODELS.put(entry.getKey(), model);
            }
        }
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
        for (IroriFuelState.FirewoodModel firewoodModel : IroriFuelState.FirewoodModel.values()) {
            event.register(
                    IRORI_FIREWOOD_MODEL_KEYS.get(firewoodModel),
                    SimpleUnbakedStandaloneModel.blockStateModel(ShadowsAndPetals.asResource("block/irori/firewood/" + firewoodModel.modelName()))
            );
        }
        for (IroriBlockEntity.GrillModel grillModel : IroriBlockEntity.GrillModel.values()) {
            event.register(
                    IRORI_GRILL_MODEL_KEYS.get(grillModel),
                    SimpleUnbakedStandaloneModel.blockStateModel(
                            ShadowsAndPetals.asResource("block/grill/" + grillModel.modelName()))
            );
        }
    }

    private static void registerWoodPostConnectionModels(ModelEvent.RegisterStandalone event) {
        WOOD_POST_CHAIN_MODEL_KEYS.clear();
        WOOD_POST_LINK_MODEL_KEYS.clear();

        for (WoodPostBlock.ConnectionType type : WoodPostBlock.ConnectionType.values()) {
            if (!type.isChain()) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                WoodPostChainModelKey key = new WoodPostChainModelKey(type, direction);
                StandaloneModelKey<BlockStateModel> standaloneKey = key.standaloneKey();
                event.register(standaloneKey, SimpleUnbakedStandaloneModel.blockStateModel(chainModelId(type, usesUpperModel(direction)), rotationState(direction)));
                WOOD_POST_CHAIN_MODEL_KEYS.put(key, standaloneKey);
            }
        }

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof WoodPostBlock)) {
                continue;
            }

            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            for (Direction direction : Direction.values()) {
                WoodPostLinkModelKey key = new WoodPostLinkModelKey(blockId.getPath(), direction);
                StandaloneModelKey<BlockStateModel> standaloneKey = key.standaloneKey();
                event.register(standaloneKey, SimpleUnbakedStandaloneModel.blockStateModel(linkModelId(blockId, usesUpperModel(direction)), rotationState(direction)));
                WOOD_POST_LINK_MODEL_KEYS.put(key, standaloneKey);
            }
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
        for (IroriFuelState.FirewoodModel firewoodModel : IroriFuelState.FirewoodModel.values()) {
            BlockStateModel model = event.getModelManager().getStandaloneModel(IRORI_FIREWOOD_MODEL_KEYS.get(firewoodModel));
            if (model != null) {
                IRORI_FIREWOOD_MODELS.put(firewoodModel, model);
            }
        }
        IRORI_GRILL_MODELS.clear();
        for (IroriBlockEntity.GrillModel grillModel : IroriBlockEntity.GrillModel.values()) {
            BlockStateModel model = event.getModelManager().getStandaloneModel(IRORI_GRILL_MODEL_KEYS.get(grillModel));
            if (model != null) {
                IRORI_GRILL_MODELS.put(grillModel, model);
            }
        }
    }

    private static void cacheWoodPostConnectionModels(ModelEvent.BakingCompleted event) {
        WOOD_POST_CHAIN_MODELS.clear();
        for (Map.Entry<WoodPostChainModelKey, StandaloneModelKey<BlockStateModel>> entry : WOOD_POST_CHAIN_MODEL_KEYS.entrySet()) {
            BlockStateModel bakedModel = event.getModelManager().getStandaloneModel(entry.getValue());
            if (bakedModel != null) {
                WOOD_POST_CHAIN_MODELS.put(entry.getKey(), bakedModel);
            }
        }

        WOOD_POST_LINK_MODELS.clear();
        for (Map.Entry<WoodPostLinkModelKey, StandaloneModelKey<BlockStateModel>> entry : WOOD_POST_LINK_MODEL_KEYS.entrySet()) {
            BlockStateModel bakedModel = event.getModelManager().getStandaloneModel(entry.getValue());
            if (bakedModel != null) {
                WOOD_POST_LINK_MODELS.put(entry.getKey(), bakedModel);
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

    private static Map<IroriFuelState.FirewoodModel, StandaloneModelKey<BlockStateModel>> createIroriFirewoodModelKeys() {
        Map<IroriFuelState.FirewoodModel, StandaloneModelKey<BlockStateModel>> modelKeys = new EnumMap<>(IroriFuelState.FirewoodModel.class);
        for (IroriFuelState.FirewoodModel firewoodModel : IroriFuelState.FirewoodModel.values()) {
            Identifier id = ShadowsAndPetals.asResource("irori_firewood/" + firewoodModel.modelName());
            modelKeys.put(firewoodModel, new StandaloneModelKey<>(id::toString));
        }
        return modelKeys;
    }

    private static Map<IroriBlockEntity.GrillModel, StandaloneModelKey<BlockStateModel>> createIroriGrillModelKeys() {
        Map<IroriBlockEntity.GrillModel, StandaloneModelKey<BlockStateModel>> modelKeys =
                new EnumMap<>(IroriBlockEntity.GrillModel.class);
        for (IroriBlockEntity.GrillModel grillModel : IroriBlockEntity.GrillModel.values()) {
            Identifier id = ShadowsAndPetals.asResource("irori_grill/" + grillModel.modelName());
            modelKeys.put(grillModel, new StandaloneModelKey<>(id::toString));
        }
        return modelKeys;
    }

    private static Map<DyeColor, StandaloneModelKey<BlockStateModel>> createWindChimeBodyKeys() {
        Map<DyeColor, StandaloneModelKey<BlockStateModel>> modelKeys = new EnumMap<>(DyeColor.class);
        for (DyeColor ribbon : DyeColor.values()) {
            Identifier id = ShadowsAndPetals.asResource("wind_chime_body/" + ribbon.getName());
            modelKeys.put(ribbon, new StandaloneModelKey<>(id::toString));
        }
        return modelKeys;
    }

    private static Map<DyeColor, StandaloneModelKey<BlockStateModel>> createWindChimeMainRibbonKeys() {
        Map<DyeColor, StandaloneModelKey<BlockStateModel>> modelKeys = new EnumMap<>(DyeColor.class);
        for (DyeColor ribbon : DyeColor.values()) {
            Identifier id = ShadowsAndPetals.asResource("wind_chime_main_ribbon/" + ribbon.getName());
            modelKeys.put(ribbon, new StandaloneModelKey<>(id::toString));
        }
        return modelKeys;
    }

    private static Map<DyeColor, StandaloneModelKey<BlockStateModel>> createWindChimeVaneKeys() {
        Map<DyeColor, StandaloneModelKey<BlockStateModel>> modelKeys = new EnumMap<>(DyeColor.class);
        for (DyeColor vane : DyeColor.values()) {
            Identifier id = ShadowsAndPetals.asResource("wind_chime_vane/" + vane.getName());
            modelKeys.put(vane, new StandaloneModelKey<>(id::toString));
        }
        return modelKeys;
    }

    private static Identifier chainModelId(WoodPostBlock.ConnectionType type, boolean upperHalf) {
        return ShadowsAndPetals.asResource("block/wood_post_" + type.getSerializedName() + (upperHalf ? "_link_top" : "_link"));
    }

    private static Identifier linkModelId(Identifier blockId, boolean upperHalf) {
        return ShadowsAndPetals.asResource("block/" + blockId.getPath() + (upperHalf ? "_link_top" : "_link"));
    }

    private static boolean usesUpperModel(Direction direction) {
        return switch (direction) {
            case UP, NORTH, EAST -> true;
            default -> false;
        };
    }

    private static ModelState rotationState(Direction direction) {
        return switch (direction) {
            case DOWN, UP -> BlockModelRotation.IDENTITY;
            case NORTH, SOUTH -> rotatedModelState(90, 0);
            case WEST, EAST -> rotatedModelState(90, 90);
        };
    }

    private static ModelState rotatedModelState(int xDegrees, int yDegrees) {
        return new Variant.SimpleModelState(quadrant(xDegrees), quadrant(yDegrees), Quadrant.R0, false).asModelState();
    }

    private static Quadrant quadrant(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> Quadrant.R0;
            case 90 -> Quadrant.R90;
            case 180 -> Quadrant.R180;
            case 270 -> Quadrant.R270;
            default -> throw new IllegalArgumentException("Unsupported rotation: " + degrees);
        };
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

    private record WoodPostChainModelKey(WoodPostBlock.ConnectionType type, Direction direction) {
        private StandaloneModelKey<BlockStateModel> standaloneKey() {
            Identifier id = ShadowsAndPetals.asResource("wood_post_chain/" + type.getSerializedName() + "/" + direction.getSerializedName());
            return new StandaloneModelKey<>(id::toString);
        }
    }

    private record WoodPostLinkModelKey(String blockName, Direction direction) {
        private StandaloneModelKey<BlockStateModel> standaloneKey() {
            Identifier id = ShadowsAndPetals.asResource("wood_post_link/" + blockName + "/" + direction.getSerializedName());
            return new StandaloneModelKey<>(id::toString);
        }
    }

}
