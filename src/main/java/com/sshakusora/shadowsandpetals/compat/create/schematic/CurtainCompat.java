package com.sshakusora.shadowsandpetals.compat.create.schematic;

import com.simibubi.create.api.schematic.nbt.SafeNbtWriterRegistry;
import com.simibubi.create.api.schematic.requirement.SchematicRequirementRegistries;
import com.simibubi.create.api.schematic.state.SchematicStateFilterRegistry;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.AbstractCurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LargeCurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LongCurtainBlock;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Create schematic support for the three curtain block families.
 *
 * <p>Create places every saved block state and invokes {@code setPlacedBy}
 * with a null placer. Curtain placement therefore must not synthesize another
 * pair or 2x2 structure during schematic printing. The saved states already
 * contain every member, and the requirement registry charges one item for
 * each logical curtain root.</p>
 */
public final class CurtainCompat {
    private static boolean registered;

    private CurtainCompat() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        for (var block : BlockRegistry.CURTAINS) {
            registerCurtain(block.get());
        }
        for (var block : BlockRegistry.LONG_CURTAINS) {
            registerLongCurtain(block.get());
        }
        for (var block : BlockRegistry.LARGE_CURTAINS) {
            registerLargeCurtain(block.get());
        }

        SafeNbtWriterRegistry.REGISTRY.register(
                BlockEntityRegistry.CURTAIN.get(),
                CurtainCompat::writeSafeCurtainNbt
        );
        SafeNbtWriterRegistry.REGISTRY.register(
                BlockEntityRegistry.LONG_CURTAIN.get(),
                CurtainCompat::writeSafeCurtainNbt
        );
        SafeNbtWriterRegistry.REGISTRY.register(
                BlockEntityRegistry.LARGE_CURTAIN.get(),
                CurtainCompat::writeSafeCurtainNbt
        );
        registered = true;
    }

    private static void registerCurtain(CurtainBlock block) {
        SchematicRequirementRegistries.BLOCKS.register(
                block,
                CurtainCompat::getCurtainRequirement
        );
        SchematicStateFilterRegistry.REGISTRY.register(
                block,
                CurtainCompat::filterAnimationState
        );
    }

    private static void registerLongCurtain(LongCurtainBlock block) {
        SchematicRequirementRegistries.BLOCKS.register(
                block,
                CurtainCompat::getLongCurtainRequirement
        );
        SchematicStateFilterRegistry.REGISTRY.register(
                block,
                CurtainCompat::filterAnimationState
        );
    }

    private static void registerLargeCurtain(LargeCurtainBlock block) {
        SchematicRequirementRegistries.BLOCKS.register(
                block,
                CurtainCompat::getLargeCurtainRequirement
        );
        SchematicStateFilterRegistry.REGISTRY.register(
                block,
                CurtainCompat::filterAnimationState
        );
    }

    private static ItemRequirement getCurtainRequirement(
            BlockState state,
            BlockEntity blockEntity
    ) {
        return isCurtainRoot(state)
                ? itemRequirement(state)
                : ItemRequirement.NONE;
    }

    private static ItemRequirement getLongCurtainRequirement(
            BlockState state,
            BlockEntity blockEntity
    ) {
        return isLongCurtainRoot(state)
                ? itemRequirement(state)
                : ItemRequirement.NONE;
    }

    private static ItemRequirement getLargeCurtainRequirement(
            BlockState state,
            BlockEntity blockEntity
    ) {
        return isLargeCurtainRoot(state)
                ? itemRequirement(state)
                : ItemRequirement.NONE;
    }

    private static ItemRequirement itemRequirement(BlockState state) {
        return new ItemRequirement(
                ItemRequirement.ItemUseType.CONSUME,
                state.getBlock().asItem()
        );
    }

    /** A one-block curtain is always its own schematic root. */
    static boolean isCurtainRoot(BlockState state) {
        return state.getBlock() instanceof CurtainBlock;
    }

    /** The lower half is the canonical root of a long curtain. */
    static boolean isLongCurtainRoot(BlockState state) {
        return state.getBlock() instanceof LongCurtainBlock
                && isLongCurtainRoot(state.getValue(LongCurtainBlock.HALF));
    }

    static boolean isLongCurtainRoot(DoubleBlockHalf half) {
        return half == DoubleBlockHalf.LOWER;
    }

    /** The lower outer anchor is the canonical root of a 2x2 curtain. */
    static boolean isLargeCurtainRoot(BlockState state) {
        return state.getBlock() instanceof LargeCurtainBlock
                && isLargeCurtainRoot(
                state.getValue(LargeCurtainBlock.HALF),
                state.getValue(LargeCurtainBlock.COLUMN)
        );
    }

    static boolean isLargeCurtainRoot(
            DoubleBlockHalf half,
            LargeCurtainBlock.Column column
    ) {
        return half == DoubleBlockHalf.LOWER && column == LargeCurtainBlock.Column.OUTER;
    }

    /**
     * A schematic is a static snapshot. Restoring an old animation timestamp
     * or redstone-powered flag would make a newly pasted curtain depend on the
     * source world's runtime state. Keep the visible OPEN pose and let the
     * destination world recalculate POWERED from its own neighbours.
     */
    static BlockState filterAnimationState(BlockEntity blockEntity, BlockState state) {
        if (state.hasProperty(AbstractCurtainBlock.ANIMATING)) {
            state = state.setValue(AbstractCurtainBlock.ANIMATING, false);
        }
        if (state.hasProperty(AbstractCurtainBlock.POWERED)) {
            state = state.setValue(AbstractCurtainBlock.POWERED, false);
        }
        return state;
    }

    /**
     * Curtain block entities only store the animation clock. The OPEN pose is
     * already part of the block state, so intentionally omit the timestamp and
     * let the constructor derive the initial pose from the placed state.
     */
    private static void writeSafeCurtainNbt(
            BlockEntity blockEntity,
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        // Deliberately empty: transition timing must not be copied into a schematic.
    }
}
