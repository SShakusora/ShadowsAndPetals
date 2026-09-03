package com.sshakusora.shadowsandpetals.client.model;

import com.mojang.math.Quadrant;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState;
import com.sshakusora.shadowsandpetals.client.model.bonsai.BonsaiPotBlockStateModel;
import com.sshakusora.shadowsandpetals.client.model.registry.BlockStateModelDecoratorRegistry;
import com.sshakusora.shadowsandpetals.client.model.registry.ClientModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.registry.StandaloneBlockModel;
import com.sshakusora.shadowsandpetals.client.model.registry.StandaloneBlockModelSet;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative client model registrations used by dynamic block and block-entity renderers.
 */
public final class BlockModelRegistry {
    public static final StandaloneBlockModelSet<IroriFuelState.FirewoodModel> IRORI_FIREWOOD =
            ClientModelRegistry.enumBlockStateSet("irori_firewood", IroriFuelState.FirewoodModel.class)
                    .keyPath(IroriFuelState.FirewoodModel::modelName)
                    .model(model -> ShadowsAndPetals.asResource("block/irori/firewood/" + model.modelName()))
                    .register();

    public static final StandaloneBlockModelSet<IroriBlockEntity.GrillModel> IRORI_GRILL =
            ClientModelRegistry.enumBlockStateSet("irori_grill", IroriBlockEntity.GrillModel.class)
                    .keyPath(IroriBlockEntity.GrillModel::modelName)
                    .model(model -> ShadowsAndPetals.asResource("block/grill/" + model.modelName()))
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
                    .rotation(key -> rotationState(key.direction()))
                    .register();

    private static final StandaloneBlockModelSet<WoodPostLinkModelKey> WOOD_POST_LINKS =
            ClientModelRegistry.<WoodPostLinkModelKey>blockStateSet("wood_post_link")
                    .keys(BlockModelRegistry::woodPostLinkKeys)
                    .keyPath(key -> key.blockName() + "/" + key.direction().getSerializedName())
                    .model(key -> linkModelId(key.blockId(), usesUpperModel(key.direction())))
                    .rotation(key -> rotationState(key.direction()))
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
                    .model(shape -> ShadowsAndPetals.asResource("block/bonsai/bonsai_" + shape.getSerializedName() + "_dead"))
                    .register();

    /**
     * Per-bone baked models of the curtain rig, keyed by (dye color, bone).
     * Each set resolves to the per-bone files under
     * {@code block/curtain/curtain_<half>_<side>[_<color>]/<bone>}.
     */
    public static final String[] CURTAIN_UPPER_BONES = {
            "g1", "g1_1", "g2", "g2_1", "g3", "g3_1", "g4", "g4_1", "group"
    };
    public static final String[] CURTAIN_LOWER_BONES = {
            "g1", "g2", "g3", "g4"
    };

    public static final StandaloneBlockModelSet<CurtainBoneKey> CURTAIN_UPPER_R =
            curtainBoneSet("curtain_upper_r", CURTAIN_UPPER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> CURTAIN_LOWER_R =
            curtainBoneSet("curtain_lower_r", CURTAIN_LOWER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> CURTAIN_UPPER_L =
            curtainBoneSet("curtain_upper_l", CURTAIN_UPPER_BONES);
    public static final StandaloneBlockModelSet<CurtainBoneKey> CURTAIN_LOWER_L =
            curtainBoneSet("curtain_lower_l", CURTAIN_LOWER_BONES);

    /** A dye color paired with one rig bone of a curtain part. */
    public record CurtainBoneKey(DyeColor color, String bone) {
    }

    private static StandaloneBlockModelSet<CurtainBoneKey> curtainBoneSet(String part, String[] bones) {
        return ClientModelRegistry
                .<CurtainBoneKey>blockStateSet(part)
                .keys(() -> curtainBoneKeys(bones))
                .keyPath(key -> key.color().getName() + "/" + key.bone())
                .model(key -> ShadowsAndPetals.asResource(
                        "block/curtain/" + part
                                + (key.color() == DyeColor.WHITE ? "" : "_" + key.color().getName())
                                + "/" + key.bone()))
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
    }

    private BlockModelRegistry() {
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        ClientModelRegistry.registerStandaloneModels(event);
    }

    public static void cacheBakedModels(ModelEvent.BakingCompleted event) {
        ClientModelRegistry.cacheBakedModels(event);
    }

    public static void wrapBlockStateModels(ModelEvent.ModifyBakingResult event) {
        BlockStateModelDecoratorRegistry.applyAll(event);
    }

    public static void wrapRecessedLampCompositeModels(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> bakedModels =
                Map.copyOf(event.getBakingResult().blockStateModels());
        Map<BlockState, BlockStateModel> slabModels = new HashMap<>();
        bakedModels.forEach((state, model) -> {
            if (state.getBlock() instanceof SlabBlock
                    && state.hasProperty(BlockStateProperties.SLAB_TYPE)
                    && state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE
                    && !state.hasBlockEntity()) {
                slabModels.put(state, model);
            }
        });
        Map<BlockState, BlockStateModel> immutableSlabModels = Map.copyOf(slabModels);

        event.getBakingResult().blockStateModels().replaceAll((state, model) ->
                state.is(BlockRegistry.RECESSED_LAMP_COMPOSITE.get())
                        ? new RecessedLampCompositeBlockStateModel(
                                state.getBlock(), model, immutableSlabModels)
                        : model
        );
    }

    public static @Nullable BlockStateModel getVanityDrawerModel(Block vanityBlock) {
        return VANITY_DRAWER.get(vanityWoodTypeFor(vanityBlock));
    }

    public static @Nullable BlockStateModel getWoodPostConnectionModel(
            Block block,
            WoodPostBlock.ConnectionType type,
            Direction direction
    ) {
        if (type == WoodPostBlock.ConnectionType.OTHER_POST) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            return WOOD_POST_LINKS.get(new WoodPostLinkModelKey(blockId, direction));
        }
        if (!type.isChain()) {
            return null;
        }
        return WOOD_POST_CHAINS.get(new WoodPostChainModelKey(type, direction));
    }

    private static Iterable<WoodPostChainModelKey> woodPostChainKeys() {
        List<WoodPostChainModelKey> keys = new ArrayList<>();
        for (WoodPostBlock.ConnectionType type : WoodPostBlock.ConnectionType.values()) {
            if (!type.isChain()) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                keys.add(new WoodPostChainModelKey(type, direction));
            }
        }
        return keys;
    }

    private static Iterable<WoodPostLinkModelKey> woodPostLinkKeys() {
        List<WoodPostLinkModelKey> keys = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof WoodPostBlock)) {
                continue;
            }
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            for (Direction direction : Direction.values()) {
                keys.add(new WoodPostLinkModelKey(blockId, direction));
            }
        }
        return keys;
    }

    private static Identifier chainModelId(WoodPostBlock.ConnectionType type, boolean upperHalf) {
        return ShadowsAndPetals.asResource(
                "block/wood_post_" + type.getSerializedName() + (upperHalf ? "_link_top" : "_link")
        );
    }

    private static Identifier linkModelId(Identifier blockId, boolean upperHalf) {
        return ShadowsAndPetals.asResource(
                "block/" + blockId.getPath() + (upperHalf ? "_link_top" : "_link")
        );
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
        return new Variant.SimpleModelState(
                quadrant(xDegrees),
                quadrant(yDegrees),
                Quadrant.R0,
                false
        ).asModelState();
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
        String woodName = path.endsWith("_vanity")
                ? path.substring(0, path.length() - "_vanity".length())
                : "oak";

        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            if (woodType.getName().equals(woodName)) {
                return woodType;
            }
        }
        return WoodBlockList.WoodType.OAK;
    }

    private record WoodPostChainModelKey(WoodPostBlock.ConnectionType type, Direction direction) {
    }

    private record WoodPostLinkModelKey(Identifier blockId, Direction direction) {
        private String blockName() {
            return blockId.getPath();
        }
    }
}
