package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public class ModDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(true, new ModBlockStateProvider(output));
        generator.addProvider(true, new ModWoodModelProvider(output));
        generator.addProvider(true, new ModItemModelProvider(output));
        generator.addProvider(true, new ModClientItemProvider(output));
        generator.addProvider(true, new ModLanguageProvider(output, DatagenLangRegistry.DEFAULT_LOCALE));
        generator.addProvider(true, new ModLanguageProvider(output, DatagenLangRegistry.ZH_CN));
        generator.addProvider(true, new ModRecipeProvider.Runner(output, lookupProvider));
        generator.addProvider(true, new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)
        ), lookupProvider));
        generator.addProvider(true, new ModBlockTagProvider(output, lookupProvider));
        generator.addProvider(true, new ModDataMapProvider(output, lookupProvider));
        generator.addProvider(true, new WorldGenProvider(output, lookupProvider));
    }
}
