package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Strippable;

import java.util.concurrent.CompletableFuture;

public class ModDataMapProvider extends DataMapProvider {
    public ModDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        var strippables = builder(NeoForgeDataMaps.STRIPPABLES);
        var furnaceFuels = builder(NeoForgeDataMaps.FURNACE_FUELS);

        BlockRegistry.WOOD_SETS.forEach(woodSet -> {
            addStrippables(strippables, woodSet);
            addWoodFuels(furnaceFuels, woodSet);
        });
    }

    private static void addStrippables(Builder<Strippable, Block> strippables, WoodSetList.WoodSet woodSet) {
        strippables.add(woodSet.log().get().builtInRegistryHolder(), new Strippable(woodSet.strippedLog().get()), false);
        strippables.add(woodSet.wood().get().builtInRegistryHolder(), new Strippable(woodSet.strippedWood().get()), false);
        strippables.add(woodSet.post().get().builtInRegistryHolder(), new Strippable(woodSet.strippedPost().get()), false);
        strippables.add(woodSet.woodPost().get().builtInRegistryHolder(), new Strippable(woodSet.strippedWoodPost().get()), false);
    }

    private static void addWoodFuels(Builder<FurnaceFuel, Item> furnaceFuels, WoodSetList.WoodSet woodSet) {
        addFuel(furnaceFuels, woodSet.post(), 100);
        addFuel(furnaceFuels, woodSet.strippedPost(), 100);
        addFuel(furnaceFuels, woodSet.woodPost(), 100);
        addFuel(furnaceFuels, woodSet.strippedWoodPost(), 100);
        addFuel(furnaceFuels, woodSet.verticalSlab(), 150);
    }

    private static void addFuel(
            Builder<FurnaceFuel, Item> furnaceFuels,
            DeferredBlock<? extends Block> block,
            int burnTime
    ) {
        furnaceFuels.add(block.get().asItem().builtInRegistryHolder(), new FurnaceFuel(burnTime), false);
    }

    @Override
    public String getName() {
        return ShadowsAndPetals.MOD_ID + " Data Maps";
    }
}
