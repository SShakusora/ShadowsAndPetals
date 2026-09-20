package com.sshakusora.shadowsandpetals.compat.create.schematic;

import com.simibubi.create.api.schematic.requirement.SchematicRequirementRegistries;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registers Create Schematic material requirements for Rockery structures.
 */
public final class RockeryCompat {
    private static boolean registered;

    private RockeryCompat() {
    }

    /**
     * Register all Rockery variants after the normal block registry phase.
     * The method is idempotent for development and test lifecycle replay.
     */
    public static synchronized void register() {
        if (registered) {
            return;
        }

        for (DeferredBlock<RockeryBlock> deferredBlock : rockeries()) {
            SchematicRequirementRegistries.BLOCKS.register(
                    deferredBlock.get(),
                    RockeryCompat::getRequiredItems
            );
        }
        registered = true;
    }

    /**
     * Every saved Rockery part represents one stone in the carved structure.
     * Create calls this once for each saved PART state, so the total requirement
     * naturally equals the dimensions' part count.
     */
    static ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, Items.STONE);
    }

    static int requiredStoneCount(RockeryDimensions dimensions) {
        return dimensions.partCount();
    }

    private static List<DeferredBlock<RockeryBlock>> rockeries() {
        return List.of(
                BlockRegistry.ROCKERY_1x1x1,
                BlockRegistry.ROCKERY_1x1x2,
                BlockRegistry.ROCKERY_1x2x1,
                BlockRegistry.ROCKERY_1x2x2,
                BlockRegistry.ROCKERY_1x3x1
        );
    }
}
