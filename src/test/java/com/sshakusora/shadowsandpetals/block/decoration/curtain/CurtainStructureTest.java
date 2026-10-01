package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CurtainStructureTest {
    @Test
    void everyCurtainSizeUsesTheSameMutualPartnerGeometry() {
        BlockPos anchor = new BlockPos(12, 8, -4);

        for (Direction facing : new Direction[]{
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            for (CurtainSide side : CurtainSide.values()) {
                BlockPos partner = CurtainStructure.partnerAnchor(anchor, facing, side);
                BlockPos reverse = CurtainStructure.partnerAnchor(partner, facing, side.mirror());

                assertEquals(anchor.relative(CurtainStructure.towardPartner(facing, side)), partner);
                assertEquals(anchor, reverse, facing + " " + side);
            }
        }
    }

    @Test
    void logicalUnitSizesRemainOneTwoAndFourMembers() {
        BlockPos anchor = new BlockPos(2, 5, 9);
        assertEquals(1, List.of(anchor).size());
        assertEquals(2, new BlockPos[]{anchor, anchor.above()}.length);
        assertEquals(4, LargeCurtainGeometry.structurePositions(anchor, Direction.EAST).length);
    }

    @Test
    void allCurtainSizeCombinationsCanPairWhenTheirGeometryMatches() {
        BlockPos rail = new BlockPos(12, 8, -4);

        for (Direction facing : new Direction[]{
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            for (CurtainSide side : CurtainSide.values()) {
                BlockPos partnerRail = CurtainStructure.partnerRailPosition(rail, facing, side);
                for (CurtainStructure.CurtainSize size : CurtainStructure.CurtainSize.values()) {
                    CurtainStructure structure = structureAtRail(rail, facing, side, size);
                    for (CurtainStructure.CurtainSize partnerSize : CurtainStructure.CurtainSize.values()) {
                        CurtainStructure partner = structure(
                                physicalAnchor(partnerRail, partnerSize),
                                facing, side.mirror(), partnerSize);

                        assertTrue(
                                CurtainPairController.isCompatiblePartner(structure, partner),
                                () -> size + " should pair with " + partnerSize
                                        + " for " + facing + " " + side);
                        assertTrue(
                                CurtainPairController.isCompatiblePartner(partner, structure),
                                () -> partnerSize + " should pair with " + size
                                        + " for " + facing + " " + side);
                    }
                }
            }
        }
    }

    @Test
    void curtainsMustShareTheUpperRailYCoordinate() {
        BlockPos rail = new BlockPos(12, 8, -4);
        Direction facing = Direction.NORTH;
        CurtainSide side = CurtainSide.LEFT;
        CurtainStructure structure = structureAtRail(
                rail, facing, side, CurtainStructure.CurtainSize.NORMAL);
        BlockPos partnerRail = structure.partnerRailPosition();

        for (CurtainStructure.CurtainSize partnerSize : CurtainStructure.CurtainSize.values()) {
            CurtainStructure aligned = structureAtRail(
                    partnerRail, facing, side.mirror(), partnerSize);
            CurtainStructure oneRowTooHigh = structureAtRail(
                    partnerRail.above(), facing, side.mirror(), partnerSize);

            assertTrue(CurtainPairController.isCompatiblePartner(structure, aligned),
                    () -> "aligned " + partnerSize + " curtain should pair");
            assertFalse(CurtainPairController.isCompatiblePartner(structure, oneRowTooHigh),
                    () -> "misaligned " + partnerSize + " curtain should not pair");
        }
    }

    @Test
    void pairingStillRequiresMutualGeometry() {
        BlockPos rail = new BlockPos(12, 8, -4);
        CurtainStructure structure = structure(
                rail, Direction.NORTH, CurtainSide.LEFT, CurtainStructure.CurtainSize.NORMAL);
        CurtainStructure wrongFacing = structure(
                rail.relative(Direction.EAST), Direction.SOUTH,
                CurtainSide.RIGHT, CurtainStructure.CurtainSize.LONG);
        CurtainStructure sameSide = structure(
                rail.relative(Direction.WEST), Direction.NORTH,
                CurtainSide.LEFT, CurtainStructure.CurtainSize.LARGE);

        assertFalse(CurtainPairController.isCompatiblePartner(structure, wrongFacing));
        assertFalse(CurtainPairController.isCompatiblePartner(structure, sameSide));
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

    private static CurtainStructure structure(
            BlockPos anchor,
            Direction facing,
            CurtainSide side,
            CurtainStructure.CurtainSize size
    ) {
        return new CurtainStructure(anchor, null, facing, side, List.of(anchor), size);
    }

    private static CurtainStructure structureAtRail(
            BlockPos rail,
            Direction facing,
            CurtainSide side,
            CurtainStructure.CurtainSize size
    ) {
        return structure(physicalAnchor(rail, size), facing, side, size);
    }

    private static BlockPos physicalAnchor(BlockPos rail, CurtainStructure.CurtainSize size) {
        return size == CurtainStructure.CurtainSize.NORMAL ? rail : rail.below();
    }
}
