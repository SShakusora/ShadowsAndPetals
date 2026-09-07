package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainBlock;
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
        // Every registered block, including white, carries its dye color in
        // the id (for example, white_curtain).
        String color = path.substring(0, path.length() - "_curtain".length());
        PropertyDispatch<MultiVariant> dispatch = PropertyDispatch.initial(
                        CurtainBlock.HALF,
                        CurtainBlock.SIDE,
                        CurtainBlock.OPEN,
                        CurtainBlock.ANIMATING)
                .generate((half, side, open, animating) -> {
                    String halfName = half == DoubleBlockHalf.UPPER ? "upper" : "lower";
                    String sideName = side == CurtainBlock.Side.RIGHT ? "right" : "left";
                    String poseName = open ? "open" : "closed";
                    String modelName = "static/" + sideName + "/" + poseName + "/" + color
                            + "/" + halfName;
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
