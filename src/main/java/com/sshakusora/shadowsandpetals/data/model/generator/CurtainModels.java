package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.CurtainBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Datagen for the experimental two-block curtain. The block-state models
 * only feed the item model; the placed blocks are rendered by
 * {@code CurtainBlockEntityRenderer} through the per-half, per-side
 * animation rigs.
 */
public final class CurtainModels {
    private CurtainModels() {
    }

    public static void block(
            BlockModelContext<? extends CurtainBlock> context,
            SAPBlockModelGenerator generator
    ) {
        CurtainBlock block = context.get();
        String path = context.id().getPath();
        String color = path.endsWith("_curtain")
                ? path.substring(0, path.length() - "_curtain".length())
                : "white";
        String variantSuffix = color.equals("white") ? "" : "_" + color;
        MultiVariant upperRight = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_upper_r" + variantSuffix));
        MultiVariant lowerRight = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_lower_r" + variantSuffix));
        MultiVariant upperLeft = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_upper_l" + variantSuffix));
        MultiVariant lowerLeft = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_lower_l" + variantSuffix));
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(CurtainBlock.HALF, CurtainBlock.SIDE)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.RIGHT, upperRight)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.LEFT, upperLeft)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.RIGHT, lowerRight)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.LEFT, lowerLeft))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/curtain/" + color + "_curtain")
        );
    }
}