package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
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

    private String name(Item item) {
        return item.builtInRegistryHolder().key().location().getPath();
    }
}
