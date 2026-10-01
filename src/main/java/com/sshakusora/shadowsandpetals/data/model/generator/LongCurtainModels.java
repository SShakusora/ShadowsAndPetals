package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.AbstractCurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainSide;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LongCurtainBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Datagen for the two-cell long curtain. */
public final class LongCurtainModels {
    private LongCurtainModels() {
    }

    public static void block(
            BlockModelContext<? extends LongCurtainBlock> context,
            SAPBlockModelGenerator generator
    ) {
        LongCurtainBlock block = context.get();
        String path = context.id().getPath();
        String color = path.substring(0, path.length() - "_long_curtain".length());
        PropertyDispatch<MultiVariant> dispatch = PropertyDispatch.initial(
                        LongCurtainBlock.HALF,
                        AbstractCurtainBlock.SIDE,
                        AbstractCurtainBlock.OPEN,
                        AbstractCurtainBlock.ANIMATING)
                .generate((half, side, open, animating) -> {
                    String halfName = half == DoubleBlockHalf.UPPER ? "upper" : "lower";
                    String sideName = side == CurtainSide.RIGHT ? "right" : "left";
                    String poseName = open ? "open" : "closed";
                    String modelName = "static/" + sideName + "/" + poseName + "/" + color
                            + "/" + halfName;
                    return BlockModelGenerators.plainVariant(
                            generator.modLoc("block/long_curtain/" + modelName));
                });
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(dispatch)
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/long_curtain/item/" + color)
        );
    }
}
