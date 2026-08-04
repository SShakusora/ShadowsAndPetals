package com.sshakusora.shadowsandpetals.api.outline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutlineGeometryTest {
    @Test
    void boxContainsItsTwelveEdges() {
        assertEquals(
                12,
                OutlineGeometry.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D).lines().size()
        );
    }

    @Test
    void octagonalPrismContainsTwoRingsAndEightSideEdges() {
        assertEquals(
                24,
                OutlineGeometry.octagonalPrism(
                        0.0D, 0.0D, 16.0D, 16.0D, 4.0D, 0.0D, 16.0D
                ).lines().size()
        );
    }
}
