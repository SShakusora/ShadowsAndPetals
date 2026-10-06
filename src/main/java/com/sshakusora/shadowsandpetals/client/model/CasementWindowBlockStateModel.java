package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowBlock;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowSide;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Bakes the authored open-window model into the static block-state path.
 * During a transition the block state is invisible and the BER applies the
 * same pivot transform at the requested partial-tick angle.
 */
public final class CasementWindowBlockStateModel extends DelegateBlockStateModel
        implements DynamicBlockStateModel {
    private final boolean open;
    private final Vec3 pivot;
    private final float angle;

    @SuppressWarnings("unused")
    public CasementWindowBlockStateModel(
            CasementWindowBlock block,
            BlockState bakedState,
            BlockStateModel delegate
    ) {
        super(delegate);
        this.open = bakedState.getValue(CasementWindowBlock.OPEN);
        CasementWindowSide side = bakedState.getValue(CasementWindowBlock.SIDE);
        this.pivot = CasementWindowGeometry.pivot(
                bakedState.getValue(CasementWindowBlock.FACING), side);
        this.angle = CasementWindowGeometry.targetAngle(side);
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        List<BlockStateModelPart> originals = new ArrayList<>();
        delegate.collectParts(random, originals);
        addTransformed(originals, parts);
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        return new GeometryKey(
                delegate.createGeometryKey(level, pos, state, random), open, pivot, angle);
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts
    ) {
        List<BlockStateModelPart> originals = new ArrayList<>();
        delegate.collectParts(level, pos, state, random, originals);
        addTransformed(originals, parts);
    }

    private void addTransformed(
            List<BlockStateModelPart> originals,
            List<BlockStateModelPart> output
    ) {
        if (!open) {
            output.addAll(originals);
            return;
        }
        for (BlockStateModelPart original : originals) {
            output.add(new RotatedPart(original, pivot, angle));
        }
    }

    private record GeometryKey(
            @Nullable Object delegateKey,
            boolean open,
            Vec3 pivot,
            float angle
    ) {
    }

    private static final class RotatedPart implements BlockStateModelPart {
        private final List<BakedQuad>[] quadsByDirection;
        private final List<BakedQuad> generalQuads;
        private final TriState ambientOcclusion;
        private final Material.Baked particleMaterial;
        private final int materialFlags;

        @SuppressWarnings("unchecked")
        private RotatedPart(BlockStateModelPart delegate, Vec3 pivot, float angle) {
            quadsByDirection = new List[Direction.values().length];
            for (Direction direction : Direction.values()) {
                quadsByDirection[direction.get3DDataValue()] = List.of();
            }
            generalQuads = transformAll(delegate, pivot, angle);
            ambientOcclusion = delegate.ambientOcclusion();
            particleMaterial = delegate.particleMaterial();
            int flags = delegate.materialFlags();
            for (BakedQuad quad : generalQuads) {
                flags |= quad.materialInfo().flags();
            }
            materialFlags = flags;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null
                    ? generalQuads
                    : quadsByDirection[direction.get3DDataValue()];
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ambientOcclusion != TriState.FALSE;
        }

        @Override
        public TriState ambientOcclusion() {
            return ambientOcclusion;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particleMaterial;
        }

        @Override
        public int materialFlags() {
            return materialFlags;
        }

        private static List<BakedQuad> transformAll(
                BlockStateModelPart source,
                Vec3 pivot,
                float angle
        ) {
            int size = source.getQuads(null).size();
            for (Direction direction : Direction.values()) {
                size += source.getQuads(direction).size();
            }
            if (size == 0) {
                return List.of();
            }
            List<BakedQuad> transformed = new ArrayList<>(size);
            for (Direction direction : Direction.values()) {
                for (BakedQuad quad : source.getQuads(direction)) {
                    transformed.add(BakedQuadTransform.rotateY(quad, pivot, angle));
                }
            }
            for (BakedQuad quad : source.getQuads(null)) {
                transformed.add(BakedQuadTransform.rotateY(quad, pivot, angle));
            }
            return List.copyOf(transformed);
        }
    }
}
