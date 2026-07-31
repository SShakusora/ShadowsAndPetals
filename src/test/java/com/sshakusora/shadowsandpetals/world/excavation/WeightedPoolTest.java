package com.sshakusora.shadowsandpetals.world.excavation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedPoolTest {
    @Test
    void selectsEntriesByTheirCumulativeWeights() {
        WeightedPool<String> pool = WeightedPool.of(List.of(
                new WeightedPool.Entry<>("seaweed", 3),
                new WeightedPool.Entry<>("shell", 2),
                new WeightedPool.Entry<>("driftwood", 1)
        ));

        assertEquals("seaweed", pool.getByRoll(0).orElseThrow());
        assertEquals("seaweed", pool.getByRoll(2).orElseThrow());
        assertEquals("shell", pool.getByRoll(3).orElseThrow());
        assertEquals("shell", pool.getByRoll(4).orElseThrow());
        assertEquals("driftwood", pool.getByRoll(5).orElseThrow());
        assertTrue(pool.getByRoll(6).isEmpty());
    }

    @Test
    void emptyPoolReturnsNoEntry() {
        assertTrue(WeightedPool.empty().getByRoll(0).isEmpty());
    }
}
