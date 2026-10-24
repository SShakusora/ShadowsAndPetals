package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ShadowsAndPetals.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (var generator : DatagenItemModelRegistry.generators()) {
            generator.accept(this);
        }
    }

    public void generatedItem(Item item) {
        withExistingParent(name(item), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name(item)));
    }

    public void boxItem(Item item) {
        var builder = getBuilder(name(item))
                .parent(new ModelFile.UncheckedModelFile("builtin/entity"))
                .texture("particle", modLoc("block/empty_box"))
                .guiLight(BlockModel.GuiLight.SIDE);

        builder.transforms()
                .transform(ItemDisplayContext.GUI)
                    .rotation(30.0F, 225.0F, 0.0F)
                    .scale(0.625F)
                    .end()
                .transform(ItemDisplayContext.GROUND)
                    .rotation(0.0F, 0.0F, 0.0F)
                    .translation(0.0F, 3.0F, 0.0F)
                    .scale(0.25F)
                    .end()
                .transform(ItemDisplayContext.FIXED)
                    .rotation(0.0F, 0.0F, 0.0F)
                    .scale(0.5F)
                    .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                    .rotation(75.0F, 45.0F, 0.0F)
                    .translation(0.0F, 2.5F, 0.0F)
                    .scale(0.375F)
                    .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    .rotation(0.0F, 45.0F, 0.0F)
                    .scale(0.4F)
                    .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                    .rotation(0.0F, 225.0F, 0.0F)
                    .scale(0.4F)
                    .end();
    }

    private String name(Item item) {
        return item.builtInRegistryHolder().key().location().getPath();
    }
}
