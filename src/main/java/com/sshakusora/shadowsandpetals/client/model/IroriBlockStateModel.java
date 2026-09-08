package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPart;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillBlock;
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
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
        RandomSource random
    ) {
        if (state == null || state.getBlock() != expectedBlock) {
            return new FallbackGeometryKey(state == null ? expectedBlock : state.getBlock());
        }

        long seed = random.nextLong();
        random.setSeed(seed);
        Object delegateKey = delegate.createGeometryKey(level, pos, state, random);
        if (delegateKey == null) {
            return null;
        }

        IroriGrillPart grillPart = grillPartAt(level, pos, state);
        Object grillKey = null;
        if (grillPart != null) {
            BlockStateModel grillModel = BlockModelRegistry.IRORI_GRILL_LOWER.get(grillPart);
            if (grillModel != null) {
                random.setSeed(seed);
                grillKey = grillModel.createGeometryKey(level, pos, state, random);
                if (grillKey == null) {
                    return null;
                }
            }
        }
        return new GeometryKey(delegateKey, grillPart, grillKey);
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts
    ) {
        if (state == null || state.getBlock() != expectedBlock) {
            delegate.collectParts(random, parts);
            return;
        }

        long seed = random.nextLong();
        random.setSeed(seed);
        List<BlockStateModelPart> delegateParts = new ArrayList<>();
        delegate.collectParts(level, pos, state, random, delegateParts);
        for (BlockStateModelPart part : delegateParts) {
            parts.add(new ForceAmbientOcclusionPart(part));
        }

        IroriGrillPart grillPart = grillPartAt(level, pos, state);
        if (grillPart == null) {
            return;
        }
        BlockStateModel grillModel = BlockModelRegistry.IRORI_GRILL_LOWER.get(grillPart);
        if (grillModel == null) {
            return;
        }
        random.setSeed(seed);
        List<BlockStateModelPart> grillParts = new ArrayList<>();
        grillModel.collectParts(level, pos, state, random, grillParts);
        for (BlockStateModelPart part : grillParts) {
            parts.add(new ForceAmbientOcclusionPart(part));
        }
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return delegate.particleMaterial(level, pos, state);
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int flags = delegate.materialFlags(level, pos, state);
        IroriGrillPart grillPart = grillPartAt(level, pos, state);
        if (grillPart == null) {
            return flags;
        }
        BlockStateModel grillModel = BlockModelRegistry.IRORI_GRILL_LOWER.get(grillPart);
        return grillModel == null ? flags : flags | grillModel.materialFlags(level, pos, state);
    }

    private static @Nullable IroriGrillPart grillPartAt(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state
    ) {
        if (!IroriBlock.hasGrill(state)) {
            return null;
        }
        BlockState upperState = level.getBlockState(pos.above());
        if (!(upperState.getBlock() instanceof IroriGrillBlock)) {
            return null;
        }
        IroriGrillPart part = upperState.getValue(IroriGrillBlock.GRILL_PART);
        BlockPos masterPos = part.masterPosition(pos.above());
        if (!IroriBlock.hasGrill(level.getBlockState(masterPos))) {
            return null;
        }
        return part;
    }

    private record GeometryKey(
            @Nullable Object delegateKey,
            @Nullable IroriGrillPart grillPart,
            @Nullable Object grillKey
    ) {
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
