package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.RoofTileSlabBlock;
import com.sshakusora.shadowsandpetals.block.decoration.RoofTileVerticalSlabBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VerticalSlabBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Optional;

public final class RoofTileModels {
    private static final ModelTemplate BLOCK = template("roof_tile_block", Optional.empty(), TextureSlot.ALL);
    private static final ModelTemplate SLAB = template("roof_tile_slab", Optional.empty(), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
    private static final ModelTemplate SLAB_TOP = template("roof_tile_slab_top", Optional.of("_top"), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
    private static final ModelTemplate VERTICAL_SLAB = template("roof_tile_vertical_slab", Optional.empty(), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
    private static final ModelTemplate VERTICAL_SLAB_SOUTH = template("roof_tile_vertical_slab_south", Optional.of("_south"), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
    private static final ModelTemplate VERTICAL_SLAB_WEST = template("roof_tile_vertical_slab_west", Optional.of("_west"), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
    private static final ModelTemplate VERTICAL_SLAB_EAST = template("roof_tile_vertical_slab_east", Optional.of("_east"), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
    private static final ModelTemplate STAIRS = template("roof_tile_stairs", Optional.empty(), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
    private static final ModelTemplate INNER_STAIRS = template("roof_tile_inner_stairs", Optional.of("_inner"), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
    private static final ModelTemplate OUTER_STAIRS = template("roof_tile_outer_stairs", Optional.of("_outer"), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);

    private RoofTileModels() {
    }

    public static void base(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator,
            Identifier texture
    ) {
        Block block = context.get();
        Identifier model = generator.create(
                BLOCK,
                generator.blockModelId(block),
                new TextureMapping().put(TextureSlot.ALL, new Material(texture))
        );
        generator.blockState(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(block, generator, model);
    }

    public static void shapes(
            BlockModelContext<? extends RoofTileSlabBlock> context,
            SAPBlockModelGenerator generator,
            DyeColor color
    ) {
        Block base = BlockRegistry.ROOF_TILES.get(color).get();
        RoofTileSlabBlock slab = context.get();
        RoofTileVerticalSlabBlock vertical = BlockRegistry.ROOF_TILE_VERTICAL_SLABS.get(color).get();
        StairBlock stairs = BlockRegistry.ROOF_TILE_STAIRS.get(color).get();
        TextureMapping mapping = mapping(color);

        Identifier slabBottom = generator.create(SLAB, slab, mapping);
        MultiVariant slabTop = BlockModelGenerators.plainVariant(generator.create(SLAB_TOP, slab, mapping));
        generator.blockState(createSlab(
                slab,
                BlockModelGenerators.plainVariant(slabBottom),
                slabTop,
                BlockModelGenerators.plainVariant(generator.blockModelId(base))
        ));
        generator.suggestItemModel(slab.asItem(), slabBottom);

        Identifier verticalModel = generator.create(VERTICAL_SLAB, vertical, mapping);
        generator.blockState(createVerticalSlab(
                vertical,
                BlockModelGenerators.plainVariant(verticalModel),
                BlockModelGenerators.plainVariant(generator.create(VERTICAL_SLAB_SOUTH, vertical, mapping)),
                BlockModelGenerators.plainVariant(generator.create(VERTICAL_SLAB_WEST, vertical, mapping)),
                BlockModelGenerators.plainVariant(generator.create(VERTICAL_SLAB_EAST, vertical, mapping)),
                BlockModelGenerators.plainVariant(generator.blockModelId(base))
        ));
        generator.suggestItemModel(vertical.asItem(), verticalModel);

        MultiVariant inner = BlockModelGenerators.plainVariant(generator.create(INNER_STAIRS, stairs, mapping));
        Identifier straight = generator.create(STAIRS, stairs, mapping);
        MultiVariant outer = BlockModelGenerators.plainVariant(generator.create(OUTER_STAIRS, stairs, mapping));
        generator.blockState(createStairs(stairs, inner, BlockModelGenerators.plainVariant(straight), outer));
        generator.suggestItemModel(stairs.asItem(), straight);
    }

    private static BlockModelDefinitionGenerator createStairs(
            Block block,
            MultiVariant inner,
            MultiVariant straight,
            MultiVariant outer
    ) {
        BlockModelDefinitionGenerator definition = BlockModelGenerators.createStairs(block, inner, straight, outer);
        if (definition instanceof MultiVariantGenerator variants) {
            return variants.with(VariantMutator.UV_LOCK.withValue(false));
        }
        throw new IllegalStateException("Unexpected stairs generator type: " + definition.getClass().getName());
    }

    private static BlockModelDefinitionGenerator createSlab(
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

    private static BlockModelDefinitionGenerator createVerticalSlab(
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
                                ? horizontal(full, facing)
                                : horizontal(switch (relativeType(type, facing)) {
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
        Rotation inverse = switch (facing) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.CLOCKWISE_90;
            default -> throw new IllegalArgumentException("Roof tile facing must be horizontal: " + facing);
        };
        return VerticalSlabBlock.VerticalSlabType.fromDirection(inverse.rotate(type.direction()));
    }

    private static MultiVariant horizontal(MultiVariant model, Direction facing) {
        return switch (facing) {
            case NORTH -> model;
            case EAST -> model.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> model.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> model.with(BlockModelGenerators.Y_ROT_270);
            default -> throw new IllegalArgumentException("Roof tile facing must be horizontal: " + facing);
        };
    }

    private static TextureMapping mapping(DyeColor color) {
        Material texture = new Material(ShadowsAndPetals.asResource("block/roof_tile/" + color.getName()));
        return new TextureMapping()
                .put(TextureSlot.BOTTOM, texture)
                .put(TextureSlot.TOP, texture)
                .put(TextureSlot.SIDE, texture);
    }

    private static ModelTemplate template(String parent, Optional<String> suffix, TextureSlot... slots) {
        return new ModelTemplate(
                Optional.of(ShadowsAndPetals.asResource("block/template/" + parent)),
                suffix,
                slots
        );
    }
}
