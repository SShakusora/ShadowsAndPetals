package com.sshakusora.shadowsandpetals.compat.chinjufu;

import com.sshakusora.shadowsandpetals.block.WoodBlockList.WoodType;
import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.block.agriculture.OrangeTreeBlock;
import com.sshakusora.shadowsandpetals.block.decoration.IngotPileBlock;
import com.sshakusora.shadowsandpetals.block.decoration.RoofTileBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import com.sshakusora.shadowsandpetals.registries.SAPRegistries;
import com.sshakusora.shadowsandpetals.registries.builder.RegBlockBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Chinjufu-specific block aliases and state conversions. */
public final class ChinjufuBlockCompat {
    private ChinjufuBlockCompat() {}

    public static RegBlockBuilder<IngotPileBlock> ingotPileStateAlias(
            RegBlockBuilder<IngotPileBlock> builder,
            String legacyPath
    ) {
        return builder.stateAliasProperties(ChinjufuIds.MOD_ID, legacyPath,
                legacy -> legacy
                        .property(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                        .property(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM)
                        .property(BlockStateProperties.WATERLOGGED, false),
                (legacyState, targetState) -> targetState
                        .setValue(IngotPileBlock.HORIZONTAL_AXIS,
                                legacyState.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis())
                        .setValue(BlockStateProperties.SLAB_TYPE,
                                legacyState.getValue(BlockStateProperties.SLAB_TYPE))
                        .setValue(BlockStateProperties.WATERLOGGED,
                                legacyState.getValue(BlockStateProperties.WATERLOGGED)));
    }

    /** Maps Chinjufu's old {@code cra} roof-tile flag onto the new directional roof tile. */
    public static RegBlockBuilder<RoofTileBlock> roofTileStateAlias(
            RegBlockBuilder<RoofTileBlock> builder,
            String legacyPath
    ) {
        BooleanProperty crack = BooleanProperty.create("cra");
        return builder.stateAliasProperties(ChinjufuIds.MOD_ID, legacyPath,
                legacy -> legacy.property(crack, false),
                (legacyState, targetState) -> targetState);
    }

    /** Maps the old 0..11 double-height mikan crop state to the new 0..7 crop state. */
    public static RegBlockBuilder<OrangeTreeBlock> orangeTreeStateAlias(
            RegBlockBuilder<OrangeTreeBlock> builder
    ) {
        IntegerProperty legacyStage = IntegerProperty.create("stage", 0, 11);
        return builder.stateAliasProperties(ChinjufuIds.MOD_ID, "block_wood_mikan",
                legacy -> legacy
                        .property(legacyStage, 0)
                        .property(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                        .property(BlockStateProperties.WATERLOGGED, false),
                (legacyState, targetState) -> targetState
                        .setValue(OrangeTreeBlock.AGE,
                                Math.min(legacyState.getValue(legacyStage), OrangeTreeBlock.MAX_AGE))
                        .setValue(OrangeTreeBlock.HALF,
                                legacyState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF))
                        .setValue(OrangeTreeBlock.FACING, Direction.NORTH));
    }

    /** Adds a state and item alias for one of the old Tansu blocks that becomes a vanity. */
    public static RegBlockBuilder<VanityBlock> vanityStateAlias(
            RegBlockBuilder<VanityBlock> builder,
            WoodType woodType
    ) {
        String legacyPath = ChinjufuIds.tansuBlockPath(woodType);
        if (legacyPath == null) {
            return builder;
        }

        BooleanProperty open = BooleanProperty.create("open");
        return builder
                .stateAliasProperties(ChinjufuIds.MOD_ID, legacyPath,
                        legacy -> legacy
                                .property(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                                .property(BlockStateProperties.WATERLOGGED, false)
                                .property(open, false)
                                .property(BlockStateProperties.CHEST_TYPE, ChestType.SINGLE),
                        (legacyState, targetState) -> targetState
                                .setValue(VanityBlock.FACING,
                                        legacyState.getValue(BlockStateProperties.HORIZONTAL_FACING))
                                .setValue(VanityBlock.HALF, DoubleBlockHalf.LOWER)
                                .setValue(VanityBlock.WATERLOGGED,
                                        legacyState.getValue(BlockStateProperties.WATERLOGGED)))
                .itemAlias(ChinjufuIds.MOD_ID, legacyPath);
    }

    /** Adds the direct block and item aliases that do not require a compatibility block. */
    public static void registerDirectAliases(
            DeferredBlock<SaplingBlock> autumnOakSapling,
            DeferredBlock<LeavesBlock> autumnOakLeaves,
            WoodSetList.WoodSet sakura,
            WoodSetList.WoodSet maple,
            WoodSetList.WoodSet ginkgo
    ) {
        addAlias(autumnOakSapling, "block_tree_oakkare_nae");
        addAlias(autumnOakLeaves, "block_tree_oakkare_leaf");

        registerWoodSetAliases(sakura, "sakura", "block_tree_sakura_log", "block_tree_sakura_flow", "block_tree_sakura_nae");
        registerWoodSetAliases(maple, "kaede", "block_tree_kaede_log", "block_tree_kaede_leaf", "block_tree_kaede_nae");
        registerWoodSetAliases(ginkgo, "ichoh", "block_tree_ichoh_log", "block_tree_ichoh_leaf", "block_tree_ichoh_nae");
    }

    private static void registerWoodSetAliases(
            WoodSetList.WoodSet set,
            String legacyWoodName,
            String legacyLog,
            String legacyLeaves,
            String legacySapling
    ) {
        addAlias(set.log(), legacyLog);
        addAlias(set.leaves(), legacyLeaves);
        addAlias(set.sapling(), legacySapling);
        addAlias(set.planks(), "block_planks_" + legacyWoodName);
        addAlias(set.slab(), "block_slabhalf_" + legacyWoodName);
        addAlias(set.stairs(), "block_stairs_" + legacyWoodName);
        addAlias(set.fence(), "block_fence_" + legacyWoodName);
        addAlias(set.fenceGate(), "block_fencegate_" + legacyWoodName);
        addAlias(set.pressurePlate(), "block_plate_" + legacyWoodName);
        addAlias(set.button(), "block_button_" + legacyWoodName);
    }

    private static void addAlias(DeferredBlock<? extends Block> target, String legacyPath) {
        ResourceLocation legacyId = ChinjufuIds.id(legacyPath);
        SAPRegistries.BLOCKS.addAlias(legacyId, target.getId());
        SAPRegistries.ITEMS.addAlias(legacyId, target.getId());
    }

}
