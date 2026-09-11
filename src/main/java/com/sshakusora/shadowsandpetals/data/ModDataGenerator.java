package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.curtain.ModCurtainAssetProvider;
import com.sshakusora.shadowsandpetals.data.irori.ModIroriGrillAssetProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModCurtainAssetProvider(output));
        generator.addProvider(event.includeClient(), new ModIroriGrillAssetProvider(output));
        generator.addProvider(event.includeClient(), new ModRockeryModelProvider(output));
        generator.addProvider(event.includeClient(), new ModConnectedTextureProvider(output));
        generator.addProvider(event.includeClient(), new ModSoundDefinitionsProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output, "en_us"));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output, "zh_cn"));
        generator.addProvider(event.includeServer(), new AdvancementProvider(output, lookupProvider, List.of(
                new ModAdvancementProvider()
        )));
        generator.addProvider(event.includeServer(), new ModDataMapProvider(output, lookupProvider));
        generator.addProvider(event.includeServer(), new ModBonsaiTreeProvider(output));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookupProvider));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)
        ), lookupProvider));
        ModBlockTagProvider blockTags = new ModBlockTagProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ModItemTagProvider(
                output, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(), new WorldGenProvider(output, lookupProvider));
    }
}
