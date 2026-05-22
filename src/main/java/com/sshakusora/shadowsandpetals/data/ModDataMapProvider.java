package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
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

        strippables.add(BlockRegistry.SAKURA_LOG.get().builtInRegistryHolder(), new Strippable(BlockRegistry.STRIPPED_SAKURA_LOG.get()), false);
        strippables.add(BlockRegistry.SAKURA_WOOD.get().builtInRegistryHolder(), new Strippable(BlockRegistry.STRIPPED_SAKURA_WOOD.get()), false);

        strippables.add(BlockRegistry.MAPLE_LOG.get().builtInRegistryHolder(), new Strippable(BlockRegistry.STRIPPED_MAPLE_LOG.get()), false);
        strippables.add(BlockRegistry.MAPLE_WOOD.get().builtInRegistryHolder(), new Strippable(BlockRegistry.STRIPPED_MAPLE_WOOD.get()), false);

        strippables.add(BlockRegistry.GINKGO_LOG.get().builtInRegistryHolder(), new Strippable(BlockRegistry.STRIPPED_GINKGO_LOG.get()), false);
        strippables.add(BlockRegistry.GINKGO_WOOD.get().builtInRegistryHolder(), new Strippable(BlockRegistry.STRIPPED_GINKGO_WOOD.get()), false);
    }

    @Override
    public String getName() {
        return ShadowsAndPetals.MOD_ID + " Data Maps";
    }
}
