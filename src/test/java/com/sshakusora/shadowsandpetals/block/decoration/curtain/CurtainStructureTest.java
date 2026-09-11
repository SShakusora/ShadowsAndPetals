package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CurtainStructureTest {
    @Test
    void smallAndLargeStructuresUseTheSameMutualPartnerGeometry() {
        BlockPos anchor = new BlockPos(12, 8, -4);

        for (Direction facing : new Direction[]{
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            for (CurtainBlock.Side side : CurtainBlock.Side.values()) {
                BlockPos partner = CurtainStructure.partnerAnchor(anchor, facing, side);
                BlockPos reverse = CurtainStructure.partnerAnchor(partner, facing, side.mirror());

                assertEquals(anchor.relative(CurtainStructure.towardPartner(facing, side)), partner);
                assertEquals(anchor, reverse, facing + " " + side);
            }
        }
    }

    @Test
    void logicalUnitSizesRemainTwoAndFourMembers() {
        BlockPos anchor = new BlockPos(2, 5, 9);
        assertEquals(2, new BlockPos[]{anchor, anchor.above()}.length);
        assertEquals(4, LargeCurtainGeometry.structurePositions(anchor, Direction.EAST).length);
    }

    @Test
    void animationIsRequiredOnlyWhenAnyLogicalMemberDiffersFromTheTargetPose() {
        assertTrue(CurtainStructure.requiresAnimation(Stream.of(false, false), true));
        assertTrue(CurtainStructure.requiresAnimation(Stream.of(true, true), false));
        assertTrue(CurtainStructure.requiresAnimation(Stream.of(false, true), true));
        assertTrue(CurtainStructure.requiresAnimation(Stream.of(false, true), false));
        assertFalse(CurtainStructure.requiresAnimation(Stream.of(false, false), false));
        assertFalse(CurtainStructure.requiresAnimation(Stream.of(true, true), true));
    }
}
