package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.resources.Identifier;

/** Client-side model datagen for the bonsai block. */
public final class BonsaiBlockModels {
    private BonsaiBlockModels() {
    }

    public static void block(
            BlockModelContext<BonsaiBlock> context,
            SAPBlockModelGenerator generator
    ) {
        Identifier model = ShadowsAndPetals.asResource("block/bonsai/bonsai");
        // The model is wrapped on the client to apply all sixteen rotation
        // segments while keeping the pot in the normal chunk renderer.
        generator.blockState(MultiVariantGenerator.dispatch(
                context.get(),
                BlockModelGenerators.plainVariant(model)
        ));
        StandardBlockModels.parentBlockItem(context.get(), generator, model);
    }
}