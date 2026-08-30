package com.sshakusora.shadowsandpetals.client.renderer;

import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BonsaiRenderRoutingTest {
    @Test
    void maximumEnvelopeContainsEveryVisualState() {
        AABB maximum = BonsaiRenderRouting.maxTreeBounds();
        for (BonsaiBlockEntity.Shape shape : BonsaiBlockEntity.Shape.values()) {
            for (boolean dead : new boolean[]{false, true}) {
                for (int rotation = 0; rotation < 16; rotation++) {
                    AABB bounds = BonsaiRenderRouting.treeBounds(shape, dead, rotation);
                    assertTrue(maximum.minX <= bounds.minX);
                    assertTrue(maximum.minY <= bounds.minY);
                    assertTrue(maximum.minZ <= bounds.minZ);
                    assertTrue(maximum.maxX >= bounds.maxX);
                    assertTrue(maximum.maxY >= bounds.maxY);
                    assertTrue(maximum.maxZ >= bounds.maxZ);
                }
            }
        }
    }

    @Test
    void interiorPositionUsesChunkBuffer() {
        assertFalse(BonsaiRenderRouting.usesBer(new BlockPos(2, 2, 2)));
    }

    @Test
    void sectionBoundaryUsesBerFallback() {
        assertTrue(BonsaiRenderRouting.usesBer(new BlockPos(0, 2, 2)));
        assertTrue(BonsaiRenderRouting.usesBer(new BlockPos(14, 2, 2)));
        assertTrue(BonsaiRenderRouting.usesBer(new BlockPos(2, 15, 2)));
    }
}
