package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.sofa.SofaBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

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

        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> {
            Direction facing = state.getValue(SofaBlock.FACING);
            SofaBlock.SofaShape shape = state.getValue(SofaBlock.SHAPE);
            String modelName = shape.modelSuffix().isEmpty()
                    ? color
                    : color + "_" + shape.modelSuffix();
            return ConfiguredModel.builder()
                    .modelFile(generator.uncheckedModel(
                            generator.modLoc("block/sofa/" + modelName)))
                    .rotationY(Math.floorMod(shape.modelRotationDegrees(facing), 360))
                    .build();
        }, SofaBlock.WATERLOGGED);

        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/sofa/" + color)
        );
    }
}
