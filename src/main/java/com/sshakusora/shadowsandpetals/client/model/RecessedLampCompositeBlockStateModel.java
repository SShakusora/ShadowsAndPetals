package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.blockentity.RecessedLampBlockEntity;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class RecessedLampCompositeBlockStateModel extends DelegateBlockStateModel implements DynamicBlockStateModel {
    private final Map<BlockState, BlockStateModel> slabModels;

    public RecessedLampCompositeBlockStateModel(
            BlockStateModel lampModel,
            Map<BlockState, BlockStateModel> slabModels
    ) {
        super(lampModel);
        this.slabModels = slabModels;
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        long seed = random.nextLong();
        random.setSeed(seed);
        Object lampKey = delegate.createGeometryKey(level, pos, state, random);
        if (lampKey == null) {
            return null;
        }

        BlockState storedSlab = getStoredSlab(level, pos);
        BlockStateModel slabModel = storedSlab != null ? slabModels.get(storedSlab) : null;
        Object slabKey = null;
        if (slabModel != null) {
            random.setSeed(seed);
            slabKey = slabModel.createGeometryKey(level, pos, storedSlab, random);
            if (slabKey == null) {
                return null;
            }
        }
        return new GeometryKey(lampKey, storedSlab, slabKey);
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts
    ) {
        long seed = random.nextLong();
        random.setSeed(seed);
        delegate.collectParts(level, pos, state, random, parts);

        BlockState storedSlab = getStoredSlab(level, pos);
        BlockStateModel slabModel = storedSlab != null ? slabModels.get(storedSlab) : null;
        if (slabModel != null) {
            random.setSeed(seed);
            slabModel.collectParts(level, pos, storedSlab, random, parts);
        }
    }

    @Override
    public Material.Baked particleMaterial(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state
    ) {
        BlockState storedSlab = getStoredSlab(level, pos);
        BlockStateModel slabModel = storedSlab != null ? slabModels.get(storedSlab) : null;
        return slabModel != null
                ? slabModel.particleMaterial(level, pos, storedSlab)
                : delegate.particleMaterial(level, pos, state);
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state
    ) {
        int flags = delegate.materialFlags(level, pos, state);
        BlockState storedSlab = getStoredSlab(level, pos);
        BlockStateModel slabModel = storedSlab != null ? slabModels.get(storedSlab) : null;
        return slabModel != null
                ? flags | slabModel.materialFlags(level, pos, storedSlab)
                : flags;
    }

    private static @Nullable BlockState getStoredSlab(BlockAndTintGetter level, BlockPos pos) {
        BlockState state = level.getModelData(pos).get(RecessedLampBlockEntity.STORED_SLAB_MODEL_PROPERTY);
        return RecessedLampBlockEntity.isValidStoredSlab(state) ? state : null;
    }

    private record GeometryKey(
            @Nullable Object lampKey,
            @Nullable BlockState storedSlab,
            @Nullable Object slabKey
    ) {
    }
}
