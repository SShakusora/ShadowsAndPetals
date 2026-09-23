package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.sofa.SofaBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.core.Direction;

/** Blockstate and item-model generation for the modular sofa meshes. */
public final class SofaModels {
    private SofaModels() {
    }

    public static void block(
            BlockModelContext<? extends SofaBlock> context,
            SAPBlockModelGenerator generator
    ) {
        SofaBlock block = context.get();
        String path = context.id().getPath();
        String color = path.substring(0, path.length() - "_sofa".length());

        PropertyDispatch<MultiVariant> dispatch = PropertyDispatch
                .initial(SofaBlock.FACING, SofaBlock.SHAPE)
                .generate((facing, shape) -> model(generator, color, facing, shape));

        generator.blockState(MultiVariantGenerator.dispatch(block).with(dispatch));

        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/sofa/" + color)
        );
    }

    private static MultiVariant model(
            SAPBlockModelGenerator generator,
            String color,
            Direction facing,
            SofaBlock.SofaShape shape
    ) {
        String modelName = shape.modelSuffix().isEmpty()
                ? color
                : color + "_" + shape.modelSuffix();

        MultiVariant variant = BlockModelGenerators.plainVariant(
                generator.modLoc("block/sofa/" + modelName));
        return rotate(variant, shape.modelRotationDegrees(facing));
    }

    private static MultiVariant rotate(MultiVariant variant, int degrees) {
        return switch (degrees % 360) {
            case 90 -> variant.with(BlockModelGenerators.Y_ROT_90);
            case 180 -> variant.with(BlockModelGenerators.Y_ROT_180);
            case 270 -> variant.with(BlockModelGenerators.Y_ROT_270);
            default -> variant;
        };
    }

}
