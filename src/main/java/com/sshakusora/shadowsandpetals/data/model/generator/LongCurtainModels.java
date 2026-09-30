package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainSide;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LongCurtainBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public final class LongCurtainModels {
    private LongCurtainModels() {}
    public static void block(BlockModelContext<? extends LongCurtainBlock> context, SAPBlockModelGenerator generator) {
        LongCurtainBlock block = context.get();
        String path = context.id().getPath();
        String color = path.endsWith("_long_curtain") ? path.substring(0, path.length() - 13) : path;
        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> {
            String half = state.getValue(LongCurtainBlock.HALF) == DoubleBlockHalf.UPPER ? "upper" : "lower";
            String side = state.getValue(LongCurtainBlock.SIDE) == CurtainSide.RIGHT ? "right" : "left";
            String pose = state.getValue(LongCurtainBlock.OPEN) ? "open" : "closed";
            ResourceLocation id = generator.modLoc("block/long_curtain/static/" + side + "/" + pose + "/" + color + "/" + half);
            Direction facing = state.getValue(LongCurtainBlock.FACING);
            int y = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
            return ConfiguredModel.builder().modelFile(new ModelFile.UncheckedModelFile(id)).rotationY(y).build();
        }, LongCurtainBlock.POWERED);
        StandardBlockModels.parentBlockItem(block, generator, generator.modLoc("block/long_curtain/item/" + color));
    }
}
