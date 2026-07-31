package com.sshakusora.shadowsandpetals.api.excavation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandExcavationDropDataTest {
    @Test
    void decodesCategoryWeightAndCountRange() {
        SandExcavationDropData data = SandExcavationDropData.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("""
                        {
                          "category": "seafood",
                          "weight": 7,
                          "min_count": 1,
                          "max_count": 3
                        }
                        """)
        ).getOrThrow();

        assertEquals(SandExcavationDropCategory.SEAFOOD, data.category());
        assertEquals(7, data.weight());
        assertEquals(1, data.minCount());
        assertEquals(3, data.maxCount());
    }

    @Test
    void rejectsInvertedCountRange() {
        var result = SandExcavationDropData.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("""
                        {
                          "category": "trash",
                          "min_count": 3,
                          "max_count": 1
                        }
                        """)
        );

        assertTrue(result.error().isPresent());
    }

    @Test
    void rejectsNonPositiveWeight() {
        var result = SandExcavationDropData.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("""
                        {
                          "category": "trash",
                          "weight": 0
                        }
                        """)
        );

        assertTrue(result.error().isPresent());
    }
}
