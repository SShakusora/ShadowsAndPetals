package com.sshakusora.shadowsandpetals.data.model;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.1 compatibility facade for the 26.x model generator.
 *
 * <p>The 26.x client model graph does not exist in the 1.21.1 datagen API.  The
 * registry callbacks are retained as no-op hooks so common registration code
 * remains source-compatible; legacy JSON assets are supplied from resources.</p>
 */
public class SAPBlockModelGenerator {
    private final @Nullable BlockStateProvider provider;

    public SAPBlockModelGenerator() {
        this(null);
    }

    public SAPBlockModelGenerator(@Nullable BlockStateProvider provider) {
        this.provider = provider;
    }

    public ResourceLocation modLoc(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    public ResourceLocation blockModelId(Block block) {
        return ShadowsAndPetals.asResource("block/" + BuiltInRegistries.BLOCK.getKey(block).getPath());
    }

    public void suggestItemModel(Item item, ResourceLocation model) {
    }

    /**
     * Emits the legacy block model, blockstate and block-item model for a
     * simple cube callback.
     */
    public void cubeAllWithItem(Block block, String name, ResourceLocation texture) {
        if (provider == null) {
            return;
        }
        ModelFile model = provider.models().cubeAll(name, texture);
        provider.simpleBlockWithItem(block, model);
    }

    public void simpleBlockWithItem(Block block, ResourceLocation model) {
        if (provider == null) {
            return;
        }
        provider.simpleBlockWithItem(block, provider.models().getExistingFile(model));
    }

    public void jsonModel(ResourceLocation id, Object model) {
    }

    public void blockState(Object generator) {
    }

    public void model(ResourceLocation id, Object model) {
    }
}
