package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowBlock;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowSide;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;

/** Datagen for the left and right casement-window models. */
public final class CasementWindowModels {
    private CasementWindowModels() {
    }

    public static void block(
            BlockModelContext<? extends CasementWindowBlock> context,
            SAPBlockModelGenerator generator
    ) {
        CasementWindowBlock block = context.get();
        String path = context.id().getPath();
        String wood = path.substring(0, path.length() - "_casement_window".length());

        PropertyDispatch<MultiVariant> dispatch = PropertyDispatch.initial(
                        CasementWindowBlock.SIDE,
                        CasementWindowBlock.OPEN,
                        CasementWindowBlock.ANIMATING)
                .generate((side, open, animating) -> {
                    String sideName = side == CasementWindowSide.RIGHT ? "right" : "left";
                    return BlockModelGenerators.plainVariant(
                            generator.modLoc("block/casement_window/" + wood + "_" + sideName));
                });

        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(dispatch)
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));

        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/casement_window/" + wood + "_left")
        );
    }
}
