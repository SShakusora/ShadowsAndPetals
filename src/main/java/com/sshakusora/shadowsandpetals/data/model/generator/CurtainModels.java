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
 * Datagen for the two-block curtain. Outside the animation window the placed
 * blocks render as plain block-state models: the closed pose or the baked
 * open pose. While ANIMATING the render shape is INVISIBLE and
 * {@code CurtainBlockEntityRenderer} owns the pose.
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
        MultiVariant upperRightClosed = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_upper_r" + variantSuffix));
        MultiVariant lowerRightClosed = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_lower_r" + variantSuffix));
        MultiVariant upperLeftClosed = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_upper_l" + variantSuffix));
        MultiVariant lowerLeftClosed = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_lower_l" + variantSuffix));
        MultiVariant upperRightOpen = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_upper_r_open" + variantSuffix));
        MultiVariant lowerRightOpen = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_lower_r_open" + variantSuffix));
        MultiVariant upperLeftOpen = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_upper_l_open" + variantSuffix));
        MultiVariant lowerLeftOpen = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_lower_l_open" + variantSuffix));
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(CurtainBlock.HALF, CurtainBlock.SIDE, CurtainBlock.OPEN, CurtainBlock.ANIMATING)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.RIGHT, true, true, upperRightOpen)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.RIGHT, true, false, upperRightOpen)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.RIGHT, false, true, upperRightClosed)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.RIGHT, false, false, upperRightClosed)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.LEFT, true, true, upperLeftOpen)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.LEFT, true, false, upperLeftOpen)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.LEFT, false, true, upperLeftClosed)
                        .select(DoubleBlockHalf.UPPER, CurtainBlock.Side.LEFT, false, false, upperLeftClosed)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.RIGHT, true, true, lowerRightOpen)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.RIGHT, true, false, lowerRightOpen)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.RIGHT, false, true, lowerRightClosed)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.RIGHT, false, false, lowerRightClosed)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.LEFT, true, true, lowerLeftOpen)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.LEFT, true, false, lowerLeftOpen)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.LEFT, false, true, lowerLeftClosed)
                        .select(DoubleBlockHalf.LOWER, CurtainBlock.Side.LEFT, false, false, lowerLeftClosed))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/curtain/" + color + "_curtain")
        );
    }
}