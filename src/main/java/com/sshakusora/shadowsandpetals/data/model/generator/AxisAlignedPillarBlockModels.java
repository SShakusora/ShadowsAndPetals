package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public final class AxisAlignedPillarBlockModels {
    private AxisAlignedPillarBlockModels() {
    }

    public static void withItem(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator
    ) {
        Block block = context.get();
        Identifier modelId = generator.blockModelId(block);
        generator.blockState(BlockModelGenerators.createAxisAlignedPillarBlock(
                block,
                BlockModelGenerators.plainVariant(modelId)
        ));
        StandardBlockModels.parentBlockItem(block, generator, modelId);
    }
}
