package com.sshakusora.shadowsandpetals.client;

import com.mojang.math.Quadrant;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class BlockModelRegistry {
    private static final Map<WoodBlockList.WoodType, StandaloneModelKey<BlockStateModel>> VANITY_DRAWER_MODEL_KEYS = createVanityDrawerModelKeys();
    private static final Map<WoodBlockList.WoodType, BlockStateModel> VANITY_DRAWER_MODELS = new EnumMap<>(WoodBlockList.WoodType.class);
    private static final Map<IroriFirewoodModel, StandaloneModelKey<BlockStateModel>> IRORI_FIREWOOD_MODEL_KEYS = createIroriFirewoodModelKeys();
    private static final Map<IroriFirewoodModel, BlockStateModel> IRORI_FIREWOOD_MODELS = new EnumMap<>(IroriFirewoodModel.class);
    private static final Map<WoodPostChainModelKey, StandaloneModelKey<BlockStateModel>> WOOD_POST_CHAIN_MODEL_KEYS = new HashMap<>();
    private static final Map<WoodPostChainModelKey, BlockStateModel> WOOD_POST_CHAIN_MODELS = new HashMap<>();
    private static final Map<WoodPostLinkModelKey, StandaloneModelKey<BlockStateModel>> WOOD_POST_LINK_MODEL_KEYS = new HashMap<>();
    private static final Map<WoodPostLinkModelKey, BlockStateModel> WOOD_POST_LINK_MODELS = new HashMap<>();

    private BlockModelRegistry() {
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        registerVanityModels(event);
        registerIroriFirewoodModels(event);
        registerWoodPostConnectionModels(event);
    }

    public static void cacheBakedModels(ModelEvent.BakingCompleted event) {
        cacheVanityModels(event);
        cacheIroriFirewoodModels(event);
        cacheWoodPostConnectionModels(event);
    }

    public static void wrapBlockStateModels(ModelEvent.ModifyBakingResult event) {
        event.getBakingResult().blockStateModels().replaceAll((state, model) -> {
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

    public static @Nullable BlockStateModel getIroriFirewoodModel(IroriFirewoodModel model) {
        BlockStateModel cachedModel = IRORI_FIREWOOD_MODELS.get(model);
        if (cachedModel != null) {
            return cachedModel;
        }

        return Minecraft.getInstance()
                .getModelManager()
                .getStandaloneModel(IRORI_FIREWOOD_MODEL_KEYS.get(model));
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
        for (IroriFirewoodModel model : IroriFirewoodModel.values()) {
            BlockStateModel bakedModel = event.getModelManager().getStandaloneModel(IRORI_FIREWOOD_MODEL_KEYS.get(model));
            if (bakedModel != null) {
                IRORI_FIREWOOD_MODELS.put(model, bakedModel);
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

    private static Map<IroriFirewoodModel, StandaloneModelKey<BlockStateModel>> createIroriFirewoodModelKeys() {
        Map<IroriFirewoodModel, StandaloneModelKey<BlockStateModel>> modelKeys = new EnumMap<>(IroriFirewoodModel.class);
        for (IroriFirewoodModel model : IroriFirewoodModel.values()) {
            Identifier id = ShadowsAndPetals.asResource("irori_firewood/" + model.modelName());
            modelKeys.put(model, new StandaloneModelKey<>(id::toString));
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
