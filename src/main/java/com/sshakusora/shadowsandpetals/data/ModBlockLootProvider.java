package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.LargeCurtainBlock;
import com.sshakusora.shadowsandpetals.legacy.LegacyCompatIds;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
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

    public void dropOther(Block block, ItemLike item) {
        add(block, createSingleItemTable(item));
    }

    /**
     * Drops the item only when the broken block state is the lower half,
     * like vanilla doors: one item per curtain no matter which half broke.
     */
    public void dropSelfLowerHalfOnly(Block block) {
        add(block, LootTable.lootTable().withPool(
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)))
                        .add(LootItem.lootTableItem(block))
        ));
    }

    /**
     * Drops the item only from the anchor block (lower outer corner) of a
     * four-block curtain: one item per structure no matter which block broke.
     */
    public void dropSelfAnchorOnly(Block block, EnumProperty<LargeCurtainBlock.Column> column) {
        add(block, LootTable.lootTable().withPool(
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                                        .hasProperty(column, LargeCurtainBlock.Column.OUTER)
                                        .hasProperty(LargeCurtainBlock.ANCHOR, true)))
                        .add(LootItem.lootTableItem(block))
        ));
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
}
