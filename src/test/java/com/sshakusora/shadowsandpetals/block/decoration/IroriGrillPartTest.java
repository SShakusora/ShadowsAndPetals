package com.sshakusora.shadowsandpetals.block.decoration;

import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPart;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriComponentTopology;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IroriGrillPartTest {
    @Test
    void oneByOneUsesTheSingleCellModel() {
        BlockPos master = new BlockPos(4, 12, -2);
        assertEquals(Map.of(master, IroriGrillPart.SINGLE), IroriGrillPart.forComponent(
                IroriComponentTopology.layout(1, 1), master));
    }

    @Test
    void stripsUseNorthSouthOrWestEastPartsFromTheElectedMaster() {
        BlockPos master = new BlockPos(4, 12, -2);
        assertEquals(
                Map.of(master, IroriGrillPart.STRIP_NORTH,
                        master.south(), IroriGrillPart.STRIP_SOUTH),
                IroriGrillPart.forComponent(IroriComponentTopology.layout(1, 2), master)
        );
        assertEquals(
                Map.of(master, IroriGrillPart.STRIP_WEST,
                        master.east(), IroriGrillPart.STRIP_EAST),
                IroriGrillPart.forComponent(IroriComponentTopology.layout(2, 1), master)
        );
    }

    @Test
    void twoByTwoPartsRoundTripTheirMasterAndUpperPositions() {
        BlockPos master = new BlockPos(4, 12, -2);
        Map<BlockPos, IroriGrillPart> parts = IroriGrillPart.forComponent(
                IroriComponentTopology.layout(2, 2), master);

        assertEquals(4, parts.size());
        for (Map.Entry<BlockPos, IroriGrillPart> entry : parts.entrySet()) {
            BlockPos upper = entry.getKey().above();
            assertEquals(upper, entry.getValue().upperPosition(master));
            assertEquals(master, entry.getValue().masterPosition(upper));
        }
    }
}
