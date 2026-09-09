package com.sshakusora.shadowsandpetals.blockentity.irori;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IroriBlockEntityWaterloggingTest {
    @Test
    void dryComponentDoesNotExtinguishSharedFire() {
        assertFalse(IroriComponentTopology.hasWaterloggedState(List.of(false, false)));
    }

    @Test
    void waterloggedNonMasterComponentExtinguishesSharedFire() {
        assertTrue(IroriComponentTopology.hasWaterloggedState(List.of(false, true)));
    }
}
