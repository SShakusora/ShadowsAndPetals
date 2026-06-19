package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    public ModSoundDefinitionsProvider(PackOutput output) {
        super(output, ShadowsAndPetals.MOD_ID);
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
