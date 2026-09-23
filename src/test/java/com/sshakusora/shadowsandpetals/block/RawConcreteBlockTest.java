package com.sshakusora.shadowsandpetals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawConcreteBlockTest {
    @Test
    void selectedVariantsMapToTheRegisteredTextureSheets() {
        BlockPos pos = new BlockPos(2, 4, 6);

        assertEquals(0, RawConcreteBlock.textureIndexForVariant(
                RawConcreteBlock.TextureVariant.BLANK, pos, Direction.UP));
        assertEquals(1, RawConcreteBlock.textureIndexForVariant(
                RawConcreteBlock.TextureVariant.SINGLE_HOLE, pos, Direction.UP));
        assertEquals(2, RawConcreteBlock.textureIndexForVariant(
                RawConcreteBlock.TextureVariant.FOUR_HOLE, pos, Direction.UP));
    }

    @Test
    void textureSelectionDoesNotDependOnPositionOrFace() {
        for (BlockPos pos : new BlockPos[]{new BlockPos(2, 4, 6), new BlockPos(-1, 7, -9)}) {
            for (Direction face : Direction.values()) {
                assertEquals(1, RawConcreteBlock.textureIndexForVariant(
                        RawConcreteBlock.TextureVariant.SINGLE_HOLE, pos, face));
            }
        }
    }

    @Test
    void statePropertyCycleOrderIsBlankSingleHoleAndFourHole() {
        assertEquals(
                List.of(
                        RawConcreteBlock.TextureVariant.BLANK,
                        RawConcreteBlock.TextureVariant.SINGLE_HOLE,
                        RawConcreteBlock.TextureVariant.FOUR_HOLE),
                List.copyOf(RawConcreteBlock.TEXTURE.getPossibleValues()));
    }

    @Test
    void invalidStatesUseTheBaseTextureInsteadOfReadingMissingProperties() {
        assertEquals(0, RawConcreteBlock.selectTextureIndex(null, BlockPos.ZERO, Direction.UP));
    }
}
