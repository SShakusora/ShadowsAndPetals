package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampBlock;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPart;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowBlock;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState;
import com.sshakusora.shadowsandpetals.client.model.bonsai.BonsaiPotBlockStateModel;
import com.sshakusora.shadowsandpetals.client.model.registry.*;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Declarative client model registrations used by dynamic block renderers. */
public final class BlockModelRegistry {
    public static final StandaloneBlockModelSet<IroriFuelState.FirewoodModel> IRORI_FIREWOOD =
            ClientModelRegistry.enumBlockStateSet("irori_firewood", IroriFuelState.FirewoodModel.class)
                    .keyPath(IroriFuelState.FirewoodModel::modelName)
                    .model(model -> ShadowsAndPetals.asResource("block/irori/firewood/" + model.modelName()))
                    .register();

    public static final StandaloneBlockModelSet<IroriGrillPart> IRORI_GRILL_LOWER =
            ClientModelRegistry.enumBlockStateSet("irori_grill_lower", IroriGrillPart.class)
                    .keyPath(IroriGrillPart::modelName)
                    .model(part -> ShadowsAndPetals.asResource(
                            "block/grill/double/" + part.modelName() + "_lower"))
                    .rotation(part -> isHorizontalGrillPart(part)
                            ? StandaloneModelRotation.of(0, 90)
                            : StandaloneModelRotation.IDENTITY)
                    .register();

    public static final StandaloneBlockModelSet<WoodBlockList.WoodType> VANITY_DRAWER =
            ClientModelRegistry.enumBlockStateSet("vanity_drawer", WoodBlockList.WoodType.class)
                    .keyPath(WoodBlockList.WoodType::getName)
                    .model(wood -> ShadowsAndPetals.asResource("block/vanity/" + wood.getName() + "_drawer"))
                    .register();

    private static final StandaloneBlockModelSet<WoodPostChainModelKey> WOOD_POST_CHAINS =
            ClientModelRegistry.<WoodPostChainModelKey>blockStateSet("wood_post_chain")
                    .keys(BlockModelRegistry::woodPostChainKeys)
                    .keyPath(key -> key.type().getSerializedName() + "/" + key.direction().getSerializedName())
                    .model(key -> chainModelId(key.type(), usesUpperModel(key.direction())))
                    .rotation(key -> rotationForDirection(key.direction()))
                    .register();

    private static final StandaloneBlockModelSet<WoodPostLinkModelKey> WOOD_POST_LINKS =
            ClientModelRegistry.<WoodPostLinkModelKey>blockStateSet("wood_post_link")
                    .keys(BlockModelRegistry::woodPostLinkKeys)
                    .keyPath(key -> key.blockName() + "/" + key.direction().getSerializedName())
                    .model(key -> linkModelId(key.blockId(), usesUpperModel(key.direction())))
                    .rotation(key -> rotationForDirection(key.direction()))
                    .register();

    public static final StandaloneBlockModel SHISHI_ODOSHI_MAIN = ClientModelRegistry
            .blockState("shishi_odoshi_main")
            .model(ShadowsAndPetals.asResource("block/shishi_odoshi/main"))
            .register();

    public static final StandaloneBlockModelSet<DyeColor> WIND_CHIME_BODY = ClientModelRegistry
            .enumBlockStateSet("wind_chime_body", DyeColor.class)
            .keyPath(DyeColor::getName)
            .model(WindChimeColors::blockBodyModelId)
            .register();

    public static final StandaloneBlockModelSet<DyeColor> WIND_CHIME_MAIN_RIBBON = ClientModelRegistry
            .enumBlockStateSet("wind_chime_main_ribbon", DyeColor.class)
            .keyPath(DyeColor::getName)
            .model(WindChimeColors::blockMainRibbonModelId)
            .register();

    public static final StandaloneBlockModelSet<DyeColor> WIND_CHIME_VANE = ClientModelRegistry
            .enumBlockStateSet("wind_chime_vane", DyeColor.class)
            .keyPath(DyeColor::getName)
            .model(WindChimeColors::blockVaneModelId)
            .register();

    public static final StandaloneBlockModelSet<RecessedLampModelKey> RECESSED_LAMP_CONNECTED =
            ClientModelRegistry.<RecessedLampModelKey>blockStateSet("recessed_lamp_connected")
                    .keys(BlockModelRegistry::recessedLampModelKeys)
                    .keyPath(key -> (key.ceiling() ? "down" : "up") + "/" + key.index()
                            + (key.lit() ? "_on" : "_off"))
                    .model(key -> ShadowsAndPetals.asResource(
                            "block/recessed_lamp/" + (key.ceiling() ? "down" : "up") + "_"
                                    + key.index() + (key.lit() ? "_on" : "_off")))
                    .register();

    public static final StandaloneBlockModel WIND_CHIME_ITEM_BODY = ClientModelRegistry
            .blockState("wind_chime_item_body")
            .model(WindChimeColors.itemBodyModelId())
            .register();

    public static final StandaloneBlockModelSet<DyeColor> WIND_CHIME_ITEM_RIBBON = ClientModelRegistry
            .enumBlockStateSet("wind_chime_item_ribbon", DyeColor.class)
            .keyPath(DyeColor::getName)
            .model(WindChimeColors::itemRibbonModelId)
            .register();

    public static final StandaloneBlockModelSet<DyeColor> WIND_CHIME_ITEM_VANE = ClientModelRegistry
            .enumBlockStateSet("wind_chime_item_vane", DyeColor.class)
            .keyPath(DyeColor::getName)
            .model(WindChimeColors::itemVaneModelId)
            .register();

    public static final StandaloneBlockModel COPPER_TEAPOT_LID = ClientModelRegistry
            .blockState("copper_teapot_lid")
            .model(ShadowsAndPetals.asResource("block/teapot/copper/lid"))
            .register();

    public static final StandaloneBlockModelSet<BonsaiBlockEntity.Shape> BONSAI_SHAPES =
            ClientModelRegistry.enumBlockStateSet("bonsai_shape", BonsaiBlockEntity.Shape.class)
                    .keyPath(BonsaiBlockEntity.Shape::getSerializedName)
                    .model(shape -> ShadowsAndPetals.asResource("block/bonsai/bonsai_" + shape.getSerializedName()))
                    .register();

    public static final StandaloneBlockModelSet<BonsaiBlockEntity.Shape> BONSAI_DEAD_SHAPES =
            ClientModelRegistry.enumBlockStateSet("bonsai_shape_dead", BonsaiBlockEntity.Shape.class)
                    .keyPath(BonsaiBlockEntity.Shape::getSerializedName)
                    .model(shape -> ShadowsAndPetals.asResource(
                            "block/bonsai/bonsai_" + shape.getSerializedName() + "_dead"))
                    .register();

    public static final String[] CURTAIN_UPPER_BONES = {
            "panel_1_anchor", "panel_1_fabric", "panel_2_anchor", "panel_2_fabric",
            "panel_3_anchor", "panel_3_fabric", "panel_4_anchor", "panel_4_fabric", "rail"
    };
    public static final String[] CURTAIN_LOWER_BONES = {"panel_1", "panel_2", "panel_3", "panel_4"};

    public static final StandaloneBlockModelSet<CurtainBoneKey> CURTAIN_UPPER_RIGHT =
            curtainBoneSet("curtain_upper_right", "right", CURTAIN_UPPER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> CURTAIN_LOWER_RIGHT =
            curtainBoneSet("curtain_lower_right", "right", CURTAIN_LOWER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> CURTAIN_UPPER_LEFT =
            curtainBoneSet("curtain_upper_left", "left", CURTAIN_UPPER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> CURTAIN_LOWER_LEFT =
            curtainBoneSet("curtain_lower_left", "left", CURTAIN_LOWER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> LONG_CURTAIN_UPPER_RIGHT =
            longCurtainBoneSet("long_curtain_upper_right", "right", CURTAIN_UPPER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> LONG_CURTAIN_LOWER_RIGHT =
            longCurtainBoneSet("long_curtain_lower_right", "right", CURTAIN_LOWER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> LONG_CURTAIN_UPPER_LEFT =
            longCurtainBoneSet("long_curtain_upper_left", "left", CURTAIN_UPPER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> LONG_CURTAIN_LOWER_LEFT =
            longCurtainBoneSet("long_curtain_lower_left", "left", CURTAIN_LOWER_BONES);

    public static final String[] LARGE_CURTAIN_BONES = {
            "panel_1_anchor", "panel_1_fabric", "panel_2_anchor", "panel_2_fabric",
            "panel_3_anchor", "panel_3_fabric", "panel_4_anchor", "panel_4_fabric",
            "panel_5_anchor", "panel_5_fabric", "panel_6_anchor", "panel_6_fabric",
            "panel_7_anchor", "panel_7_fabric", "panel_8_anchor", "panel_8_fabric", "rail"
    };

    public static final StandaloneBlockModelSet<CurtainBoneKey> LARGE_CURTAIN_RIGHT =
            largeCurtainBoneSet("right");
    public static final StandaloneBlockModelSet<CurtainBoneKey> LARGE_CURTAIN_LEFT =
            largeCurtainBoneSet("left");

    public record CurtainBoneKey(DyeColor color, String bone) {
    }

    static {
        BlockStateModelDecoratorRegistry.forBlock(IroriBlock.class)
                .wrap(IroriBlockStateModel::new)
                .register();
        BlockStateModelDecoratorRegistry.forBlock(WoodPostBlock.class)
                .wrap(WoodPostBlockStateModel::new)
                .register();
        BlockStateModelDecoratorRegistry.forBlock(BonsaiBlock.class)
                .wrapWithState(BonsaiPotBlockStateModel::new)
                .register();
        BlockStateModelDecoratorRegistry.forBlock(CasementWindowBlock.class)
                .wrapWithState(CasementWindowBlockStateModel::new)
                .register();
        BlockStateModelDecoratorRegistry.forBlock(RecessedLampBlock.class)
                .wrap(RecessedLampConnectedBlockStateModel::new)
                .register();
    }

    private BlockModelRegistry() {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        ClientModelRegistry.registerAdditionalModels(event);
    }

    public static void cacheBakedModels(ModelEvent.BakingCompleted event) {
        ClientModelRegistry.cacheBakedModels(event);
        RecessedLampConnectedBlockStateModel.clearCache();
    }

    public static @Nullable BakedModel getRecessedLampModel(RecessedLampModelKey key) {
        return RECESSED_LAMP_CONNECTED.get(key);
    }

    public static void wrapBlockStateModels(ModelEvent.ModifyBakingResult event) {
        BlockStateModelDecoratorRegistry.applyAll(event);
    }

    public static void wrapRecessedLampCompositeModels(ModelEvent.ModifyBakingResult event) {
        Block block = BlockRegistry.RECESSED_LAMP_COMPOSITE.get();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            event.getModels().computeIfPresent(location, (ignored, model) ->
                    model instanceof RecessedLampCompositeBlockStateModel
                            ? model : new RecessedLampCompositeBlockStateModel(block, model));
        }
    }

    public static void wrapIroriGrillCopperTeapotModels(ModelEvent.ModifyBakingResult event) {
        Block block = BlockRegistry.IRORI_GRILL_COPPER_TEAPOT.get();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            event.getModels().computeIfPresent(location, (ignored, model) ->
                    model instanceof IroriGrillCopperTeapotBlockStateModel
                            ? model : new IroriGrillCopperTeapotBlockStateModel(block, model));
        }
    }

    public static @Nullable BakedModel getVanityDrawerModel(Block vanityBlock) {
        return VANITY_DRAWER.get(vanityWoodTypeFor(vanityBlock));
    }

    public static @Nullable BakedModel getWoodPostConnectionModel(
            Block block, WoodPostBlock.ConnectionType type, Direction direction) {
        if (type == WoodPostBlock.ConnectionType.OTHER_POST) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            return WOOD_POST_LINKS.get(new WoodPostLinkModelKey(blockId, direction));
        }
        if (!type.isChain()) {
            return null;
        }
        return WOOD_POST_CHAINS.get(new WoodPostChainModelKey(type, direction));
    }

    private static StandaloneBlockModelSet<CurtainBoneKey> curtainBoneSet(
            String setName, String side, String[] bones) {
        return ClientModelRegistry.<CurtainBoneKey>blockStateSet(setName)
                .keys(() -> curtainBoneKeys(bones))
                .keyPath(key -> key.color().getName() + "/" + key.bone())
                .model(key -> ShadowsAndPetals.asResource(
                        "block/curtain/animated/" + side + "/"
                                + key.color().getName() + "/" + key.bone()))
                .register();
    }

    private static StandaloneBlockModelSet<CurtainBoneKey> longCurtainBoneSet(
            String setName, String side, String[] bones) {
        return ClientModelRegistry.<CurtainBoneKey>blockStateSet(setName)
                .keys(() -> curtainBoneKeys(bones))
                .keyPath(key -> key.color().getName() + "/" + key.bone())
                .model(key -> ShadowsAndPetals.asResource(
                        "block/long_curtain/animated/" + side + "/"
                                + key.color().getName() + "/" + key.bone()))
                .register();
    }

    private static StandaloneBlockModelSet<CurtainBoneKey> largeCurtainBoneSet(String side) {
        return ClientModelRegistry.<CurtainBoneKey>blockStateSet("large_curtain_" + side)
                .keys(() -> curtainBoneKeys(LARGE_CURTAIN_BONES))
                .keyPath(key -> key.color().getName() + "/" + key.bone())
                .model(key -> ShadowsAndPetals.asResource(
                        "block/large_curtain/animated/" + side + "/"
                                + key.color().getName() + "/" + key.bone()))
                .register();
    }

    private static List<CurtainBoneKey> curtainBoneKeys(String[] bones) {
        List<CurtainBoneKey> keys = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            for (String bone : bones) {
                keys.add(new CurtainBoneKey(color, bone));
            }
        }
        return keys;
    }

    private static Iterable<WoodPostChainModelKey> woodPostChainKeys() {
        List<WoodPostChainModelKey> keys = new ArrayList<>();
        for (WoodPostBlock.ConnectionType type : WoodPostBlock.ConnectionType.values()) {
            if (type.isChain()) {
                for (Direction direction : Direction.values()) {
                    keys.add(new WoodPostChainModelKey(type, direction));
                }
            }
        }
        return keys;
    }

    private static Iterable<RecessedLampModelKey> recessedLampModelKeys() {
        List<RecessedLampModelKey> keys = new ArrayList<>();
        for (boolean ceiling : new boolean[]{false, true}) {
            for (boolean lit : new boolean[]{false, true}) {
                for (int index = 1; index <= 15; index++) {
                    keys.add(new RecessedLampModelKey(ceiling, lit, index));
                }
            }
        }
        return keys;
    }

    private static Iterable<WoodPostLinkModelKey> woodPostLinkKeys() {
        List<WoodPostLinkModelKey> keys = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof WoodPostBlock) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                for (Direction direction : Direction.values()) {
                    keys.add(new WoodPostLinkModelKey(blockId, direction));
                }
            }
        }
        return keys;
    }

    private static ResourceLocation chainModelId(WoodPostBlock.ConnectionType type, boolean upperHalf) {
        return ShadowsAndPetals.asResource(
                "block/wood_post_" + type.getSerializedName() + (upperHalf ? "_link_top" : "_link"));
    }

    private static ResourceLocation linkModelId(ResourceLocation blockId, boolean upperHalf) {
        return ShadowsAndPetals.asResource(
                "block/" + blockId.getPath() + (upperHalf ? "_link_top" : "_link"));
    }

    private static boolean usesUpperModel(Direction direction) {
        return switch (direction) {
            case UP, NORTH, EAST -> true;
            default -> false;
        };
    }

    private static StandaloneModelRotation rotationForDirection(Direction direction) {
        return switch (direction) {
            case DOWN, UP -> StandaloneModelRotation.IDENTITY;
            case NORTH, SOUTH -> StandaloneModelRotation.of(90, 0);
            case WEST, EAST -> StandaloneModelRotation.of(90, 90);
        };
    }

    private static boolean isHorizontalGrillPart(IroriGrillPart part) {
        return part == IroriGrillPart.STRIP_WEST || part == IroriGrillPart.STRIP_EAST;
    }

    private static WoodBlockList.WoodType vanityWoodTypeFor(Block vanityBlock) {
        String path = BuiltInRegistries.BLOCK.getKey(vanityBlock).getPath();
        String woodName = path.endsWith("_vanity")
                ? path.substring(0, path.length() - "_vanity".length()) : "oak";
        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            if (woodType.getName().equals(woodName)) {
                return woodType;
            }
        }
        return WoodBlockList.WoodType.OAK;
    }

    private record WoodPostChainModelKey(WoodPostBlock.ConnectionType type, Direction direction) {
    }

    private record WoodPostLinkModelKey(ResourceLocation blockId, Direction direction) {
        private String blockName() {
            return blockId.getPath();
        }
    }

    public record RecessedLampModelKey(boolean ceiling, boolean lit, int index) {
        public RecessedLampModelKey {
            if (index < 1 || index > 15) {
                throw new IllegalArgumentException("Recessed lamp model index must be between 1 and 15");
            }
        }
    }
}
