package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowBlock;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowSide;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

public final class CasementWindowModels {
    private CasementWindowModels() {
    }

    public static void block(
            BlockModelContext<? extends CasementWindowBlock> context,
            SAPBlockModelGenerator generator
    ) {
        CasementWindowBlock block = context.get();
        String wood = context.id().getPath().endsWith("_casement_window")
                ? context.id().getPath().substring(
                0, context.id().getPath().length() - "_casement_window".length())
                : context.id().getPath();

        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> {
            CasementWindowSide side = state.getValue(CasementWindowBlock.SIDE);
            ResourceLocation model = generator.modLoc(
                    "block/casement_window/" + wood + "_"
                            + side.getSerializedName());
            int y = switch (state.getValue(CasementWindowBlock.FACING)) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> throw new IllegalStateException(
                        "Unsupported casement window facing");
            };
            return ConfiguredModel.builder()
                    .modelFile(generator.uncheckedModel(model))
                    .rotationY(y)
                    .build();
        }, CasementWindowBlock.WATERLOGGED, CasementWindowBlock.ANIMATING);

        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/casement_window/" + wood + "_left")
        );
    }
}
