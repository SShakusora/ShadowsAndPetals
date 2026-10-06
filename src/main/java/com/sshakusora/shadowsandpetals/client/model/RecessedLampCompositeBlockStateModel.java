package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.blockentity.RecessedLampBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Composes the lamp model with the slab stored in its block entity. */
public final class RecessedLampCompositeBlockStateModel extends BakedModelWrapper<BakedModel>
        implements IDynamicBakedModel {
    private final Block expectedBlock;

    public RecessedLampCompositeBlockStateModel(Block expectedBlock, BakedModel delegate) {
        super(delegate);
        this.expectedBlock = expectedBlock;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData originalData) {
        ModelData base = super.getModelData(level, pos, state, originalData);
        if (state.getBlock() != expectedBlock) {
            return base;
        }
        return RecessedLampConnectedBlockStateModel.addConnectionData(level, pos, state, base);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        ChunkRenderTypeSet result = super.getRenderTypes(state, random, data);
        if (state.getBlock() != expectedBlock) {
            return result;
        }

        result = ChunkRenderTypeSet.union(result, ChunkRenderTypeSet.of(RenderType.cutout()));

        BlockState storedSlab = data.get(RecessedLampBlockEntity.STORED_SLAB_MODEL_PROPERTY);
        if (!RecessedLampBlockEntity.isValidStoredSlab(storedSlab)) {
            return result;
        }

        BakedModel slabModel = BakedModelSupport.blockModel(storedSlab);
        ChunkRenderTypeSet slabTypes = slabModel.getRenderTypes(storedSlab, random, ModelData.EMPTY);
        return ChunkRenderTypeSet.union(result, slabTypes);
    }

    @Override
    public List<BakedQuad> getQuads(
            BlockState state,
            @Nullable Direction face,
            RandomSource random,
            ModelData data,
            @Nullable RenderType renderType
    ) {
        List<BakedQuad> lampQuads = RecessedLampConnectedBlockStateModel.getConnectedLampQuads(
                state, face, random, data, renderType, originalModel);
        List<BakedQuad> result = new ArrayList<>(lampQuads);
        if (state.getBlock() != expectedBlock) {
            return result;
        }
        BlockState storedSlab = data.get(RecessedLampBlockEntity.STORED_SLAB_MODEL_PROPERTY);
        if (!RecessedLampBlockEntity.isValidStoredSlab(storedSlab)) {
            return result;
        }
        BakedModel slabModel = BakedModelSupport.blockModel(storedSlab);
        List<BakedQuad> slabQuads = getQuadsForRenderType(
                slabModel, storedSlab, face, random, ModelData.EMPTY, renderType);
        result.addAll(slabQuads);
        return result;
    }

    private static List<BakedQuad> getQuadsForRenderType(
            BakedModel model,
            BlockState state,
            @Nullable Direction face,
            RandomSource random,
            ModelData data,
            @Nullable RenderType renderType
    ) {
        if (renderType != null && !model.getRenderTypes(state, random, data).contains(renderType)) {
            return List.of();
        }
        return BakedModelSupport.getQuads(model, state, face, random, data, renderType);
    }

}
