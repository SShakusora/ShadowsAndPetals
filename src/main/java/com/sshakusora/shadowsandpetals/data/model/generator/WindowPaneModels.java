package com.sshakusora.shadowsandpetals.data.model.generator;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.block.decoration.WindowPaneBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public final class WindowPaneModels {
    private WindowPaneModels() {
    }

    public static void block(
            BlockModelContext<? extends WindowPaneBlock> context,
            SAPBlockModelGenerator generator,
            String modelName,
            Block planks
    ) {
        WindowPaneBlock block = context.get();
        Identifier modelId = generator.modLoc("block/window_pane/" + modelName);
        generator.jsonModel(modelId, childModel(
                generator.modLoc("block/window_pane/window_pane"),
                plankTexture(planks)
        ));
        registerBlockState(block, generator, modelId);
        StandardBlockModels.parentBlockItem(block, generator, modelId);
    }

    public static void redLacquered(
            BlockModelContext<? extends WindowPaneBlock> context,
            SAPBlockModelGenerator generator
    ) {
        WindowPaneBlock block = context.get();
        Identifier modelId = generator.modLoc("block/window_pane/red");
        generator.jsonModel(modelId, childModel(
                generator.modLoc("block/window_pane/window_pane"),
                generator.modLoc("block/window_pane/red")
        ));
        registerBlockState(block, generator, modelId);
        StandardBlockModels.parentBlockItem(block, generator, modelId);
    }

    @SuppressWarnings("unused")
    private static void registerBlockState(
            WindowPaneBlock block,
            SAPBlockModelGenerator generator,
            Identifier modelId
    ) {
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(WindowPaneBlock.AXIS, WindowPaneBlock.WATERLOGGED)
                        .generate((axis, waterlogged) -> axisVariant(modelId, axis))));
    }

    private static JsonObject childModel(Identifier parent, Identifier texture) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent.toString());
        JsonObject textures = new JsonObject();
        textures.addProperty("windowframes", texture.toString());
        textures.addProperty("particle", texture.toString());
        model.add("textures", textures);
        return model;
    }

    private static Identifier plankTexture(Block planks) {
        Identifier planksId = BuiltInRegistries.BLOCK.getKey(planks);
        return Identifier.fromNamespaceAndPath(
                planksId.getNamespace(),
                "block/" + planksId.getPath()
        );
    }

    private static MultiVariant axisVariant(Identifier modelId, Direction.Axis axis) {
        MultiVariant variant = BlockModelGenerators.plainVariant(modelId);
        return switch (axis) {
            case X -> variant.with(BlockModelGenerators.Y_ROT_90);
            case Y -> variant.with(BlockModelGenerators.X_ROT_90);
            case Z -> variant;
        };
    }
}
