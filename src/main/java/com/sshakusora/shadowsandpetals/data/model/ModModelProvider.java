package com.sshakusora.shadowsandpetals.data.model;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.stream.Stream;

public final class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, ShadowsAndPetals.MOD_ID);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return ModelDatagenRegistry.knownBlocks();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return ModelDatagenRegistry.knownItems();
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        SAPBlockModelGenerator blockGenerator = new SAPBlockModelGenerator(blockModels);
        ModelDatagenRegistry.generateBlocks(blockGenerator);

        SAPItemModelGenerator itemGenerator = new SAPItemModelGenerator(blockModels, blockGenerator);
        ModelDatagenRegistry.generateItemModels(itemGenerator);
        ModelDatagenRegistry.finalizeClientItems(itemGenerator);
    }
}
