package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.client.ct.CTBlockStateModel;
import com.sshakusora.shadowsandpetals.client.ct.CTRegistry;
import com.sshakusora.shadowsandpetals.client.ct.CTTextureSelector;
import com.sshakusora.shadowsandpetals.client.ct.CTTextureType;
import com.sshakusora.shadowsandpetals.client.model.bonsai.BonsaiPotBlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"DataFlowIssue", "NullableProblems"})
class DynamicBlockStateModelSafetyTest {
    // The production breaking-overlay path supplies AIR as the state. Plain JUnit
    // does not bootstrap Minecraft's registries, so null is used as an equally
    // invalid context to exercise the same defensive fallback branch.
    private static final BlockAndTintGetter EMPTY_LEVEL = BlockAndTintGetter.EMPTY;
    private static final BlockPos EMPTY_POS = BlockPos.ZERO;

    @Test
    void bonsaiFallsBackForBreakingOverlayState() {
        Block block = null;
        RecordingModel delegate = new RecordingModel();
        BonsaiPotBlockStateModel model = new BonsaiPotBlockStateModel(block, delegate);

        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create(), parts);

        assertEquals(1, delegate.contextFreeCollects);
        assertNotNull(model.createGeometryKey(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create()));
    }

    @Test
    void bonsaiBreakingOverlayRetainsBakedRotation() {
        RecordingModel delegate = new RecordingModel();
        BonsaiPotBlockStateModel model = new BonsaiPotBlockStateModel(null, delegate);

        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create(), parts);

        assertEquals(1, delegate.contextFreeCollects);
        assertEquals(1, parts.size());
        assertNotSame(delegate.part, parts.getFirst());
    }

    @Test
    void connectedTextureFallsBackForBreakingOverlayState() {
        Block block = null;
        RecordingModel delegate = new RecordingModel();
        CTRegistry.CTEntry entry = new CTRegistry.CTEntry(
                Identifier.withDefaultNamespace("block/raw_concrete/base"),
                List.of(Identifier.withDefaultNamespace("block/raw_concrete/connected")),
                CTTextureSelector.FIRST,
                CTTextureType.OMNIDIRECTIONAL,
                0);
        CTBlockStateModel model = new CTBlockStateModel(block, delegate, entry);

        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create(), parts);

        assertEquals(1, delegate.contextFreeCollects);
        assertNotNull(model.createGeometryKey(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create()));
    }

    @Test
    void woodPostFallsBackForBreakingOverlayState() {
        Block block = null;
        RecordingModel delegate = new RecordingModel();
        WoodPostBlockStateModel model = new WoodPostBlockStateModel(block, delegate);

        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create(), parts);

        assertEquals(1, delegate.contextFreeCollects);
        assertNotNull(model.createGeometryKey(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create()));
    }

    @Test
    void iroriFallsBackForBreakingOverlayState() {
        Block block = null;
        RecordingModel delegate = new RecordingModel();
        IroriBlockStateModel model = new IroriBlockStateModel(block, delegate);

        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create(), parts);

        assertEquals(1, delegate.contextFreeCollects);
        assertNotNull(model.createGeometryKey(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create()));
    }

    @Test
    void recessedLampFallsBackForBreakingOverlayState() {
        Block block = null;
        RecordingModel delegate = new RecordingModel();
        RecessedLampCompositeBlockStateModel model =
                new RecessedLampCompositeBlockStateModel(block, delegate, Map.of());

        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create(), parts);

        assertEquals(1, delegate.contextFreeCollects);
        assertNotNull(model.createGeometryKey(EMPTY_LEVEL, EMPTY_POS, null, RandomSource.create()));
    }

    private static final class RecordingModel implements BlockStateModel {
        private int contextFreeCollects;
        private final BlockStateModelPart part = new RecordingPart();

        @Override
        @Deprecated
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            contextFreeCollects++;
            output.add(part);
        }

        @Override
        public void collectParts(
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                RandomSource random,
                List<BlockStateModelPart> output
        ) {
            throw new AssertionError("invalid state was forwarded to a contextual delegate");
        }

        @Override
        @Deprecated
        public Material.Baked particleMaterial() {
            return null;
        }

        @Override
        @Deprecated
        public int materialFlags() {
            return 0;
        }

        @Override
        public Object createGeometryKey(
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                RandomSource random
        ) {
            throw new AssertionError("invalid state was forwarded to a contextual delegate");
        }
    }

    private static final class RecordingPart implements BlockStateModelPart {
        @Override
        public List<net.minecraft.client.resources.model.geometry.BakedQuad> getQuads(@Nullable Direction direction) {
            return List.of();
        }

        @Override
        @Deprecated
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public Material.Baked particleMaterial() {
            return null;
        }

        @Override
        public int materialFlags() {
            return 0;
        }
    }
}
