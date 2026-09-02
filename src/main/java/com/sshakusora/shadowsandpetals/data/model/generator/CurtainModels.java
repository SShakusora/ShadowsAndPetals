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
 * {@code CurtainBlockEntityRenderer} through the per-half animation rigs.
 */
public final class CurtainModels {
    private CurtainModels() {
    }

    public static void block(
            BlockModelContext<? extends CurtainBlock> context,
            SAPBlockModelGenerator generator
    ) {
        CurtainBlock block = context.get();
        MultiVariant upper = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_upper_r"));
        MultiVariant lower = BlockModelGenerators.plainVariant(
                generator.modLoc("block/curtain/curtain_lower_r"));
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(CurtainBlock.HALF)
                        .select(DoubleBlockHalf.UPPER, upper)
                        .select(DoubleBlockHalf.LOWER, lower))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/curtain/curtain_lower_r")
        );
    }
}