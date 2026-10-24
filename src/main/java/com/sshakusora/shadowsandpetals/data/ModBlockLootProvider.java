package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.legacy.LegacyCompatIds;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

public class ModBlockLootProvider extends BlockLootSubProvider {
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
        return SAPRegistries.BLOCKS.getEntries().stream()
                .filter(holder -> !LegacyCompatIds.isLegacyCompatId(holder.getId()))
                .map(holder -> (Block) holder.get())
                .toList();
    }

    public void dropSelf(Block block) {
        super.dropSelf(block);
    }

    public void dropSlab(Block block) {
        add(block, createSlabItemTable(block));
    }

    public void dropOre(Block block, ItemLike item) {
        add(block, createOreDrop(block, item.asItem()));
    }

    public void addTable(Block block, LootTable.Builder builder) {
        add(block, builder);
    }

    public void dropLeaves(LeavesBlock leaves, SaplingBlock sapling) {
        add(leaves, createLeavesDrops(leaves, sapling, NORMAL_LEAVES_SAPLING_CHANCES));
    }

    public LootTable.Builder noDropTable() {
        return noDrop();
    }

    public LootTable.Builder createBoxTable(Block block) {
        return LootTable.lootTable().withPool(
                applyExplosionCondition(
                        block,
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(LootItem.lootTableItem(block).apply(
                                        CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                                .include(DataComponents.BLOCK_ENTITY_DATA)
                                ))
                )
        );
    }
}
