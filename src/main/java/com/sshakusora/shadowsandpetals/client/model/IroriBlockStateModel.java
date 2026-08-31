package com.sshakusora.shadowsandpetals.client.model;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public final class IroriBlockStateModel extends DelegateBlockStateModel implements DynamicBlockStateModel {
    private final Block expectedBlock;

    public IroriBlockStateModel(Block expectedBlock, BlockStateModel delegate) {
        super(delegate);
        this.expectedBlock = expectedBlock;
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (state == null || state.getBlock() != expectedBlock) {
            return new FallbackGeometryKey(state == null ? expectedBlock : state.getBlock());
        }

        return delegate.createGeometryKey(level, pos, state, random);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        if (state == null || state.getBlock() != expectedBlock) {
            delegate.collectParts(random, parts);
            return;
        }

        List<BlockStateModelPart> delegateParts = new ArrayList<>();
        delegate.collectParts(level, pos, state, random, delegateParts);
        for (BlockStateModelPart part : delegateParts) {
            parts.add(new ForceAmbientOcclusionPart(part));
        }
    }

    private record FallbackGeometryKey(Block stateBlock) {
    }

    private record ForceAmbientOcclusionPart(BlockStateModelPart delegate) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return delegate.getQuads(direction);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public TriState ambientOcclusion() {
            return TriState.TRUE;
        }

        @Override
        public Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return delegate.materialFlags();
        }
    }
}
