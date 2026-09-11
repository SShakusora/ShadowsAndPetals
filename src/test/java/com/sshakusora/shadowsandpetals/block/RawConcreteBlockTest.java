package com.sshakusora.shadowsandpetals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RawConcreteBlockTest {
    @Test
    void holeCoordinatesUseTheHoleAndDenseHoleTextures() {
        BlockPos holePos = new BlockPos(2, 4, 6);

        assertTrue(RawConcreteBlock.isHolePosition(holePos, Direction.UP));
        assertEquals(1, RawConcreteBlock.selectTextureIndex(false, holePos, Direction.UP));
        assertEquals(2, RawConcreteBlock.selectTextureIndex(true, holePos, Direction.UP));
    }

    @Test
    void nonHoleCoordinatesAlwaysUseTheContinuousTexture() {
        BlockPos nonHolePos = new BlockPos(1, 4, 6);

        assertFalse(RawConcreteBlock.isHolePosition(nonHolePos, Direction.UP));
        assertEquals(0, RawConcreteBlock.selectTextureIndex(false, nonHolePos, Direction.UP));
        assertEquals(0, RawConcreteBlock.selectTextureIndex(true, nonHolePos, Direction.UP));
    }

    @Test
    void holeCoordinatesUseTheSameRuleForNegativePositions() {
        assertTrue(RawConcreteBlock.isHolePosition(new BlockPos(-2, 0, -4), Direction.NORTH));
        assertFalse(RawConcreteBlock.isHolePosition(new BlockPos(-1, 0, -4), Direction.NORTH));
    }

    @Test
    void invalidStatesUseTheBaseTextureInsteadOfReadingMissingProperties() {
        assertEquals(0, RawConcreteBlock.selectTextureIndex(null, BlockPos.ZERO, Direction.UP));
    }
}
