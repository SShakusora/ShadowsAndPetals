package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Coordinates interaction, redstone and animation for either curtain family.
 */
final class CurtainPairController {
    private CurtainPairController() {
    }

    static boolean isPoweredPair(Level level, BlockPos pos, BlockState state) {
        Optional<CurtainStructure> current = CurtainStructure.resolve(level, pos);
        if (current.isEmpty()) {
            return state.getValue(CurtainBlock.POWERED) || level.hasNeighborSignal(pos);
        }

        CurtainStructure structure = current.get();
        if (structure.hasStoredPower(level) || structure.hasLiveRedstoneSignal(level)) {
            return true;
        }
        return findPartner(level, structure)
                .map(partner -> partner.hasStoredPower(level) || partner.hasLiveRedstoneSignal(level))
                .orElse(false);
    }

    static void setPowered(Level level, BlockPos pos, boolean powered) {
        CurtainStructure.resolve(level, pos).ifPresent(structure -> structure.setPowered(level, powered));
    }

    static void togglePair(Level level, BlockPos pos, boolean requestedOpen) {
        Optional<CurtainStructure> current = CurtainStructure.resolve(level, pos);
        if (current.isEmpty()) {
            return;
        }

        CurtainStructure structure = current.get();
        Optional<CurtainStructure> partner = findPartner(level, structure);
        boolean localPowered = structure.hasLiveRedstoneSignal(level);
        boolean partnerPowered = partner.map(candidate -> candidate.hasLiveRedstoneSignal(level)).orElse(false);
        boolean targetOpen = requestedOpen || localPowered || partnerPowered;
        long gameTime = level.getGameTime();

        structure.setOpen(level, targetOpen, gameTime);
        partner.ifPresent(candidate -> candidate.setOpen(level, targetOpen, gameTime));
    }

    private static Optional<CurtainStructure> findPartner(Level level, CurtainStructure structure) {
        BlockPos partnerAnchor = structure.partnerAnchor();
        return CurtainStructure.resolve(level, partnerAnchor)
                .filter(partner -> partner.anchor().equals(partnerAnchor))
                .filter(partner -> partner.facing() == structure.facing())
                .filter(partner -> partner.side() != structure.side())
                .filter(partner -> partner.partnerAnchor().equals(structure.anchor()));
    }
}