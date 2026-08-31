package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.renderer.BonsaiBlockEntityRenderer;
import com.sshakusora.shadowsandpetals.client.renderer.BonsaiPartCacheKey;
import com.sshakusora.shadowsandpetals.client.renderer.BonsaiRenderRouting;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk-rendered bonsai pot/tree model with the block's full 16-step rotation.
 *
 * <p>Vanilla block-state JSON rotations are limited to quadrant rotations.
 * This wrapper applies the same 22.5-degree rotation used by the tree BER to
 * the baked pot quads, then caches the resulting parts for each segment.  The
 * tree is included from the immutable block-entity ModelData snapshot unless
 * this position is assigned to the cross-section BER fallback.</p>
 */
@SuppressWarnings("ConstantConditions")
public final class BonsaiPotBlockStateModel extends DelegateBlockStateModel
        implements DynamicBlockStateModel {
    private final Block expectedBlock;
    private final Map<Integer, List<BlockStateModelPart>> rotatedParts = new ConcurrentHashMap<>();
    private final Map<TreeGeometryKey, List<BlockStateModelPart>> rotatedTreeParts =
            new ConcurrentHashMap<>();

    public BonsaiPotBlockStateModel(Block expectedBlock, BlockStateModel delegate) {
        super(delegate);
        this.expectedBlock = expectedBlock;
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        if (state == null || state.getBlock() != expectedBlock) {
            return new GeometryKey(state == null ? expectedBlock : state.getBlock(), 0, false, null);
        }

        return new GeometryKey(
                delegate.createGeometryKey(level, pos, state, random),
                state.getValue(BonsaiBlock.ROTATION),
                BonsaiRenderRouting.usesBer(pos),
                level.getModelData(pos).get(BonsaiBlockEntity.RENDER_DATA)
        );
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

        int rotation = state.getValue(BonsaiBlock.ROTATION);
        parts.addAll(rotatedParts.computeIfAbsent(
                rotation,
                ignored -> bakeRotatedParts(level, pos, state, random, rotation)
        ));

        if (BonsaiRenderRouting.usesBer(pos)) {
            return;
        }

        BonsaiBlockEntity.RenderData renderData =
                level.getModelData(pos).get(BonsaiBlockEntity.RENDER_DATA);
        if (renderData == null || !renderData.planted()) {
            return;
        }

        BlockStateModel treeModel = renderData.dead()
                ? BlockModelRegistry.BONSAI_DEAD_SHAPES.get(renderData.shape())
                : BlockModelRegistry.BONSAI_SHAPES.get(renderData.shape());
        if (treeModel == null) {
            return;
        }

        BonsaiPartCacheKey key = BonsaiPartCacheKey.forState(
                renderData.shape(),
                true,
                renderData.dead(),
                renderData.trunkBlockId(),
                renderData.leavesBlockId()
        );
        TreeGeometryKey treeKey = new TreeGeometryKey(key, rotation);
        parts.addAll(rotatedTreeParts.computeIfAbsent(
                treeKey,
                ignored -> BonsaiBlockEntityRenderer.rotateParts(
                        BonsaiBlockEntityRenderer.getCachedParts(
                                List.of(treeModel), level, pos, state, key
                        ).parts(),
                        rotation
                )
        ));
    }

    private List<BlockStateModelPart> bakeRotatedParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            int rotation
    ) {
        List<BlockStateModelPart> originals = new ArrayList<>();
        delegate.collectParts(level, pos, state, random, originals);
        return BonsaiBlockEntityRenderer.rotateParts(originals, rotation);
    }

    private record GeometryKey(
            @Nullable Object delegateKey,
            int rotation,
            boolean usesBer,
            BonsaiBlockEntity.@Nullable RenderData renderData
    ) {
    }

    private record TreeGeometryKey(BonsaiPartCacheKey key, int rotation) {
    }

}
