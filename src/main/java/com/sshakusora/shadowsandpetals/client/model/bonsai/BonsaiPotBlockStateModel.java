package com.sshakusora.shadowsandpetals.client.model.bonsai;

import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.client.renderer.BonsaiPartCacheKey;
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
 * This wrapper applies the same model-space rotation used by the tree geometry
 * to the baked pot quads, then caches the resulting parts for each segment.  The
 * tree is included from the immutable block-entity ModelData snapshot.</p>
 *
 * <p>For the first cross-section policy, the complete tree mesh remains in
 * the section that owns the bonsai block. It is not duplicated into
 * neighboring sections, so adjacent sections cannot z-fight or double-submit
 * the tree. A tree that extends beyond its owner section may therefore
 * disappear at a section culling boundary; a clipped multi-section mesh can
 * be added later without changing the ModelData contract.</p>
 */
@SuppressWarnings("ConstantConditions")
public final class BonsaiPotBlockStateModel extends DelegateBlockStateModel
        implements DynamicBlockStateModel {
    private final Block expectedBlock;
    private final int breakingOverlayRotation;
    private final Map<Integer, List<BlockStateModelPart>> rotatedParts = new ConcurrentHashMap<>();
    private final Map<TreeGeometryKey, List<BlockStateModelPart>> rotatedTreeParts =
            new ConcurrentHashMap<>();

    public BonsaiPotBlockStateModel(Block expectedBlock, BlockStateModel delegate) {
        this(expectedBlock, null, delegate);
    }

    public BonsaiPotBlockStateModel(
            Block expectedBlock,
            @Nullable BlockState bakedState,
            BlockStateModel delegate
    ) {
        this(expectedBlock, rotationFor(expectedBlock, bakedState), delegate);
    }

    BonsaiPotBlockStateModel(
            Block expectedBlock,
            int breakingOverlayRotation,
            BlockStateModel delegate
    ) {
        super(delegate);
        this.expectedBlock = expectedBlock;
        this.breakingOverlayRotation = breakingOverlayRotation;
    }

    private static int rotationFor(Block expectedBlock, @Nullable BlockState bakedState) {
        return bakedState != null
                && bakedState.getBlock() == expectedBlock
                && bakedState.hasProperty(BonsaiBlock.ROTATION)
                ? bakedState.getValue(BonsaiBlock.ROTATION)
                : 0;
    }

    @Override
    @Deprecated
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        List<BlockStateModelPart> originals = new ArrayList<>();
        delegate.collectParts(random, originals);
        parts.addAll(BonsaiTreeGeometryCache.rotateParts(originals, breakingOverlayRotation));
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        if (state == null || state.getBlock() != expectedBlock) {
            return new GeometryKey(state == null ? expectedBlock : state.getBlock(), 0, null);
        }

        return new GeometryKey(
                delegate.createGeometryKey(level, pos, state, random),
                state.getValue(BonsaiBlock.ROTATION),
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
            collectParts(random, parts);
            return;
        }

        int rotation = state.getValue(BonsaiBlock.ROTATION);
        parts.addAll(rotatedParts.computeIfAbsent(
                rotation,
                ignored -> bakeRotatedParts(level, pos, state, random, rotation)
        ));

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
        // First cross-section policy: keep the complete tree in the owning
        // section. Never duplicate it into neighboring section buffers.
        parts.addAll(rotatedTreeParts.computeIfAbsent(
                treeKey,
                ignored -> BonsaiTreeGeometryCache.rotateParts(
                        BonsaiTreeGeometryCache.getParts(
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
        return BonsaiTreeGeometryCache.rotateParts(originals, rotation);
    }

    private record GeometryKey(
            @Nullable Object delegateKey,
            int rotation,
            BonsaiBlockEntity.@Nullable RenderData renderData
    ) {
    }

    private record TreeGeometryKey(BonsaiPartCacheKey key, int rotation) {
    }

}
