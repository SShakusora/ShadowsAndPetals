package com.sshakusora.shadowsandpetals.item.hammer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HammerItemTest {
    @Test
    void impactTicksFollowTheExportedAnimationWithoutAccumulatingDrift() {
        List<Integer> impactTicks = new ArrayList<>();
        for (int usedTicks = 0; usedTicks <= 108; usedTicks++) {
            if (HammerItem.AnimationTiming.isImpactTick(usedTicks)) {
                impactTicks.add(usedTicks);
            }
        }

        assertEquals(
                List.of(13, 29, 45, 61, 76, 92, 108),
                impactTicks);
    }
}
