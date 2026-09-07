package com.sshakusora.shadowsandpetals.data.model;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.model.WindChimeItemModel;
import com.sshakusora.shadowsandpetals.client.model.WoodenBarrelItemModel;
import com.sshakusora.shadowsandpetals.registries.FluidRegistry;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiConsumer;

public final class SAPItemModelGenerator {
    private static final Identifier TEA_BUCKET_MODEL_TYPE = ShadowsAndPetals.asResource("tea_bucket");
    private static final Material BUCKET_TEXTURE = new Material(Identifier.withDefaultNamespace("item/bucket"));
    private static final Material BUCKET_FLUID_MASK =
            new Material(Identifier.fromNamespaceAndPath("neoforge", "item/mask/bucket_fluid"));

    private final ItemModelOutput output;
    private final BiConsumer<Identifier, ModelInstance> modelOutput;
    private final SuggestedItemModels suggestedItems;

    public SAPItemModelGenerator(BlockModelGenerators source, SAPBlockModelGenerator blockModels) {
        this.output = source.itemModelOutput;
        this.modelOutput = source.modelOutput;
        this.suggestedItems = blockModels.suggestedItems();
    }

    public Identifier modLoc(String path) {
        return ShadowsAndPetals.asResource(path);
    }

    public Identifier generatedItem(Item item) {
        Identifier modelId = ModelLocationUtils.getModelLocation(item);
        ModelTemplates.FLAT_ITEM.create(modelId, TextureMapping.layer0(item), modelOutput);
        suggest(item, modelId);
        return modelId;
    }

    public Identifier generatedItem(Item item, Identifier texture) {
        Identifier modelId = ModelLocationUtils.getModelLocation(item);
        ModelTemplates.FLAT_ITEM.create(
                modelId,
                new TextureMapping().put(TextureSlot.LAYER0, new Material(texture)),
                modelOutput
        );
        suggest(item, modelId);
        return modelId;
    }

    public void model(Identifier id, ModelInstance model) {
        modelOutput.accept(id, model);
    }

    public Identifier create(ModelTemplate template, Identifier id, TextureMapping mapping) {
        return template.create(id, mapping, modelOutput);
    }

    public void parentModel(Identifier modelId, Identifier parent) {
        new ModelTemplate(Optional.of(parent), Optional.empty())
                .create(modelId, new TextureMapping(), modelOutput);
    }

    public void suggest(Item item, Identifier modelId) {
        suggestedItems.accept(item, ItemModelUtils.plainModel(modelId));
    }

    void finalizeClientItem(Item item, @Nullable Identifier explicitModel, @Nullable Identifier customType) {
        if (customType != null) {
            output.accept(item, customItemModel(customType));
            return;
        }
        if (explicitModel != null) {
            output.accept(item, ItemModelUtils.plainModel(explicitModel));
            return;
        }
        SuggestedItemModels.Suggestion suggestion = suggestedItems.get(item);
        if (suggestion != null) {
            output.accept(item, suggestion.model(), suggestion.properties());
            return;
        }
        output.accept(item, ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item)));
    }

    private static ItemModel.Unbaked customItemModel(Identifier type) {
        if (type.equals(WindChimeItemModel.TYPE)) {
            return WindChimeItemModel.Unbaked.INSTANCE;
        }
        if (type.equals(WoodenBarrelItemModel.TYPE)) {
            return WoodenBarrelItemModel.Unbaked.INSTANCE;
        }
        if (type.equals(TEA_BUCKET_MODEL_TYPE)) {
            return new DynamicFluidContainerModel.Unbaked(
                    new DynamicFluidContainerModel.Textures(
                            Optional.empty(),
                            Optional.of(BUCKET_TEXTURE),
                            Optional.of(BUCKET_FLUID_MASK),
                            Optional.empty()
                    ),
                    FluidRegistry.TEA.get(),
                    false,
                    true,
                    true
            );
        }
        throw new IllegalArgumentException("Unsupported custom client item model type for datagen: " + type);
    }
}
