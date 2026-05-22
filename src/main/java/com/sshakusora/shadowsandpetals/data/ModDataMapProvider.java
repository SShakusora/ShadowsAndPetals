package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;
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

        BlockRegistry.WOOD_SETS.forEach(woodSet -> addStrippables(strippables, woodSet));
    }

    private static void addStrippables(Builder<Strippable, Block> strippables, WoodSetList.WoodSet woodSet) {
        strippables.add(woodSet.log().get().builtInRegistryHolder(), new Strippable(woodSet.strippedLog().get()), false);
        strippables.add(woodSet.wood().get().builtInRegistryHolder(), new Strippable(woodSet.strippedWood().get()), false);
        strippables.add(woodSet.post().get().builtInRegistryHolder(), new Strippable(woodSet.strippedPost().get()), false);
        strippables.add(woodSet.woodPost().get().builtInRegistryHolder(), new Strippable(woodSet.strippedWoodPost().get()), false);
    }

    @Override
    public String getName() {
        return ShadowsAndPetals.MOD_ID + " Data Maps";
    }
}
