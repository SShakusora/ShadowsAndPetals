package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowBlock;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowSide;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies the final open pose to the chunk renderer. During a transition the
 * block is invisible and the BER uses the same pivot and quarter-turn.
 */
public final class CasementWindowBlockStateModel extends BakedModelWrapper<BakedModel>
        implements IDynamicBakedModel {
    private final boolean open;
    private final CasementWindowState state;

    public CasementWindowBlockStateModel(
            CasementWindowBlock block,
            BlockState bakedState,
            BakedModel delegate
    ) {
        super(delegate);
        this.open = bakedState.getValue(CasementWindowBlock.OPEN);
        this.state = new CasementWindowState(
                bakedState.getValue(CasementWindowBlock.FACING),
                bakedState.getValue(CasementWindowBlock.SIDE)
        );
    }

    @Override
    public List<BakedQuad> getQuads(
            BlockState blockState,
            @Nullable Direction face,
            RandomSource random
    ) {
        if (!open) {
            return BakedModelSupport.getQuads(
                    originalModel, blockState, face, random, ModelData.EMPTY, null);
        }
        if (face != null) {
            return List.of();
        }
        return transformAll(blockState, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(
            BlockState blockState,
            @Nullable Direction face,
            RandomSource random,
            ModelData data,
            @Nullable RenderType renderType
    ) {
        if (!open) {
            return BakedModelSupport.getQuads(
                    originalModel, blockState, face, random, data, renderType);
        }
        if (face != null) {
            return List.of();
        }
        return transformAll(blockState, random, data, renderType);
    }

    private List<BakedQuad> transformAll(
            BlockState blockState,
            RandomSource random,
            ModelData data,
            @Nullable RenderType renderType
    ) {
        List<BakedQuad> source = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            source.addAll(BakedModelSupport.getQuads(
                    originalModel, blockState, direction, random, data, renderType));
        }
        source.addAll(BakedModelSupport.getQuads(
                originalModel, blockState, null, random, data, renderType));

        var pivot = CasementWindowGeometry.pivot(state.facing(), state.side());
        float angle = CasementWindowGeometry.targetAngle(state.side());
        List<BakedQuad> result = new ArrayList<>(source.size());
        for (BakedQuad quad : source) {
            result.add(BakedQuadTransform.rotateY(quad, pivot, angle));
        }
        return result;
    }

    private record CasementWindowState(
            Direction facing,
            CasementWindowSide side
    ) {
    }
}
