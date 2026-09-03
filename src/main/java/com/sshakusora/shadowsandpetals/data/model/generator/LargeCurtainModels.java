package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.LargeCurtainBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Datagen for the four-block large curtain. Outside the animation window the
 * placed blocks render as plain block-state models: the closed pose or the
 * baked open pose, per (row, column, side). While ANIMATING the render shape
 * is INVISIBLE and {@code LargeCurtainBlockEntityRenderer} owns the pose.
 */
public final class LargeCurtainModels {
    private LargeCurtainModels() {
    }

    public static void block(
            BlockModelContext<? extends LargeCurtainBlock> context,
            SAPBlockModelGenerator generator
    ) {
        LargeCurtainBlock block = context.get();
        String path = context.id().getPath();
        String color = path.endsWith("_large_curtain")
                ? path.substring(0, path.length() - "_large_curtain".length())
                : "white";
        String variantSuffix = color.equals("white") ? "" : "_" + color;
        PropertyDispatch<MultiVariant> dispatch = PropertyDispatch.initial(
                        LargeCurtainBlock.HALF,
                        LargeCurtainBlock.COLUMN,
                        LargeCurtainBlock.SIDE,
                        LargeCurtainBlock.OPEN,
                        LargeCurtainBlock.ANIMATING)
                .generate((half, column, side, open, animating) -> {
                    // The authored assets are the RIGHT curtain; the LEFT
                    // partner's models are the mirrored variants.
                    LargeCurtainBlock.Column modelColumn = side == LargeCurtainBlock.Side.LEFT
                            ? column.mirror()
                            : column;
                    String row = half == DoubleBlockHalf.UPPER ? "upper" : "lower";
                    String columnName = modelColumn == LargeCurtainBlock.Column.OUTER ? "outer" : "inner";
                    String suffix = row + "_" + columnName + "_" + side.getSerializedName()
                            + (open ? "_open" : "") + variantSuffix;
                    return BlockModelGenerators.plainVariant(
                            generator.modLoc("block/large_curtain/large_curtain_" + suffix));
                });
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(dispatch)
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/large_curtain/large_" + color + "_curtain")
        );
    }
}