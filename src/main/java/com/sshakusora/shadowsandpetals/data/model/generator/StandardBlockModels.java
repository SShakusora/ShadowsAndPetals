package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.VerticalSlabBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.Map;

public final class StandardBlockModels {
    private StandardBlockModels() {}

    public static void cubeAll(BlockModelContext<? extends Block> context, SAPBlockModelGenerator generator) {
        cubeAll(context, generator, generator.modLoc("block/" + context.name()));
    }

    public static void cubeAll(BlockModelContext<? extends Block> context,
                               SAPBlockModelGenerator generator, ResourceLocation texture) {
        Block block = context.get();
        ModelFile model = generator.provider().models().cubeAll(context.name(), texture);
        generator.provider().simpleBlockWithItem(block, model);
    }

    /**
     * Generates a cube block model and a separate cube model for its item.
     * This is useful when the placed block is backed by a connected-texture
     * model but the inventory icon should use a fixed preview texture.
     */
    public static void cubeAllWithItemTexture(BlockModelContext<? extends Block> context,
                                              SAPBlockModelGenerator generator,
                                              ResourceLocation blockTexture,
                                              ResourceLocation itemTexture) {
        Block block = context.get();
        ModelFile blockModel = generator.provider().models().cubeAll(context.name(), blockTexture);
        generator.provider().simpleBlock(block, blockModel);

        String itemModelName = context.name() + "_item";
        generator.provider().models().cubeAll(itemModelName, itemTexture);
        generator.simpleBlockItem(block, generator.modLoc("block/" + itemModelName));
    }

    public static void simpleBlock(BlockModelContext<? extends Block> context,
                                   SAPBlockModelGenerator generator, ResourceLocation modelId) {
        generator.provider().simpleBlock(context.get(), generator.uncheckedModel(modelId));
    }

    public static void simpleBlockWithItem(BlockModelContext<? extends Block> context,
                                           SAPBlockModelGenerator generator, ResourceLocation modelId) {
        simpleBlock(context, generator, modelId);
        parentBlockItem(context.get(), generator, modelId);
    }

    public static void simpleWaterloggedBlockWithItem(BlockModelContext<? extends Block> context,
                                                      SAPBlockModelGenerator generator,
                                                      ResourceLocation modelId) {
        generator.provider().getVariantBuilder(context.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(generator.uncheckedModel(modelId)).build());
        parentBlockItem(context.get(), generator, modelId);
    }

    public static void horizontalFacingCubeAll(BlockModelContext<? extends Block> context,
                                               SAPBlockModelGenerator generator,
                                               ResourceLocation texture) {
        Block block = context.get();
        ModelFile model = generator.provider().models().cubeAll(context.name(), texture);
        generator.provider().horizontalBlock(block, model);
        parentBlockItem(block, generator, generator.blockModelId(block));
    }

    public static void fluid(BlockModelContext<? extends LiquidBlock> context,
                             SAPBlockModelGenerator generator, ResourceLocation particleTexture) {
        ResourceLocation modelId = generator.blockModelId(context.get());
        generator.createModel(modelId.getPath(), null, Map.of("particle", particleTexture), null);
        simpleBlock(context, generator, modelId);
    }

    public static void verticalSlab(BlockModelContext<? extends VerticalSlabBlock> context,
                                    SAPBlockModelGenerator generator, ResourceLocation texture,
                                    Block doubleBlock, boolean cutoutMipped) {
        verticalSlab(context, generator, texture, generator.blockModelId(doubleBlock), cutoutMipped);
    }

    public static void verticalSlab(BlockModelContext<? extends VerticalSlabBlock> context,
                                    SAPBlockModelGenerator generator, ResourceLocation texture,
                                    ResourceLocation doubleModel, boolean cutoutMipped) {
        VerticalSlabBlock block = context.get();
        ResourceLocation modelId = generator.blockModelId(block);
        generator.createModel(modelId.getPath(), generator.modLoc("block/template/vertical_slab"),
                Map.of("bottom", texture, "top", texture, "side", texture),
                cutoutMipped ? "cutout_mipped" : null);
        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> {
            VerticalSlabBlock.VerticalSlabType type = state.getValue(VerticalSlabBlock.TYPE);
            int y = switch (type) {
                case NORTH -> 0;
                case SOUTH -> 180;
                case EAST -> 90;
                case WEST -> 270;
                case DOUBLE -> 0;
            };
            ModelFile selected = type == VerticalSlabBlock.VerticalSlabType.DOUBLE
                    ? generator.uncheckedModel(doubleModel) : generator.uncheckedModel(modelId);
            return ConfiguredModel.builder().modelFile(selected).rotationY(y).uvLock(true).build();
        }, VerticalSlabBlock.WATERLOGGED);
        parentBlockItem(block, generator, modelId);
    }

    public static void slab(BlockModelContext<? extends SlabBlock> context,
                            SAPBlockModelGenerator generator, ResourceLocation texture,
                            boolean cutoutMipped) {
        SlabBlock block = context.get();
        var models = generator.provider().models();
        var bottom = models.slab(context.name(), texture, texture, texture);
        var top = models.slabTop(context.name() + "_top", texture, texture, texture);
        var full = models.cubeAll(context.name() + "_double", texture);
        if (cutoutMipped) {
            ((BlockModelBuilder) bottom).renderType("cutout_mipped");
            ((BlockModelBuilder) top).renderType("cutout_mipped");
            ((BlockModelBuilder) full).renderType("cutout_mipped");
        }
        generator.provider().slabBlock(block, bottom, top, full);
        parentBlockItem(block, generator, generator.modLoc("block/" + context.name()));
    }

    public static void stairs(BlockModelContext<? extends StairBlock> context,
                              SAPBlockModelGenerator generator, ResourceLocation texture,
                              boolean cutoutMipped) {
        StairBlock block = context.get();
        var models = generator.provider().models();
        var straight = models.stairs(context.name(), texture, texture, texture);
        var inner = models.stairsInner(context.name() + "_inner", texture, texture, texture);
        var outer = models.stairsOuter(context.name() + "_outer", texture, texture, texture);
        if (cutoutMipped) {
            ((BlockModelBuilder) straight).renderType("cutout_mipped");
            ((BlockModelBuilder) inner).renderType("cutout_mipped");
            ((BlockModelBuilder) outer).renderType("cutout_mipped");
        }
        generator.provider().stairsBlock(block, straight, inner, outer);
        parentBlockItem(block, generator, generator.modLoc("block/" + context.name()));
    }

    public static void parentBlockItem(Block block, SAPBlockModelGenerator generator,
                                       ResourceLocation model) {
        if (block.asItem() != Items.AIR) {
            generator.simpleBlockItem(block, model);
        }
    }

}
