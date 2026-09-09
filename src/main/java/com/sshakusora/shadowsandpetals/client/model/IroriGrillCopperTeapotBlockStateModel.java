package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class IroriGrillCopperTeapotBlockStateModel
        extends DelegateBlockStateModel implements DynamicBlockStateModel {
    private final Block expectedBlock;
    private final Map<BlockState, BlockStateModel> grillModels;

    public IroriGrillCopperTeapotBlockStateModel(
            Block expectedBlock,
            BlockStateModel teapotModel,
            Map<BlockState, BlockStateModel> grillModels
    ) {
        super(teapotModel);
        this.expectedBlock = expectedBlock;
        this.grillModels = grillModels;
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        if (state == null || state.getBlock() != expectedBlock) {
            return new GeometryKey(state == null ? expectedBlock : state.getBlock(), null, null, null);
        }

        long seed = random.nextLong();
        random.setSeed(seed);
        Object teapotKey = delegate.createGeometryKey(level, pos, state, random);
        if (teapotKey == null) {
            return null;
        }

        BlockState grillState = grillState(state);
        BlockStateModel grillModel = grillModels.get(grillState);
        Object grillKey = null;
        if (grillModel != null) {
            random.setSeed(seed);
            grillKey = grillModel.createGeometryKey(level, pos, grillState, random);
            if (grillKey == null) {
                return null;
            }
        }
        return new GeometryKey(expectedBlock, teapotKey, grillState, grillKey);
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
        delegate.collectParts(level, pos, state, random, parts);

        BlockState grillState = grillState(state);
        BlockStateModel grillModel = grillModels.get(grillState);
        if (grillModel != null) {
            random.setSeed(seed);
            grillModel.collectParts(level, pos, grillState, random, parts);
        }
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return state == null || state.getBlock() != expectedBlock
                ? delegate.particleMaterial()
                : delegate.particleMaterial(level, pos, state);
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (state == null || state.getBlock() != expectedBlock) {
            return delegate.materialFlags();
        }

        int flags = delegate.materialFlags(level, pos, state);
        BlockState grillState = grillState(state);
        BlockStateModel grillModel = grillModels.get(grillState);
        return grillModel == null ? flags : flags | grillModel.materialFlags(level, pos, grillState);
    }

    private static BlockState grillState(BlockState teapotState) {
        return BlockRegistry.IRORI_GRILL.get()
                .defaultBlockState()
                .setValue(IroriGrillBlock.GRILL_PART, teapotState.getValue(IroriGrillBlock.GRILL_PART))
                .setValue(IroriGrillBlock.WATERLOGGED, teapotState.getValue(IroriGrillBlock.WATERLOGGED));
    }

    private record GeometryKey(
            Block block,
            @Nullable Object teapotKey,
            @Nullable BlockState grillState,
            @Nullable Object grillKey
    ) {
    }
}
