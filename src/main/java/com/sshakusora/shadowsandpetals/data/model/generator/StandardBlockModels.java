package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.VerticalSlabBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Optional;

public final class StandardBlockModels {
    private static final ModelTemplate VERTICAL_SLAB = new ModelTemplate(
            Optional.of(ShadowsAndPetals.asResource("block/template/vertical_slab")),
            Optional.empty(),
            TextureSlot.BOTTOM,
            TextureSlot.TOP,
            TextureSlot.SIDE
    );

    private StandardBlockModels() {
    }

    public static void cubeAll(BlockModelContext<? extends Block> context, SAPBlockModelGenerator generator) {
        cubeAll(context, generator, generator.modLoc("block/" + context.name()));
    }

    public static void cubeAll(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator,
            Identifier texture
    ) {
        Block block = context.get();
        Identifier modelId = generator.blockModelId(block);
        generator.create(
                ModelTemplates.CUBE_ALL,
                modelId,
                new TextureMapping().put(TextureSlot.ALL, new Material(texture))
        );
        simpleBlockWithItem(context, generator, modelId);
    }

    public static void simpleBlock(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator,
            Identifier modelId
    ) {
        generator.blockState(BlockModelGenerators.createSimpleBlock(
                context.get(),
                BlockModelGenerators.plainVariant(modelId)
        ));
    }

    public static void simpleBlockWithItem(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator,
            Identifier modelId
    ) {
        simpleBlock(context, generator, modelId);
        parentBlockItem(context.get(), generator, modelId);
    }

    public static void simpleWaterloggedBlockWithItem(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator,
            Identifier modelId
    ) {
        Block block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BlockStateProperties.WATERLOGGED)
                        .select(false, BlockModelGenerators.plainVariant(modelId))
                        .select(true, BlockModelGenerators.plainVariant(modelId))));
        parentBlockItem(block, generator, modelId);
    }

    public static void horizontalFacingCubeAll(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator,
            Identifier texture
    ) {
        Block block = context.get();
        Identifier modelId = generator.blockModelId(block);
        generator.create(
                ModelTemplates.CUBE_ALL,
                modelId,
                new TextureMapping().put(TextureSlot.ALL, new Material(texture))
        );
        generator.blockState(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(modelId))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        parentBlockItem(block, generator, modelId);
    }

    public static void fluid(
            BlockModelContext<? extends LiquidBlock> context,
            SAPBlockModelGenerator generator,
            Identifier particleTexture
    ) {
        Identifier modelId = generator.blockModelId(context.get());
        generator.create(
                new ModelTemplate(Optional.empty(), Optional.empty(), TextureSlot.PARTICLE),
                modelId,
                new TextureMapping().put(TextureSlot.PARTICLE, new Material(particleTexture))
        );
        simpleBlock(context, generator, modelId);
    }

    public static void verticalSlab(
            BlockModelContext<? extends VerticalSlabBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture,
            Block doubleBlock,
            boolean cutoutMipped
    ) {
        verticalSlab(context, generator, texture, generator.blockModelId(doubleBlock), cutoutMipped);
    }

    public static void verticalSlab(
            BlockModelContext<? extends VerticalSlabBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture,
            Identifier doubleModel,
            boolean cutoutMipped
    ) {
        VerticalSlabBlock block = context.get();
        TextureMapping mapping = shapeTextureMapping(texture);
        Identifier modelId = cutoutMipped
                ? generator.create(VERTICAL_SLAB, block, mapping, "cutout_mipped")
                : generator.create(VERTICAL_SLAB, block, mapping);
        MultiVariant model = BlockModelGenerators.plainVariant(modelId).with(BlockModelGenerators.UV_LOCK);
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(VerticalSlabBlock.TYPE)
                        .select(VerticalSlabBlock.VerticalSlabType.NORTH, model)
                        .select(VerticalSlabBlock.VerticalSlabType.SOUTH, model.with(BlockModelGenerators.Y_ROT_180))
                        .select(VerticalSlabBlock.VerticalSlabType.EAST, model.with(BlockModelGenerators.Y_ROT_90))
                        .select(VerticalSlabBlock.VerticalSlabType.WEST, model.with(BlockModelGenerators.Y_ROT_270))
                        .select(
                                VerticalSlabBlock.VerticalSlabType.DOUBLE,
                                BlockModelGenerators.plainVariant(doubleModel)
                        )));
        parentBlockItem(block, generator, modelId);
    }

    public static void slab(
            BlockModelContext<? extends SlabBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture,
            boolean cutoutMipped
    ) {
        SlabBlock block = context.get();
        TextureMapping mapping = shapeTextureMapping(texture);
        Identifier bottom = create(generator, ModelTemplates.SLAB_BOTTOM, block, mapping, cutoutMipped);
        Identifier top = create(generator, ModelTemplates.SLAB_TOP, block, mapping, cutoutMipped);
        Identifier full = generator.modLoc("block/" + context.name() + "_double");
        if (cutoutMipped) {
            generator.create(
                    ModelTemplates.CUBE_ALL,
                    full,
                    new TextureMapping().put(TextureSlot.ALL, new Material(texture)),
                    "cutout_mipped"
            );
        } else {
            generator.create(
                    ModelTemplates.CUBE_ALL,
                    full,
                    new TextureMapping().put(TextureSlot.ALL, new Material(texture))
            );
        }
        generator.blockState(BlockModelGenerators.createSlab(
                block,
                BlockModelGenerators.plainVariant(bottom),
                BlockModelGenerators.plainVariant(top),
                BlockModelGenerators.plainVariant(full)
        ));
        parentBlockItem(block, generator, bottom);
    }

    public static void stairs(
            BlockModelContext<? extends StairBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture,
            boolean cutoutMipped
    ) {
        StairBlock block = context.get();
        TextureMapping mapping = shapeTextureMapping(texture);
        Identifier straight = create(generator, ModelTemplates.STAIRS_STRAIGHT, block, mapping, cutoutMipped);
        Identifier inner = create(generator, ModelTemplates.STAIRS_INNER, block, mapping, cutoutMipped);
        Identifier outer = create(generator, ModelTemplates.STAIRS_OUTER, block, mapping, cutoutMipped);
        generator.blockState(BlockModelGenerators.createStairs(
                block,
                BlockModelGenerators.plainVariant(inner),
                BlockModelGenerators.plainVariant(straight),
                BlockModelGenerators.plainVariant(outer)
        ));
        parentBlockItem(block, generator, straight);
    }

    public static void parentBlockItem(Block block, SAPBlockModelGenerator generator, Identifier parent) {
        if (block.asItem() == Items.AIR) {
            return;
        }
        Identifier itemModel = generator.itemModelId(block.asItem());
        generator.parentModel(itemModel, parent);
        generator.suggestItemModel(block.asItem(), itemModel);
    }

    private static Identifier create(
            SAPBlockModelGenerator generator,
            ModelTemplate template,
            Block block,
            TextureMapping mapping,
            boolean cutoutMipped
    ) {
        return cutoutMipped
                ? generator.create(template, block, mapping, "cutout_mipped")
                : generator.create(template, block, mapping);
    }

    private static TextureMapping shapeTextureMapping(Identifier texture) {
        Material material = new Material(texture);
        return new TextureMapping()
                .put(TextureSlot.BOTTOM, material)
                .put(TextureSlot.TOP, material)
                .put(TextureSlot.SIDE, material);
    }
}
