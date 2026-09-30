package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainSide;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/** Blockstate and item models for the one-block curtain. */
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
                : path;
        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> {
            String side = state.getValue(CurtainBlock.SIDE) == CurtainSide.RIGHT ? "right" : "left";
            String pose = state.getValue(CurtainBlock.OPEN) ? "open" : "closed";
            ResourceLocation id = generator.modLoc(
                    "block/curtain/static/" + side + "/" + pose + "/" + color + "/upper"
            );
            Direction facing = state.getValue(CurtainBlock.FACING);
            int y = switch (facing) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            return ConfiguredModel.builder()
                    .modelFile(new ModelFile.UncheckedModelFile(id))
                    .rotationY(y)
                    .build();
        }, CurtainBlock.POWERED);
        StandardBlockModels.parentBlockItem(block, generator,
                generator.modLoc("block/curtain/item/" + color));
    }
}
