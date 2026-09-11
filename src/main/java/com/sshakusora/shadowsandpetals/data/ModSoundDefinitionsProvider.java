package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    public ModSoundDefinitionsProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ShadowsAndPetals.MOD_ID, existingFileHelper);
    }

    @Override
    public void registerSounds() {
        for (var holder : SAPRegistries.SOUNDS.getEntries()) {
            var def = definition().with(sound(holder.getId()));
            String subtitleKey = DatagenSoundRegistry.getSubtitle(holder.getId());
            if (subtitleKey != null) {
                def.subtitle(subtitleKey);
            }
            add(holder.getId(), def);
        }
    }
}