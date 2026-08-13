package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.registries.AdvancementRegistry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;

import java.util.function.Consumer;

public final class ModAdvancementProvider implements AdvancementSubProvider {
    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
        AdvancementRegistry.init();
        DatagenAdvancementRegistry.generate(registries, output);
    }
}
