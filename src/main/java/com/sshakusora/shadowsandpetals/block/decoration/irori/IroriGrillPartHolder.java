package com.sshakusora.shadowsandpetals.block.decoration.irori;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Marks an upper block that occupies one part of an Irori grill footprint.
 *
 * <p>The upper block may also carry another feature, such as the copper
 * teapot.  Keeping the structural information behind this marker prevents
 * Irori reconciliation and rendering from depending on one concrete upper
 * block class.</p>
 */
public interface IroriGrillPartHolder {
    static boolean isGrillPart(BlockState state) {
        return state.getBlock() instanceof IroriGrillPartHolder
                && state.hasProperty(IroriGrillBlock.GRILL_PART);
    }

    static IroriGrillPart getGrillPart(BlockState state) {
        if (!isGrillPart(state)) {
            throw new IllegalArgumentException("State is not an Irori grill part: " + state);
        }
        return state.getValue(IroriGrillBlock.GRILL_PART);
    }

    static IroriGrillPart getPart(BlockState state) {
        return getGrillPart(state);
    }

    static BlockPos masterPosition(BlockPos upperPos, BlockState upperState) {
        return getGrillPart(upperState).masterPosition(upperPos);
    }
}
