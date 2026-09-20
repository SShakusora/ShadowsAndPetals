package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.agriculture.OrangeTreeBlock;
import com.sshakusora.shadowsandpetals.block.decoration.HedgeBlock;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.block.nature.SandExcavationBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.ItemModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import com.sshakusora.shadowsandpetals.data.model.SAPItemModelGenerator;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.Map;

public final class NatureBlockModels {
    private NatureBlockModels() {}
    public static void sandExcavation(BlockModelContext<? extends SandExcavationBlock> context, SAPBlockModelGenerator generator) {
        generator.provider().getVariantBuilder(context.get()).forAllStates(state -> {
            int dusted = state.getValue(SandExcavationBlock.DUSTED);
            return ConfiguredModel.builder().modelFile(generator.uncheckedModel(
                    ResourceLocation.withDefaultNamespace("block/suspicious_sand_" + dusted))).build();
        });
    }
    public static void leaves(BlockModelContext<? extends LeavesBlock> context, SAPBlockModelGenerator generator,
                              ResourceLocation texture) {
        String base = generator.blockModelId(context.get()).getPath();
        ModelFile[] models = new ModelFile[4];
        for (int i = 0; i < models.length; i++) {
            models[i] = generator.provider().models().leaves(base + "_" + i, texture.withSuffix("_" + i));
            ((BlockModelBuilder) models[i]).renderType("cutout_mipped");
        }
        var variants = generator.provider().getVariantBuilder(context.get());
        variants.partialState().with(LeavesBlock.PERSISTENT, false).addModels(
                ConfiguredModel.builder().modelFile(models[0]).weight(1).buildLast(),
                ConfiguredModel.builder().modelFile(models[1]).weight(1).buildLast(),
                ConfiguredModel.builder().modelFile(models[2]).weight(1).buildLast(),
                ConfiguredModel.builder().modelFile(models[3]).weight(1).buildLast());
        variants.partialState().with(LeavesBlock.PERSISTENT, true).addModels(
                ConfiguredModel.builder().modelFile(models[0]).buildLast());
        StandardBlockModels.parentBlockItem(context.get(), generator,
                generator.blockModelId(context.get()).withSuffix("_0"));
    }
    public static void leavesSlab(BlockModelContext<? extends SlabBlock> context, SAPBlockModelGenerator generator,
                                  ResourceLocation texture) {
        StandardBlockModels.slab(context, generator, texture, true);
    }

    public static void leavesCarpet(BlockModelContext<? extends CarpetBlock> context,
                                    SAPBlockModelGenerator generator,
                                    ResourceLocation texture) {
        ResourceLocation modelId = generator.blockModelId(context.get()).withSuffix("_0");
        ModelFile model = generator.createModel(
                modelId.getPath(),
                ResourceLocation.withDefaultNamespace("block/carpet"),
                Map.of("wool", texture.withSuffix("_0")),
                "cutout_mipped"
        );
        generator.provider().getVariantBuilder(context.get())
                .partialState()
                .addModels(ConfiguredModel.builder().modelFile(model).buildLast());
        StandardBlockModels.parentBlockItem(
                context.get(),
                generator,
                modelId
        );
    }
    public static void leavesStairs(BlockModelContext<? extends StairBlock> context, SAPBlockModelGenerator generator,
                                    ResourceLocation texture) {
        StandardBlockModels.stairs(context, generator, texture, true);
    }
    public static void sapling(BlockModelContext<? extends SaplingBlock> context, SAPBlockModelGenerator generator,
                               ResourceLocation texture) {
        BlockModelBuilder model = generator.provider().models().cross(context.name(), texture);
        model.renderType("cutout");
        generator.provider().simpleBlock(context.get(), model);
    }
    public static void hedge(BlockModelContext<? extends HedgeBlock> context, SAPBlockModelGenerator generator,
                             ResourceLocation texture) {
        for (int mask = 0; mask < 16; mask++) {
            generator.createHedgeModel("block/" + context.name() + "_" + mask, texture,
                    (mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0);
        }
        generator.provider().getVariantBuilder(context.get()).forAllStates(state -> {
            int mask = 0;
            if (state.getValue(HedgeBlock.NORTH)) mask |= 1;
            if (state.getValue(HedgeBlock.EAST)) mask |= 2;
            if (state.getValue(HedgeBlock.SOUTH)) mask |= 4;
            if (state.getValue(HedgeBlock.WEST)) mask |= 8;
            return ConfiguredModel.builder().modelFile(generator.uncheckedModel(
                    generator.modLoc("block/" + context.name() + "_" + mask))).build();
        });
        StandardBlockModels.parentBlockItem(context.get(), generator,
                generator.modLoc("block/" + context.name() + "_5"));
    }
    public static void orangeTree(BlockModelContext<? extends OrangeTreeBlock> context, SAPBlockModelGenerator generator) {
        generator.provider().getVariantBuilder(context.get()).forAllStates(state -> {
            int age = state.getValue(OrangeTreeBlock.AGE);
            String name = age < OrangeTreeBlock.DOUBLE_HEIGHT_AGE
                    ? "tree_" + age : "tree_" + age + "_" + state.getValue(OrangeTreeBlock.HALF).getSerializedName();
            Direction facing = state.getValue(OrangeTreeBlock.FACING);
            int y = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
            return ConfiguredModel.builder().modelFile(generator.uncheckedModel(
                    generator.modLoc("block/orange/" + name))).rotationY(y).build();
        });
        StandardBlockModels.parentBlockItem(context.get(), generator, generator.modLoc("block/orange/tree_0"));
    }
    public static void rockery(BlockModelContext<? extends RockeryBlock> context, SAPBlockModelGenerator generator,
                               RockeryDimensions dimensions) {
        generator.provider().getVariantBuilder(context.get()).forAllStates(state -> {
            int part = state.getValue(RockeryBlock.PART);
            Vec3i position = dimensions.localPos(part < dimensions.partCount() ? part : 0);
            Direction facing = state.getValue(RockeryBlock.FACING);
            int y = switch (facing) { case EAST -> 270; case WEST -> 90; case NORTH -> 180; default -> 0; };
            return ConfiguredModel.builder().modelFile(generator.uncheckedModel(generator.modLoc(
                    dimensions.modelDir() + "/" + position.getX() + "_" + position.getY() + "_" + position.getZ())))
                    .rotationY(y).build();
        });
    }
    public static void saplingItem(ItemModelContext<? extends BlockItem> context, SAPItemModelGenerator generator,
                                   ResourceLocation texture) {
        generator.generatedItem(context.get(), texture);
    }
}
