package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.api.excavation.SandExcavationDataMaps;
import com.sshakusora.shadowsandpetals.api.excavation.SandExcavationDropCategory;
import com.sshakusora.shadowsandpetals.api.excavation.SandExcavationDropData;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Generates the 1.21.1 data-map JSON consumed by sand excavation.
 *
 * <p>The 26.1.2 provider used the newer data-generation API.  NeoForge
 * 1.21.1 exposes the equivalent {@link DataMapProvider} surface, so the
 * entries are rebuilt here instead of being left as static compatibility
 * resources only.</p>
 */
public final class ModDataMapProvider extends DataMapProvider {
    public ModDataMapProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider
    ) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(SandExcavationDataMaps.DROPS)
                .add(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "flint"),
                        new SandExcavationDropData(SandExcavationDropCategory.TRASH, 2, 1, 1),
                        false
                )
                .add(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "gravel"),
                        new SandExcavationDropData(SandExcavationDropCategory.TRASH, 1, 1, 1),
                        false
                )
                .add(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "stick"),
                        new SandExcavationDropData(SandExcavationDropCategory.TRASH, 3, 1, 1),
                        false
                )
                .add(
                        ResourceLocation.fromNamespaceAndPath("shadowsandpetals", "clam"),
                        new SandExcavationDropData(SandExcavationDropCategory.SEAFOOD, 10, 1, 1),
                        false
                );
    }
}
