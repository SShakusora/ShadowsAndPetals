package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.decoration.*;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelTemplates;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
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
        JsonObject variants = new JsonObject();
        JsonObject state = new JsonObject();
        state.addProperty("model", model.id().toString());
        variants.add("", state);

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
    }

    public void simpleBlockItem(Block block, ModelRef model) {
        this.models.put(itemModelId(block), BlockModelTemplates.parentModel(model.id()));
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
        this.models.put(modelId, BlockModelTemplates.cubeAllModel(texture));
        simpleBlockWithItem(block, new ModelRef(modelId));
    }

    public void axisBlockWithItem(RotatedPillarBlock block, Identifier sideTexture, Identifier endTexture) {
        Identifier modelId = blockModelId(block);
        this.models.put(modelId, BlockModelTemplates.cubeColumnModel(sideTexture, endTexture));

        JsonObject variants = new JsonObject();
        variants.add(RotatedPillarBlock.AXIS.getName() + "=x", rotatedModel(modelId, 90, 90));
        variants.add(RotatedPillarBlock.AXIS.getName() + "=y", modelRef(modelId));
        variants.add(RotatedPillarBlock.AXIS.getName() + "=z", rotatedModel(modelId, 90, 0));

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), BlockModelTemplates.parentModel(modelId));
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

        JsonObject variants = new JsonObject();
        variants.add("axis=y", rotatedModel(coreModel, 0, 0));
        variants.add("axis=x", rotatedModel(coreModel, 90, 90));
        variants.add("axis=z", rotatedModel(coreModel, 90, 0));

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), BlockModelTemplates.parentModel(coreModel));
    }

    public void saplingBlock(SaplingBlock block) {
        saplingBlock(block, modLoc("block/" + name(block)));
    }

    public void saplingBlock(SaplingBlock block, Identifier texture) {
        Identifier modelId = blockModelId(block);
        this.models.put(modelId, BlockModelTemplates.crossModel(texture));
        simpleBlock(block, new ModelRef(modelId));
    }

    public void hedgeBlockWithItem(HedgeBlock block, Identifier texture) {
        Identifier straightModel = modLoc("block/" + name(block) + "_5");

        JsonObject variants = new JsonObject();
        for (int mask = 0; mask < 16; mask++) {
            boolean north = (mask & 1) != 0;
            boolean east = (mask & 1 << 1) != 0;
            boolean south = (mask & 1 << 2) != 0;
            boolean west = (mask & 1 << 3) != 0;
            Identifier modelId = modLoc("block/" + name(block) + "_" + mask);
            this.models.put(modelId, BlockModelTemplates.hedgeStateModel(texture, north, east, south, west));
            variants.add(hedgeVariantKey(north, east, south, west, false), modelRef(modelId));
            variants.add(hedgeVariantKey(north, east, south, west, true), modelRef(modelId));
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), BlockModelTemplates.parentModel(straightModel));
    }

    public void ingotPileBlock(IngotPileBlock block) {
        String blockName = name(block);
        String metalName = blockName.endsWith("_ingot_pile")
                ? blockName.substring(0, blockName.length() - "_ingot_pile".length())
                : blockName;

        JsonObject variants = new JsonObject();
        for (SlabType type : SlabType.values()) {
            boolean isDouble = type == SlabType.DOUBLE;
            String modelPath = "block/ingot_pile/" + metalName + (isDouble ? "_double" : "_bottom");
            Identifier modelId = modLoc(modelPath);

            variants.add("axis=x,type=" + type.getSerializedName(), modelRef(modelId));
            variants.add("axis=z,type=" + type.getSerializedName(), rotatedModel(modelId, 0, 90));
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), BlockModelTemplates.parentModel(modLoc("block/ingot_pile/" + metalName + "_bottom")));
    }

    public void vanityBlock(VanityBlock block) {
        String blockName = name(block);
        String woodName = blockName.endsWith("_vanity")
                ? blockName.substring(0, blockName.length() - "_vanity".length())
                : blockName;

        Identifier lowerModel = modLoc("block/vanity/" + woodName + "_lower");
        Identifier upperModel = modLoc("block/vanity/" + woodName + "_upper");

        JsonObject variants = new JsonObject();
        for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
            Identifier model = half == DoubleBlockHalf.LOWER ? lowerModel : upperModel;
            for (boolean waterlogged : new boolean[]{false, true}) {
                int i = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    addVanityVariant(variants, half, waterlogged, dir, model, i * 90);
                    i++;
                }
            }
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
    }

    public void iroriBlock(IroriBlock block) {
        Identifier basePath = modLoc("block/irori/block");
        JsonObject variants = new JsonObject();

        for (boolean waterlogged : new boolean[]{false, true}) {
            for (boolean north : new boolean[]{false, true}) {
                for (boolean east : new boolean[]{false, true}) {
                    for (boolean south : new boolean[]{false, true}) {
                        for (boolean west : new boolean[]{false, true}) {
                            addIroriVariant(variants, north, east, south, west, waterlogged);
                        }
                    }
                }
            }
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
        this.models.put(itemModelId(block), BlockModelTemplates.parentModel(basePath));
    }

    public void bedroomLampBlock(BedroomLampBlock block) {
        Identifier onModel = modLoc("block/bedroom_lamp/on");
        Identifier offModel = modLoc("block/bedroom_lamp/off");

        JsonObject variants = new JsonObject();
        variants.add(BedroomLampBlock.LIT.getName() + "=true", modelRef(onModel));
        variants.add(BedroomLampBlock.LIT.getName() + "=false", modelRef(offModel));

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
    }

    public void wallLampBlock(WallLampBlock block) {
        Identifier onModel = modLoc("block/wall_lamp/on");
        Identifier offModel = modLoc("block/wall_lamp/off");

        JsonObject variants = new JsonObject();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int y = switch (dir) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            variants.add("facing=" + dir.getSerializedName() + ",lit=true", rotatedModel(onModel, 0, y));
            variants.add("facing=" + dir.getSerializedName() + ",lit=false", rotatedModel(offModel, 0, y));
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
    }

    public void emergencyLampBlock(EmergencyLampBlock block) {
        Identifier onModel = modLoc("block/emergency_lamp/on");
        Identifier offModel = modLoc("block/emergency_lamp/off");

        JsonObject variants = new JsonObject();
        for (Direction dir : Direction.values()) {
            int x = dir == Direction.UP ? 0 : dir == Direction.DOWN ? 180 : 90;
            int y = switch (dir) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            variants.add("facing=" + dir.getSerializedName() + ",lit=true", rotatedModel(onModel, x, y));
            variants.add("facing=" + dir.getSerializedName() + ",lit=false", rotatedModel(offModel, x, y));
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
    }

    public void deskLampBlock(DeskLampBlock block) {
        Identifier onModel = modLoc("block/desk_lamp/on");
        Identifier offModel = modLoc("block/desk_lamp/off");

        JsonObject variants = new JsonObject();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int y = switch (dir) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            variants.add("facing=" + dir.getSerializedName() + ",lit=true", rotatedModel(onModel, 0, y));
            variants.add("facing=" + dir.getSerializedName() + ",lit=false", rotatedModel(offModel, 0, y));
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
    }

    public void rockeryBlock(RockeryBlock block, RockeryDimensions dims) {
        JsonObject variants = new JsonObject();
        for (int part = 0; part <= RockeryBlock.PART.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0); part++) {
            Vec3i pos = dims.localPos(part < dims.partCount() ? part : 0);
            Identifier modelId = modLoc(dims.modelDir()
                    + "/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                int yRot = (int) facing.toYRot();
                variants.add(
                        RockeryBlock.FACING.getName() + "=" + facing.getSerializedName()
                                + "," + RockeryBlock.PART.getName() + "=" + part,
                        rotatedModel(modelId, 0, yRot)
                );
            }
        }

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        this.blockStates.put(id(block), root);
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

    private static JsonObject modelRef(Identifier modelId) {
        JsonObject json = new JsonObject();
        json.addProperty("model", modelId.toString());
        return json;
    }

    private static JsonObject rotatedModel(Identifier modelId, int x, int y) {
        return rotatedModel(modelId, x, y, false);
    }

    private static JsonObject rotatedModel(Identifier modelId, int x, int y, boolean uvLock) {
        JsonObject json = modelRef(modelId);
        if (x != 0) {
            json.addProperty("x", x);
        }
        if (y != 0) {
            json.addProperty("y", y);
        }
        if (uvLock) {
            json.addProperty("uvlock", true);
        }
        return json;
    }

    private static JsonObject multipartPart(@Nullable JsonObject when, JsonObject apply) {
        JsonObject json = new JsonObject();
        if (when != null) {
            json.add("when", when);
        }
        json.add("apply", apply);
        return json;
    }

    private static JsonObject singleCondition(String key, String value) {
        JsonObject json = new JsonObject();
        json.addProperty(key, value);
        return json;
    }

    private static Identifier chainModelId(WoodPostBlock.ConnectionType type, boolean upperHalf) {
        return ShadowsAndPetals.asResource("block/wood_post_" + type.getSerializedName() + (upperHalf ? "_link_top" : "_link"));
    }

    private static void addVanityVariant(JsonObject variants, DoubleBlockHalf half, boolean waterlogged, Direction facing, Identifier modelId, int y) {
        variants.add(
                HorizontalDirectionalBlock.FACING.getName() + "=" + facing.getSerializedName()
                        + "," + VanityBlock.HALF.getName() + "=" + half.getSerializedName()
                        + "," + BlockStateProperties.WATERLOGGED.getName() + "=" + waterlogged,
                rotatedModel(modelId, 0, y)
        );
    }

    private void addIroriVariant(JsonObject variants, boolean north, boolean east, boolean south, boolean west, boolean waterlogged) {
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

        variants.add(
                IroriBlock.NORTH.getName() + "=" + north
                        + "," + IroriBlock.EAST.getName() + "=" + east
                        + "," + IroriBlock.SOUTH.getName() + "=" + south
                        + "," + IroriBlock.WEST.getName() + "=" + west
                        + "," + IroriBlock.WATERLOGGED.getName() + "=" + waterlogged,
                rotatedModel(modelId, 0, iroriModelRotation(edgeNorth, edgeEast, edgeSouth, edgeWest, edgeCount))
        );
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

    private static String hedgeVariantKey(boolean north, boolean east, boolean south, boolean west, boolean waterlogged) {
        return HedgeBlock.NORTH.getName() + "=" + north
                + "," + HedgeBlock.EAST.getName() + "=" + east
                + "," + HedgeBlock.SOUTH.getName() + "=" + south
                + "," + HedgeBlock.WEST.getName() + "=" + west
                + "," + BlockStateProperties.WATERLOGGED.getName() + "=" + waterlogged;
    }

    public static final class Models {
        public ModelRef getExistingFile(Identifier id) {
            return new ModelRef(id);
        }
    }

    public record ModelRef(Identifier id) {}
}
