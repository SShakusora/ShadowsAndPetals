package com.sshakusora.shadowsandpetals.compat.create.schematic;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.LargeCurtainBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurtainCompatTest {
    @Test
    void smallCurtainChargesOnlyItsLowerHalf() {
        assertTrue(CurtainCompat.isSmallCurtainRoot(DoubleBlockHalf.LOWER));
        assertFalse(CurtainCompat.isSmallCurtainRoot(DoubleBlockHalf.UPPER));
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
