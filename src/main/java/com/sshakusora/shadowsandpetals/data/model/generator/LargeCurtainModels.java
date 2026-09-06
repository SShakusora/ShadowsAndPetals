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
 * Datagen for the four-block large curtain. Outside the animation window
 * each block renders its own hand-authored quadrant model: the closed pose
 * or the baked open pose, per (row, column). While ANIMATING the render
 * shape is INVISIBLE everywhere except the anchor, whose
 * {@code LargeCurtainBlockEntityRenderer} draws the whole rig.
 */
public final class LargeCurtainModels {
    private LargeCurtainModels() {
    }

    public static void block(
            BlockModelContext<? extends LargeCurtainBlock> context,
            SAPBlockModelGenerator generator
    ) {
        LargeCurtainBlock block = context.get();
        // Quadrant naming from the authored files: l/r = column, 1/2 = row.
        PropertyDispatch<MultiVariant> dispatch = PropertyDispatch.initial(
                        LargeCurtainBlock.HALF,
                        LargeCurtainBlock.COLUMN,
                        LargeCurtainBlock.SIDE,
                        LargeCurtainBlock.OPEN,
                        LargeCurtainBlock.ANIMATING)
                .generate((half, column, side, open, animating) -> {
                    // Quadrant naming is observer-relative for both sides:
                    // l = outer (bunching) column, r = inner column.
                    String quadrant = (column == LargeCurtainBlock.Column.OUTER ? "l" : "r")
                            + (half == DoubleBlockHalf.UPPER ? "1" : "2");
                    String suffix = open ? "open_" + quadrant : quadrant;
                    String modelName = (side == LargeCurtainBlock.Side.RIGHT
                            ? "large_curtain_right_" : "large_curtain_left_") + suffix;
                    return BlockModelGenerators.plainVariant(
                            generator.modLoc("block/large_curtain/" + modelName));
                });
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(dispatch)
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/large_curtain/large_curtain")
        );
    }
}