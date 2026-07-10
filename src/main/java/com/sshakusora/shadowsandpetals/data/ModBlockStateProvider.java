package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.agriculture.OrangeTreeBlock;
import com.sshakusora.shadowsandpetals.block.decoration.*;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelTemplates;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ModBlockStateProvider implements DataProvider {
    private final PackOutput.PathProvider blockStatePathProvider;
    private final PackOutput.PathProvider modelPathProvider;
    private final Map<Identifier, JsonObject> blockStates = new LinkedHashMap<>();
    private final Map<Identifier, JsonObject> models = new LinkedHashMap<>();
    private final Models modelsHelper = new Models();

    public ModBlockStateProvider(PackOutput output) {
        this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.blockStates.clear();
        this.models.clear();
        registerStatesAndModels();

        CompletableFuture<?> blockStateTask = DataProvider.saveAll(cache, json -> json, this.blockStatePathProvider::json, this.blockStates);
        CompletableFuture<?> modelTask = DataProvider.saveAll(cache, json -> json, this.modelPathProvider::json, this.models);
        return CompletableFuture.allOf(blockStateTask, modelTask);
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Block States";
    }

    protected void registerStatesAndModels() {
        for (var generator : DatagenBlockStateRegistry.generators()) {
            generator.accept(this);
        }
    }

    public Identifier modLoc(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    public Identifier mcLoc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    public Models models() {
        return this.modelsHelper;
    }

    public void simpleBlockWithItem(Block block, ModelRef model) {
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    public void simpleBlock(Block block, ModelRef model) {
        putBlockState(block, BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(model.id())));
    }

    public void simpleBlockItem(Block block, ModelRef model) {
        putParentModel(itemModelId(block), model.id());
    }

    public void leavesBlockWithItem(LeavesBlock block) {
        cubeAllBlockWithItem(block, modLoc("block/" + name(block)));
    }

    public void leavesBlockWithItem(LeavesBlock block, Identifier texture) {
        cubeAllBlockWithItem(block, texture);
    }

    public void cubeAllBlockWithItem(Block block) {
        cubeAllBlockWithItem(block, modLoc("block/" + name(block)));
    }

    public void cubeAllBlockWithItem(Block block, Identifier texture) {
        Identifier modelId = blockModelId(block);
        putCubeAllModel(modelId, texture);
        simpleBlockWithItem(block, new ModelRef(modelId));
    }

    public void horizontalFacingCubeAllBlockWithItem(Block block, Identifier texture) {
        Identifier modelId = blockModelId(block);
        putCubeAllModel(modelId, texture);
        putBlockState(
                block,
                MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(modelId))
                        .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
        );
        putParentModel(itemModelId(block), modelId);
    }

    public void axisBlockWithItem(RotatedPillarBlock block, Identifier sideTexture, Identifier endTexture) {
        Identifier modelId = blockModelId(block);
        putCubeColumnModel(modelId, sideTexture, endTexture);
        putBlockState(block, BlockModelGenerators.createAxisAlignedPillarBlock(block, BlockModelGenerators.plainVariant(modelId)));
        putParentModel(itemModelId(block), modelId);
    }

    public void woodPostBlockWithItem(WoodPostBlock block, Identifier sideTexture, Identifier endTexture) {
        Identifier coreModel = blockModelId(block);
        Identifier lowerLinkModel = modLoc(coreModel.getPath() + "_link");
        Identifier upperLinkModel = modLoc(coreModel.getPath() + "_link_top");

        this.models.put(coreModel, BlockModelTemplates.woodPostCoreModel(sideTexture, endTexture));
        this.models.put(lowerLinkModel, BlockModelTemplates.woodPostLinkModel(sideTexture, endTexture, false));
        this.models.put(upperLinkModel, BlockModelTemplates.woodPostLinkModel(sideTexture, endTexture, true));

        for (WoodPostBlock.ConnectionType type : WoodPostBlock.ConnectionType.values()) {
            if (!type.isChain()) {
                continue;
            }

            Identifier lowerChainModel = chainModelId(type, false);
            Identifier upperChainModel = chainModelId(type, true);
            this.models.put(lowerChainModel, BlockModelTemplates.woodPostChainModel(false, type.texture()));
            this.models.put(upperChainModel, BlockModelTemplates.woodPostChainModel(true, type.texture()));
        }

        putBlockState(block, BlockModelGenerators.createAxisAlignedPillarBlock(block, BlockModelGenerators.plainVariant(coreModel)));
        putParentModel(itemModelId(block), coreModel);
    }

    public void saplingBlock(SaplingBlock block) {
        saplingBlock(block, modLoc("block/" + name(block)));
    }

    public void saplingBlock(SaplingBlock block, Identifier texture) {
        Identifier modelId = blockModelId(block);
        putCrossModel(modelId, texture);
        simpleBlock(block, new ModelRef(modelId));
    }

    public void orangeTreeBlock(OrangeTreeBlock block) {
        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(OrangeTreeBlock.AGE, OrangeTreeBlock.FACING, OrangeTreeBlock.HALF)
                        .generate((age, facing, half) -> {
                            String modelName = age < OrangeTreeBlock.DOUBLE_HEIGHT_AGE
                                    ? "tree_" + age
                                    : "tree_" + age + "_" + half.getSerializedName();
                            return horizontallyRotatedVariant(modLoc("block/orange/" + modelName), facing);
                        })));
    }

    public void hedgeBlockWithItem(HedgeBlock block, Identifier texture) {
        Identifier straightModel = modLoc("block/" + name(block) + "_5");
        for (int mask = 0; mask < 16; mask++) {
            boolean north = (mask & 1) != 0;
            boolean east = (mask & 1 << 1) != 0;
            boolean south = (mask & 1 << 2) != 0;
            boolean west = (mask & 1 << 3) != 0;
            Identifier modelId = modLoc("block/" + name(block) + "_" + mask);
            this.models.put(modelId, BlockModelTemplates.hedgeStateModel(texture, north, east, south, west));
        }
        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(HedgeBlock.NORTH, HedgeBlock.EAST, HedgeBlock.SOUTH, HedgeBlock.WEST, HedgeBlock.WATERLOGGED)
                        .generate((north, east, south, west, waterlogged) -> BlockModelGenerators.plainVariant(hedgeModelId(block, north, east, south, west)))));
        putParentModel(itemModelId(block), straightModel);
    }

    public void ingotPileBlock(IngotPileBlock block) {
        String blockName = name(block);
        String metalName = blockName.endsWith("_ingot_pile")
                ? blockName.substring(0, blockName.length() - "_ingot_pile".length())
                : blockName;

        Identifier bottomModel = modLoc("block/ingot_pile/" + metalName + "_bottom");
        Identifier doubleModel = modLoc("block/ingot_pile/" + metalName + "_double");
        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(IngotPileBlock.TYPE)
                        .select(SlabType.BOTTOM, BlockModelGenerators.plainVariant(bottomModel))
                        .select(SlabType.TOP, BlockModelGenerators.plainVariant(bottomModel))
                        .select(SlabType.DOUBLE, BlockModelGenerators.plainVariant(doubleModel)))
                .with(PropertyDispatch.modify(IngotPileBlock.HORIZONTAL_AXIS)
                        .select(Direction.Axis.X, BlockModelGenerators.NOP)
                        .select(Direction.Axis.Z, BlockModelGenerators.Y_ROT_90)));
        putParentModel(itemModelId(block), modLoc("block/ingot_pile/" + metalName + "_bottom"));
    }

    public void vanityBlock(VanityBlock block) {
        String blockName = name(block);
        String woodName = blockName.endsWith("_vanity")
                ? blockName.substring(0, blockName.length() - "_vanity".length())
                : blockName;

        Identifier lowerModel = modLoc("block/vanity/" + woodName + "_lower");
        Identifier upperModel = modLoc("block/vanity/" + woodName + "_upper");

        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(VanityBlock.HALF)
                        .select(DoubleBlockHalf.LOWER, BlockModelGenerators.plainVariant(lowerModel))
                        .select(DoubleBlockHalf.UPPER, BlockModelGenerators.plainVariant(upperModel)))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
                .with(PropertyDispatch.modify(VanityBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
    }

    public void iroriBlock(IroriBlock block) {
        Identifier basePath = modLoc("block/irori/block");
        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(IroriBlock.NORTH, IroriBlock.EAST, IroriBlock.SOUTH, IroriBlock.WEST, IroriBlock.WATERLOGGED)
                        .generate((north, east, south, west, waterlogged) -> iroriVariant(north, east, south, west))));
        putParentModel(itemModelId(block), basePath);
    }

    public void copperTeapotBlock(CopperTeapotBlock block) {
        Identifier mainModel = modLoc("block/teapot/copper/main");
        Identifier onIroriModel = modLoc("block/teapot/copper/main_on_irori");

        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(CopperTeapotBlock.ON_IRORI)
                        .select(false, BlockModelGenerators.plainVariant(mainModel))
                        .select(true, BlockModelGenerators.plainVariant(onIroriModel)))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
                .with(PropertyDispatch.modify(CopperTeapotBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
    }

    public void bedroomLampBlock(BedroomLampBlock block) {
        Identifier onModel = modLoc("block/bedroom_lamp/on");
        Identifier offModel = modLoc("block/bedroom_lamp/off");

        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BedroomLampBlock.LIT)
                        .select(false, BlockModelGenerators.plainVariant(offModel))
                        .select(true, BlockModelGenerators.plainVariant(onModel))));
    }

    public void wallLampBlock(WallLampBlock block) {
        Identifier onModel = modLoc("block/wall_lamp/on");
        Identifier offModel = modLoc("block/wall_lamp/off");

        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(WallLampBlock.LIT)
                        .select(false, BlockModelGenerators.plainVariant(offModel))
                        .select(true, BlockModelGenerators.plainVariant(onModel)))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
    }

    public void emergencyLampBlock(EmergencyLampBlock block) {
        Identifier onModel = modLoc("block/emergency_lamp/on");
        Identifier offModel = modLoc("block/emergency_lamp/off");

        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(EmergencyLampBlock.LIT)
                        .select(false, BlockModelGenerators.plainVariant(offModel))
                        .select(true, BlockModelGenerators.plainVariant(onModel)))
                .with(PropertyDispatch.modify(EmergencyLampBlock.FACING)
                        .select(Direction.UP, BlockModelGenerators.NOP)
                        .select(Direction.DOWN, BlockModelGenerators.X_ROT_180)
                        .select(Direction.NORTH, BlockModelGenerators.X_ROT_90)
                        .select(Direction.EAST, BlockModelGenerators.X_ROT_90.then(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.SOUTH, BlockModelGenerators.X_ROT_90.then(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.WEST, BlockModelGenerators.X_ROT_90.then(BlockModelGenerators.Y_ROT_270))));
    }

    public void deskLampBlock(DeskLampBlock block) {
        Identifier onModel = modLoc("block/desk_lamp/on");
        Identifier offModel = modLoc("block/desk_lamp/off");

        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(DeskLampBlock.LIT)
                        .select(false, BlockModelGenerators.plainVariant(offModel))
                        .select(true, BlockModelGenerators.plainVariant(onModel)))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
    }

    public void samonBlock(SamonBlock block) {
        Identifier straightModel = modLoc("block/samon/straight");
        Identifier cornerModel = modLoc("block/samon/corner");

        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(SamonBlock.FACING, SamonBlock.CORNER)
                        .generate((facing, corner) -> rotatedVariant(corner ? cornerModel : straightModel, 0, (int) facing.toYRot()))));
        putParentModel(itemModelId(block), straightModel);
    }

    public void shishiOdoshiBlock(ShishiOdoshiBlock block) {
        Identifier blockModel = modLoc("block/shishi_odoshi/block");

        putBlockState(block, MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(blockModel))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
                .with(PropertyDispatch.modify(ShishiOdoshiBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
    }

    public void windChimeBlock(WindChimeBlock block) {
        simpleBlock(block, new ModelRef(WindChimeColors.blockBodyModelId(WindChimeColors.DEFAULT_COLOR)));
        for (DyeColor ribbon : DyeColor.values()) {
            this.models.put(
                    WindChimeColors.blockBodyModelId(ribbon),
                    parentModelWithWindChimeBodyTexture(
                            modLoc("block/wind_chimes/block"),
                            ribbon
                    )
            );
            this.models.put(
                    WindChimeColors.blockMainRibbonModelId(ribbon),
                    parentModelWithWindChimeRibbonTexture(
                            modLoc("block/wind_chimes/main_ribbon"),
                            ribbon
                    )
            );
        }
        for (DyeColor vane : DyeColor.values()) {
            this.models.put(
                    WindChimeColors.blockVaneModelId(vane),
                    parentModelWithWindChimeVaneTexture(
                            modLoc("block/wind_chimes/vane"),
                            vane
                    )
            );
        }
    }

    public void shishiOdoshiPipeBlock(ShishiOdoshiPipeBlock block) {
        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(ShishiOdoshiPipeBlock.LENGTH)
                        .generate(length -> BlockModelGenerators.plainVariant(modLoc("block/shishi_odoshi/pipe/" + length.getSerializedName()))))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
                .with(PropertyDispatch.modify(ShishiOdoshiPipeBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
        putParentModel(itemModelId(block), modLoc("block/shishi_odoshi/pipe/long"));
    }

    public void rockeryBlock(RockeryBlock block, RockeryDimensions dims) {
        putBlockState(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(RockeryBlock.FACING, RockeryBlock.PART, RockeryBlock.WATERLOGGED)
                        .generate((facing, part, waterlogged) -> {
                            Vec3i pos = dims.localPos(part < dims.partCount() ? part : 0);
                            Identifier modelId = modLoc(dims.modelDir()
                                    + "/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
                            return rotatedVariant(modelId, 0, (int) facing.toYRot());
                        })));
    }

    private String name(Block block) {
        return id(block).getPath();
    }

    private Identifier id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    private Identifier blockModelId(Block block) {
        return modLoc("block/" + name(block));
    }

    private Identifier itemModelId(Block block) {
        return modLoc("item/" + name(block));
    }

    private void putBlockState(Block block, BlockModelDefinitionGenerator generator) {
        JsonObject json = BlockStateModelDispatcher.CODEC
                .encodeStart(JsonOps.INSTANCE, generator.create())
                .getOrThrow()
                .getAsJsonObject();
        this.blockStates.put(id(block), json);
    }

    private void putGeneratedModel(Identifier modelId, ModelInstance model) {
        this.models.put(modelId, model.get().getAsJsonObject());
    }

    private void putParentModel(Identifier modelId, Identifier parent) {
        new ModelTemplate(Optional.of(parent), Optional.empty())
                .create(modelId, new TextureMapping(), this::putGeneratedModel);
    }

    private void putCubeAllModel(Identifier modelId, Identifier texture) {
        ModelTemplates.CUBE_ALL.create(
                modelId,
                new TextureMapping().put(TextureSlot.ALL, new Material(texture)),
                this::putGeneratedModel
        );
    }

    private void putCubeColumnModel(Identifier modelId, Identifier sideTexture, Identifier endTexture) {
        ModelTemplates.CUBE_COLUMN.create(
                modelId,
                new TextureMapping()
                        .put(TextureSlot.SIDE, new Material(sideTexture))
                        .put(TextureSlot.END, new Material(endTexture)),
                this::putGeneratedModel
        );
    }

    private void putCrossModel(Identifier modelId, Identifier texture) {
        ModelTemplates.CROSS.create(
                modelId,
                new TextureMapping().put(TextureSlot.CROSS, new Material(texture)),
                this::putGeneratedModel
        );
        this.models.get(modelId).addProperty("render_type", "cutout");
    }

    private Identifier hedgeModelId(HedgeBlock block, boolean north, boolean east, boolean south, boolean west) {
        int mask = (north ? 1 : 0)
                | (east ? 1 << 1 : 0)
                | (south ? 1 << 2 : 0)
                | (west ? 1 << 3 : 0);
        return modLoc("block/" + name(block) + "_" + mask);
    }

    private MultiVariant iroriVariant(boolean north, boolean east, boolean south, boolean west) {
        boolean edgeNorth = !north;
        boolean edgeEast = !east;
        boolean edgeSouth = !south;
        boolean edgeWest = !west;
        int edgeCount = (edgeNorth ? 1 : 0) + (edgeEast ? 1 : 0) + (edgeSouth ? 1 : 0) + (edgeWest ? 1 : 0);

        Identifier modelId = switch (edgeCount) {
            case 0 -> modLoc("block/irori/center");
            case 1 -> modLoc("block/irori/single_edge");
            case 2 -> edgeNorth == edgeSouth || edgeEast == edgeWest
                    ? modLoc("block/irori/double_edge")
                    : modLoc("block/irori/corner");
            case 3 -> modLoc("block/irori/end");
            case 4 -> modLoc("block/irori/block");
            default -> throw new IllegalStateException("Unexpected edge count: " + edgeCount);
        };

        return rotatedVariant(modelId, 0, iroriModelRotation(edgeNorth, edgeEast, edgeSouth, edgeWest, edgeCount));
    }

    private static MultiVariant horizontallyRotatedVariant(Identifier modelId, Direction facing) {
        return rotatedVariant(modelId, 0, switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        });
    }

    private static MultiVariant rotatedVariant(Identifier modelId, int x, int y) {
        MultiVariant variant = BlockModelGenerators.plainVariant(modelId);
        variant = applyXRotation(variant, x);
        return applyYRotation(variant, y);
    }

    private static MultiVariant applyXRotation(MultiVariant variant, int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> variant;
            case 90 -> variant.with(BlockModelGenerators.X_ROT_90);
            case 180 -> variant.with(BlockModelGenerators.X_ROT_180);
            case 270 -> variant.with(BlockModelGenerators.X_ROT_270);
            default -> throw new IllegalArgumentException("Unsupported X rotation: " + degrees);
        };
    }

    private static MultiVariant applyYRotation(MultiVariant variant, int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> variant;
            case 90 -> variant.with(BlockModelGenerators.Y_ROT_90);
            case 180 -> variant.with(BlockModelGenerators.Y_ROT_180);
            case 270 -> variant.with(BlockModelGenerators.Y_ROT_270);
            default -> throw new IllegalArgumentException("Unsupported Y rotation: " + degrees);
        };
    }

    private static JsonObject parentModelWithWindChimeRibbonTexture(Identifier parent, DyeColor ribbon) {
        TextureSlot ribbonSlot = TextureSlot.create("2");
        Identifier ribbonTexture = ShadowsAndPetals.asResource("block/wind_chime/ribbon/" + ribbon.getName());
        return parentModelWithTextures(
                parent,
                new ModelTemplate(Optional.of(parent), Optional.empty(), ribbonSlot, TextureSlot.PARTICLE),
                new TextureMapping()
                        .put(ribbonSlot, new Material(ribbonTexture))
                        .put(TextureSlot.PARTICLE, new Material(ribbonTexture))
        );
    }

    private static JsonObject parentModelWithWindChimeBodyTexture(Identifier parent, DyeColor ribbon) {
        TextureSlot ribbonSlot = TextureSlot.create("2");
        return parentModelWithTextures(
                parent,
                new ModelTemplate(Optional.of(parent), Optional.empty(), ribbonSlot, TextureSlot.PARTICLE),
                new TextureMapping()
                        .put(ribbonSlot, new Material(ShadowsAndPetals.asResource("block/wind_chime/ribbon/" + ribbon.getName())))
                        .put(TextureSlot.PARTICLE, new Material(Identifier.withDefaultNamespace("block/glass")))
        );
    }

    private static JsonObject parentModelWithWindChimeVaneTexture(Identifier parent, DyeColor vane) {
        TextureSlot vaneSlot = TextureSlot.create("windchime0");
        Identifier vaneTexture = ShadowsAndPetals.asResource("block/wind_chime/vane/" + vane.getName());
        return parentModelWithTextures(
                parent,
                new ModelTemplate(Optional.of(parent), Optional.empty(), TextureSlot.PARTICLE, vaneSlot),
                new TextureMapping()
                        .put(TextureSlot.PARTICLE, new Material(vaneTexture))
                        .put(vaneSlot, new Material(vaneTexture))
        );
    }

    private static JsonObject parentModelWithTextures(Identifier parent, ModelTemplate template, TextureMapping mapping) {
        JsonObject[] result = new JsonObject[1];
        template.create(parent.withPrefix("generated/"), mapping, (ignored, model) -> result[0] = model.get().getAsJsonObject());
        return result[0];
    }

    private static Identifier chainModelId(WoodPostBlock.ConnectionType type, boolean upperHalf) {
        return ShadowsAndPetals.asResource("block/wood_post_" + type.getSerializedName() + (upperHalf ? "_link_top" : "_link"));
    }

    private static int iroriModelRotation(boolean north, boolean east, boolean south, boolean west, int edgeCount) {
        return switch (edgeCount) {
            case 0, 4 -> 0;
            case 1 -> east ? 0 : south ? 90 : west ? 180 : 270;
            case 2 -> {
                if (east && west) {
                    yield 0;
                }
                if (north && south) {
                    yield 90;
                }
                if (north && east) {
                    yield 0;
                }
                if (east && south) {
                    yield 90;
                }
                if (south && west) {
                    yield 180;
                }
                yield 270;
            }
            case 3 -> !south ? 0 : !west ? 90 : !north ? 180 : 270;
            default -> throw new IllegalStateException("Unexpected edge count: " + edgeCount);
        };
    }

    public static final class Models {
        public ModelRef getExistingFile(Identifier id) {
            return new ModelRef(id);
        }
    }

    public record ModelRef(Identifier id) {}
}
