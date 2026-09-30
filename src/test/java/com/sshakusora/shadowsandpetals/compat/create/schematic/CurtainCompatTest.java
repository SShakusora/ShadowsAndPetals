package com.sshakusora.shadowsandpetals.compat.create.schematic;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.LargeCurtainBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurtainCompatTest {
    @Test
    void longCurtainChargesOnlyItsLowerHalf() {
        assertTrue(CurtainCompat.isLongCurtainRoot(DoubleBlockHalf.LOWER));
        assertFalse(CurtainCompat.isLongCurtainRoot(DoubleBlockHalf.UPPER));
    }

    @Test
    void largeCurtainChargesOnlyItsLowerOuterRoot() {
        assertTrue(CurtainCompat.isLargeCurtainRoot(
                DoubleBlockHalf.LOWER,
                LargeCurtainBlock.Column.OUTER
        ));
        assertFalse(CurtainCompat.isLargeCurtainRoot(
                DoubleBlockHalf.LOWER,
                LargeCurtainBlock.Column.INNER
        ));
        assertFalse(CurtainCompat.isLargeCurtainRoot(
                DoubleBlockHalf.UPPER,
                LargeCurtainBlock.Column.OUTER
        ));
    }
}
