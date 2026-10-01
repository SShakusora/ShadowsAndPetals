package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.AbstractCurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainSide;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;

/** Datagen for the single-cell curtain. */
public final class CurtainModels {
    private CurtainModels() {
    }

    public static void block(
            BlockModelContext<? extends CurtainBlock> context,
            SAPBlockModelGenerator generator
    ) {
        CurtainBlock block = context.get();
        String path = context.id().getPath();
        String color = path.substring(0, path.length() - "_curtain".length());
        PropertyDispatch<MultiVariant> dispatch = PropertyDispatch.initial(
                        AbstractCurtainBlock.SIDE,
                        AbstractCurtainBlock.OPEN,
                        AbstractCurtainBlock.ANIMATING)
                .generate((side, open, animating) -> {
                    String sideName = side == CurtainSide.RIGHT ? "right" : "left";
                    String poseName = open ? "open" : "closed";
                    String modelName = "static/" + sideName + "/" + poseName + "/" + color + "/upper";
                    return BlockModelGenerators.plainVariant(
                            generator.modLoc("block/curtain/" + modelName));
                });
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(dispatch)
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/curtain/item/" + color)
        );
    }
}
