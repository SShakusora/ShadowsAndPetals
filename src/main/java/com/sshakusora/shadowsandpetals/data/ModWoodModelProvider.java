package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.block.decoration.RoofTileSlabBlock;
import com.sshakusora.shadowsandpetals.block.decoration.RoofTileVerticalSlabBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VerticalSlabBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class ModWoodModelProvider extends ModelProvider {
    private static final ModelTemplate ROOF_TILE_SLAB = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_slab")),
            Optional.empty(),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final ModelTemplate ROOF_TILE_SLAB_TOP = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_slab_top")),
            Optional.of("_top"),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final ModelTemplate ROOF_TILE_VERTICAL_SLAB = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_vertical_slab")),
            Optional.empty(),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final ModelTemplate ROOF_TILE_VERTICAL_SLAB_SOUTH = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_vertical_slab_south")),
            Optional.of("_south"),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final ModelTemplate ROOF_TILE_VERTICAL_SLAB_WEST = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_vertical_slab_west")),
            Optional.of("_west"),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final ModelTemplate ROOF_TILE_VERTICAL_SLAB_EAST = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_vertical_slab_east")),
            Optional.of("_east"),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final ModelTemplate ROOF_TILE_STAIRS = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_stairs")),
            Optional.empty(),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final ModelTemplate ROOF_TILE_INNER_STAIRS = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_inner_stairs")),
            Optional.of("_inner"),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final ModelTemplate ROOF_TILE_OUTER_STAIRS = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/roof_tile_outer_stairs")),
            Optional.of("_outer"),
            TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final Block[] WOOD_BLOCKS = BlockRegistry.WOOD_SETS.stream()
            .flatMap(ModWoodModelProvider::blocksOf)
            .toArray(Block[]::new);
    private static final Block[] ROOF_TILE_SHAPE_BLOCKS = Stream.concat(
            Stream.concat(BlockRegistry.ROOF_TILE_SLABS.stream(), BlockRegistry.ROOF_TILE_VERTICAL_SLABS.stream()),
            BlockRegistry.ROOF_TILE_STAIRS.stream()
    ).map(holder -> holder.get()).toArray(Block[]::new);

    public ModWoodModelProvider(PackOutput output) {
        super(output, ShadowsAndPetals.MOD_ID);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.concat(Stream.of(WOOD_BLOCKS), Stream.of(ROOF_TILE_SHAPE_BLOCKS))
                .map(Block::builtInRegistryHolder);
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.concat(Stream.of(WOOD_BLOCKS), Stream.of(ROOF_TILE_SHAPE_BLOCKS))
                .map(Block::asItem)
                .map(Item::builtInRegistryHolder);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        BlockRegistry.WOOD_SETS.forEach(woodSet -> registerWoodSet(blockModels, woodSet));
        for (DyeColor color : DyeColor.values()) {
            registerRoofTileShapeModels(blockModels, color);
        }
    }

    private static Stream<Block> blocksOf(WoodSetList.WoodSet woodSet) {
        return Arrays.stream(new Block[]{
                woodSet.log().get(),
                woodSet.strippedLog().get(),
                woodSet.wood().get(),
                woodSet.strippedWood().get(),
                woodSet.planks().get(),
                woodSet.slab().get(),
                woodSet.stairs().get(),
                woodSet.fence().get(),
                woodSet.fenceGate().get(),
                woodSet.pressurePlate().get(),
                woodSet.button().get()
        });
    }

    private void registerWoodSet(BlockModelGenerators blockModels, WoodSetList.WoodSet woodSet) {
        Block log = woodSet.log().get();
        Block strippedLog = woodSet.strippedLog().get();
        Block wood = woodSet.wood().get();
        Block strippedWood = woodSet.strippedWood().get();
        Block planks = woodSet.planks().get();
        Block slab = woodSet.slab().get();
        Block stairs = woodSet.stairs().get();
        Block fence = woodSet.fence().get();
        Block fenceGate = woodSet.fenceGate().get();
        Block pressurePlate = woodSet.pressurePlate().get();
        Block button = woodSet.button().get();

        blockModels.woodProvider(log).logWithHorizontal(log).wood(wood);
        blockModels.woodProvider(strippedLog).logWithHorizontal(strippedLog).wood(strippedWood);

        BlockFamily family = new BlockFamily.Builder(planks)
                .slab(slab)
                .stairs(stairs)
                .fence(fence)
                .fenceGate(fenceGate)
                .pressurePlate(pressurePlate)
                .button(button)
                .getFamily();
        blockModels.family(planks).generateFor(family);
    }

    private static void registerRoofTileShapeModels(BlockModelGenerators blockModels, DyeColor color) {
        Block baseBlock = BlockRegistry.ROOF_TILES.get(color).get();
        RoofTileSlabBlock slab = BlockRegistry.ROOF_TILE_SLABS.get(color).get();
        RoofTileVerticalSlabBlock verticalSlab = BlockRegistry.ROOF_TILE_VERTICAL_SLABS.get(color).get();
        StairBlock stairs = BlockRegistry.ROOF_TILE_STAIRS.get(color).get();
        TextureMapping mapping = roofTileMapping(color);

        Identifier slabBottom = ROOF_TILE_SLAB.create(slab, mapping, blockModels.modelOutput);
        MultiVariant slabTop = BlockModelGenerators.plainVariant(ROOF_TILE_SLAB_TOP.create(slab, mapping, blockModels.modelOutput));
        MultiVariant slabDouble = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(baseBlock));
        blockModels.blockStateOutput.accept(createRoofTileSlab(slab, BlockModelGenerators.plainVariant(slabBottom), slabTop, slabDouble));
        blockModels.registerSimpleItemModel(slab, slabBottom);

        Identifier verticalSlabModel = ROOF_TILE_VERTICAL_SLAB.create(verticalSlab, mapping, blockModels.modelOutput);
        MultiVariant verticalSlabSouth = BlockModelGenerators.plainVariant(
                ROOF_TILE_VERTICAL_SLAB_SOUTH.create(verticalSlab, mapping, blockModels.modelOutput)
        );
        MultiVariant verticalSlabWest = BlockModelGenerators.plainVariant(
                ROOF_TILE_VERTICAL_SLAB_WEST.create(verticalSlab, mapping, blockModels.modelOutput)
        );
        MultiVariant verticalSlabEast = BlockModelGenerators.plainVariant(
                ROOF_TILE_VERTICAL_SLAB_EAST.create(verticalSlab, mapping, blockModels.modelOutput)
        );
        blockModels.blockStateOutput.accept(createRoofTileVerticalSlab(
                verticalSlab,
                BlockModelGenerators.plainVariant(verticalSlabModel),
                verticalSlabSouth,
                verticalSlabWest,
                verticalSlabEast,
                BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(baseBlock))
        ));
        blockModels.registerSimpleItemModel(verticalSlab, verticalSlabModel);

        MultiVariant inner = BlockModelGenerators.plainVariant(ROOF_TILE_INNER_STAIRS.create(stairs, mapping, blockModels.modelOutput));
        Identifier straight = ROOF_TILE_STAIRS.create(stairs, mapping, blockModels.modelOutput);
        MultiVariant outer = BlockModelGenerators.plainVariant(ROOF_TILE_OUTER_STAIRS.create(stairs, mapping, blockModels.modelOutput));
        blockModels.blockStateOutput.accept(createRoofTileStairs(stairs, inner, BlockModelGenerators.plainVariant(straight), outer));
        blockModels.registerSimpleItemModel(stairs, straight);
    }

    private static BlockModelDefinitionGenerator createRoofTileStairs(
            Block block,
            MultiVariant inner,
            MultiVariant straight,
            MultiVariant outer
    ) {
        BlockModelDefinitionGenerator generator = BlockModelGenerators.createStairs(block, inner, straight, outer);
        if (generator instanceof MultiVariantGenerator multiVariantGenerator) {
            return multiVariantGenerator.with(VariantMutator.UV_LOCK.withValue(false));
        }
        throw new IllegalStateException("Unexpected stairs generator type: " + generator.getClass().getName());
    }

    private static BlockModelDefinitionGenerator createRoofTileSlab(
            Block block,
            MultiVariant bottom,
            MultiVariant top,
            MultiVariant full
    ) {
        return MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BlockStateProperties.SLAB_TYPE)
                        .select(SlabType.BOTTOM, bottom)
                        .select(SlabType.TOP, top)
                        .select(SlabType.DOUBLE, full))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING);
    }

    private static BlockModelDefinitionGenerator createRoofTileVerticalSlab(
            RoofTileVerticalSlabBlock block,
            MultiVariant north,
            MultiVariant south,
            MultiVariant west,
            MultiVariant east,
            MultiVariant full
    ) {
        return MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(VerticalSlabBlock.TYPE, RoofTileVerticalSlabBlock.FACING)
                        .generate((type, facing) -> type == VerticalSlabBlock.VerticalSlabType.DOUBLE
                                ? horizontalVariant(full, facing)
                                : horizontalVariant(switch (relativeType(type, facing)) {
                                    case NORTH -> north;
                                    case SOUTH -> south;
                                    case WEST -> west;
                                    case EAST -> east;
                                    case DOUBLE -> throw new IllegalStateException("A half slab cannot use the double model");
                                }, facing)));
    }

    private static VerticalSlabBlock.VerticalSlabType relativeType(
            VerticalSlabBlock.VerticalSlabType type,
            Direction facing
    ) {
        Rotation inverseFacingRotation = switch (facing) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.CLOCKWISE_90;
            default -> throw new IllegalArgumentException("Roof tile facing must be horizontal: " + facing);
        };
        return VerticalSlabBlock.VerticalSlabType.fromDirection(inverseFacingRotation.rotate(type.direction()));
    }

    private static MultiVariant horizontalVariant(MultiVariant model, Direction facing) {
        return switch (facing) {
            case NORTH -> model;
            case EAST -> model.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> model.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> model.with(BlockModelGenerators.Y_ROT_270);
            default -> throw new IllegalArgumentException("Roof tile facing must be horizontal: " + facing);
        };
    }

    private static TextureMapping roofTileMapping(DyeColor color) {
        Material texture = new Material(ShadowsAndPetals.asResource("block/roof_tile/" + color.getName()));
        return new TextureMapping()
                .put(TextureSlot.BOTTOM, texture)
                .put(TextureSlot.TOP, texture)
                .put(TextureSlot.SIDE, texture);
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Wood and Roof Tile Models";
    }
}
