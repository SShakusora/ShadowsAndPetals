package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.agriculture.OrangeTreeBlock;
import com.sshakusora.shadowsandpetals.block.decoration.HedgeBlock;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.block.nature.SandExcavationBlock;
import com.sshakusora.shadowsandpetals.data.model.*;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;

public final class NatureBlockModels {
    private NatureBlockModels() {
    }

    public static void sandExcavation(
            BlockModelContext<? extends SandExcavationBlock> context,
            SAPBlockModelGenerator generator
    ) {
        generator.blockState(MultiVariantGenerator.dispatch(context.get())
                .with(PropertyDispatch.initial(SandExcavationBlock.DUSTED)
                        .select(0, BlockModelGenerators.plainVariant(generator.mcLoc("block/suspicious_sand_0")))
                        .select(1, BlockModelGenerators.plainVariant(generator.mcLoc("block/suspicious_sand_1")))
                        .select(2, BlockModelGenerators.plainVariant(generator.mcLoc("block/suspicious_sand_2")))
                        .select(3, BlockModelGenerators.plainVariant(generator.mcLoc("block/suspicious_sand_3")))));
    }

    public static void leaves(
            BlockModelContext<? extends LeavesBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture
    ) {
        StandardBlockModels.cubeAll(context, generator, texture);
    }

    public static void leavesSlab(
            BlockModelContext<? extends SlabBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture
    ) {
        StandardBlockModels.slab(context, generator, texture, true);
    }

    public static void leavesStairs(
            BlockModelContext<? extends StairBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture
    ) {
        StandardBlockModels.stairs(context, generator, texture, true);
    }

    public static void sapling(
            BlockModelContext<? extends SaplingBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture
    ) {
        Identifier modelId = generator.blockModelId(context.get());
        generator.create(
                ModelTemplates.CROSS,
                modelId,
                new TextureMapping().put(TextureSlot.CROSS, new Material(texture)),
                "cutout"
        );
        StandardBlockModels.simpleBlock(context, generator, modelId);
    }

    public static void saplingItem(
            ItemModelContext<? extends BlockItem> context,
            SAPItemModelGenerator generator,
            Identifier texture
    ) {
        generator.generatedItem(context.get(), texture);
    }

    public static void hedge(
            BlockModelContext<? extends HedgeBlock> context,
            SAPBlockModelGenerator generator,
            Identifier texture
    ) {
        HedgeBlock block = context.get();
        for (int mask = 0; mask < 16; mask++) {
            boolean north = (mask & 1) != 0;
            boolean east = (mask & 1 << 1) != 0;
            boolean south = (mask & 1 << 2) != 0;
            boolean west = (mask & 1 << 3) != 0;
            generator.jsonModel(
                    generator.modLoc("block/" + context.name() + "_" + mask),
                    BlockModelTemplates.hedgeStateModel(texture, north, east, south, west)
            );
        }
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(
                                HedgeBlock.NORTH,
                                HedgeBlock.EAST,
                                HedgeBlock.SOUTH,
                                HedgeBlock.WEST,
                                HedgeBlock.WATERLOGGED
                        )
                        .generate((north, east, south, west, waterlogged) -> BlockModelGenerators.plainVariant(
                                hedgeModel(generator, context.name(), north, east, south, west)
                        ))));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/" + context.name() + "_5")
        );
    }

    public static void orangeTree(
            BlockModelContext<? extends OrangeTreeBlock> context,
            SAPBlockModelGenerator generator
    ) {
        OrangeTreeBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(OrangeTreeBlock.AGE, OrangeTreeBlock.FACING, OrangeTreeBlock.HALF)
                        .generate((age, facing, half) -> {
                            String modelName = age < OrangeTreeBlock.DOUBLE_HEIGHT_AGE
                                    ? "tree_" + age
                                    : "tree_" + age + "_" + half.getSerializedName();
                            return placementFacingVariant(generator.modLoc("block/orange/" + modelName), facing);
                        })));
    }

    public static void rockery(
            BlockModelContext<? extends RockeryBlock> context,
            SAPBlockModelGenerator generator,
            RockeryDimensions dimensions
    ) {
        RockeryBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(RockeryBlock.FACING, RockeryBlock.PART, RockeryBlock.WATERLOGGED)
                        .generate((facing, part, waterlogged) -> {
                            Vec3i position = dimensions.localPos(part < dimensions.partCount() ? part : 0);
                            Identifier model = generator.modLoc(dimensions.modelDir()
                                    + "/" + position.getX() + "_" + position.getY() + "_" + position.getZ());
                            return modelYawVariant(model, facing);
                        })));
    }

    private static Identifier hedgeModel(
            SAPBlockModelGenerator generator,
            String name,
            boolean north,
            boolean east,
            boolean south,
            boolean west
    ) {
        int mask = (north ? 1 : 0)
                | (east ? 1 << 1 : 0)
                | (south ? 1 << 2 : 0)
                | (west ? 1 << 3 : 0);
        return generator.modLoc("block/" + name + "_" + mask);
    }

    private static MultiVariant placementFacingVariant(Identifier model, Direction facing) {
        MultiVariant variant = BlockModelGenerators.plainVariant(model);
        return switch (facing) {
            case EAST -> variant.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> variant.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> variant.with(BlockModelGenerators.Y_ROT_270);
            default -> variant;
        };
    }

    private static MultiVariant modelYawVariant(Identifier model, Direction facing) {
        MultiVariant variant = BlockModelGenerators.plainVariant(model);
        return switch (facing) {
            case EAST -> variant.with(BlockModelGenerators.Y_ROT_270);
            case WEST -> variant.with(BlockModelGenerators.Y_ROT_90);
            case NORTH -> variant.with(BlockModelGenerators.Y_ROT_180);
            default -> variant;
        };
    }
}
