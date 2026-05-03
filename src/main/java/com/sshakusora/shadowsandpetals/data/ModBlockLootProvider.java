package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Set;

public class ModBlockLootProvider extends net.minecraft.data.loot.BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (var generator : DatagenBlockLootRegistry.generators()) {
            generator.accept(this);
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return SAPRegistries.BLOCKS.getEntries().stream().map(holder -> (Block) holder.get()).toList();
    }

    public void dropSelf(Block block) {
        super.dropSelf(block);
    }

    public void dropSlab(Block block) {
        add(block, createSlabItemTable(block));
    }

    public void addTable(Block block, LootTable.Builder builder) {
        add(block, builder);
    }

    public LootTable.Builder noDropTable() {
        return noDrop();
    }
}
