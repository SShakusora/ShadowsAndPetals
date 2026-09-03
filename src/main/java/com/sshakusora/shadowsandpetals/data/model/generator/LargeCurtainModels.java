package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.LargeCurtainBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;

/**
 * Datagen for the single-block large curtain. Outside the animation window
 * the block renders as a plain block-state model: the closed pose or the
 * baked open pose. While ANIMATING the render shape is INVISIBLE and
 * {@code LargeCurtainBlockEntityRenderer} owns the pose.
 */
public final class LargeCurtainModels {
    private LargeCurtainModels() {
    }

    public static void block(
            BlockModelContext<? extends LargeCurtainBlock> context,
            SAPBlockModelGenerator generator
    ) {
        LargeCurtainBlock block = context.get();
        MultiVariant closed = BlockModelGenerators.plainVariant(
                generator.modLoc("block/large_curtain/large_curtain"));
        MultiVariant open = BlockModelGenerators.plainVariant(
                generator.modLoc("block/large_curtain/large_curtain_open"));
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(LargeCurtainBlock.OPEN, LargeCurtainBlock.ANIMATING)
                        .select(false, true, closed)
                        .select(false, false, closed)
                        .select(true, true, open)
                        .select(true, false, open))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/large_curtain/large_curtain")
        );
    }
}