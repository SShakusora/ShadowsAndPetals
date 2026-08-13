package com.sshakusora.shadowsandpetals.client.tooltip;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockeryPreviewRendererTest {
    @Test
    void pipTextureDimensionMatchesThePhysicalOffscreenTarget() {
        assertEquals(192, RockeryPreviewRenderer.pipTextureDimension(64, 3));
        assertEquals(1, RockeryPreviewRenderer.pipTextureDimension(0, 3));
    }

    @Test
    void selectionShapeDoesNotExpandIntoAnAdjacentStone() {
        AABB first = RockeryPreviewRenderer.selectionShape(
                new RockeryDimensions(2, 1, 1),
                new Vec3i(0, 0, 0)
        ).bounds();
        assertTrue(first.minX < 0.0D);
        assertEquals(1.0D, first.maxX);

        AABB second = RockeryPreviewRenderer.selectionShape(
                new RockeryDimensions(2, 1, 1),
                new Vec3i(1, 0, 0)
        ).bounds();
        assertEquals(0.0D, second.minX);
        assertTrue(second.maxX > 1.0D);
    }
}
