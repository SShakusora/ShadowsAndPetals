package com.sshakusora.shadowsandpetals.client.model.bonsai;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"DataFlowIssue", "NullableProblems"})
class BonsaiTreeGeometryCacheTest {
    @Test
    void fixedPartsModelReusesExtractedParts() {
        TestPart part = new TestPart(BakedQuad.FLAG_TRANSLUCENT);
        BlockStateModel model = BonsaiTreeGeometryCache.fixedPartsModel(List.of(part));
        List<BlockStateModelPart> output = new ArrayList<>();

        model.collectParts(RandomSource.create(7L), output);

        assertEquals(List.of(part), output);
        assertEquals(BakedQuad.FLAG_TRANSLUCENT, model.materialFlags());
    }

    @Test
    void rotatedPartsAreIndependentAndUseGeneralQuads() {
        TestPart part = new TestPart(0);
        List<BlockStateModelPart> source = List.of(part);

        List<BlockStateModelPart> rotated = BonsaiTreeGeometryCache.rotateParts(source, 3);

        assertEquals(1, rotated.size());
        assertNotSame(part, rotated.getFirst());
        assertSame(part, source.getFirst());
        for (Direction direction : Direction.values()) {
            assertTrue(rotated.getFirst().getQuads(direction).isEmpty());
        }
        assertTrue(rotated.getFirst().getQuads(null).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> rotated.add(part));
    }

    private record TestPart(int flags) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return List.of();
        }

        @Override
        @Deprecated
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public TriState ambientOcclusion() {
            return TriState.FALSE;
        }

        @Override
        public Material.Baked particleMaterial() {
            return null;
        }

        @Override
        public int materialFlags() {
            return flags;
        }
    }
}
