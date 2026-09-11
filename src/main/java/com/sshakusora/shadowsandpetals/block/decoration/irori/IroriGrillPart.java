package com.sshakusora.shadowsandpetals.block.decoration.irori;

import com.sshakusora.shadowsandpetals.blockentity.irori.IroriComponentTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;

import java.util.Map;

/**
 * Identifies one physical cell of the two-block-tall grill structure.
 *
 * <p>The master Irori cell is always the north-west cell of the grill
 * footprint.  Keeping the offset in the enum lets the upper block resolve
 * its master without another block entity or a second set of state fields.</p>
 */
public enum IroriGrillPart implements StringRepresentable {
    SINGLE("single", 0, 0),
    STRIP_NORTH("strip_north", 0, 0),
    STRIP_SOUTH("strip_south", 0, 1),
    STRIP_WEST("strip_west", 0, 0),
    STRIP_EAST("strip_east", 1, 0),
    QUAD_NORTH_WEST("quad_north_west", 0, 0),
    QUAD_NORTH_EAST("quad_north_east", 1, 0),
    QUAD_SOUTH_WEST("quad_south_west", 0, 1),
    QUAD_SOUTH_EAST("quad_south_east", 1, 1);

    private final String modelName;
    private final int offsetX;
    private final int offsetZ;

    IroriGrillPart(String modelName, int offsetX, int offsetZ) {
        this.modelName = modelName;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }

    public String modelName() {
        return modelName;
    }

    @Override
    public String getSerializedName() {
        return modelName;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetZ() {
        return offsetZ;
    }

    public BlockPos upperPosition(BlockPos masterPos) {
        return masterPos.offset(offsetX, 1, offsetZ);
    }

    public BlockPos masterPosition(BlockPos upperPos) {
        return upperPos.offset(-offsetX, -1, -offsetZ);
    }

    public boolean isAnchor() {
        return offsetX == 0 && offsetZ == 0;
    }

    /**
     * Maps the center footprint of a connected Irori component to its upper
     * structure states.  The elected master is the north-west center cell,
     * which is also the origin used by the old BER grill transform.
     */
    public static Map<BlockPos, IroriGrillPart> forComponent(
            IroriComponentTopology.Layout layout,
            BlockPos masterPos
    ) {
        if (layout.centerWidth() == 1 && layout.centerDepth() == 1) {
            return Map.of(masterPos, SINGLE);
        }

        if (layout.centerWidth() == 1) {
            return Map.of(
                    masterPos, STRIP_NORTH,
                    masterPos.south(), STRIP_SOUTH
            );
        }

        if (layout.centerDepth() == 1) {
            return Map.of(
                    masterPos, STRIP_WEST,
                    masterPos.east(), STRIP_EAST
            );
        }

        return Map.of(
                masterPos, QUAD_NORTH_WEST,
                masterPos.east(), QUAD_NORTH_EAST,
                masterPos.south(), QUAD_SOUTH_WEST,
                masterPos.south().east(), QUAD_SOUTH_EAST
        );
    }
}