package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiModelTransform;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.bonsai.BonsaiTreeGeometryCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Submits tree crack geometry only while a bonsai is being broken.
 *
 * <p>The ordinary block-breaking path receives an empty level and therefore
 * can only collect the pot from the dynamic block model.  Keeping this small
 * overlay separate from the block entity renderer preserves tree crack
 * visuals without putting every bonsai on the per-frame BER path.</p>
 */
public final class BonsaiBreakingOverlay {
    private static final ContextKey<List<BreakingTree>> BREAKING_TREES =
            new ContextKey<>(Identifier.fromNamespaceAndPath(
                    "shadowsandpetals",
                    "bonsai_breaking_trees"
            ));

    private BonsaiBreakingOverlay() {
    }

    public static void extract(ExtractLevelRenderStateEvent event) {
        List<BreakingTree> trees = null;
        BlockAndTintGetter level = event.getLevel();
        for (BlockBreakingRenderState breaking
                : event.getRenderState().blockBreakingRenderStates) {
            BlockState state = breaking.blockState();
            if (!(state.getBlock() instanceof BonsaiBlock)) {
                continue;
            }

            BlockPos pos = breaking.blockPos();
            BonsaiBlockEntity.RenderData data = level.getModelData(pos)
                    .get(BonsaiBlockEntity.RENDER_DATA);
            if (data == null || !data.planted()) {
                continue;
            }

            BlockStateModel treeModel = data.dead()
                    ? BlockModelRegistry.BONSAI_DEAD_SHAPES.get(data.shape())
                    : BlockModelRegistry.BONSAI_SHAPES.get(data.shape());
            if (treeModel == null) {
                continue;
            }

            BonsaiPartCacheKey key = BonsaiPartCacheKey.forState(
                    data.shape(),
                    true,
                    data.dead(),
                    data.trunkBlockId(),
                    data.leavesBlockId()
            );
            List<BlockStateModelPart> parts = BonsaiTreeGeometryCache.rotateParts(
                    BonsaiTreeGeometryCache.getParts(
                            List.of(treeModel),
                            level,
                            pos,
                            state,
                            key
                    ).parts(),
                    state.getValue(BonsaiBlock.ROTATION)
            );
            if (trees == null) {
                trees = new ArrayList<>();
            }
            trees.add(new BreakingTree(
                    pos,
                    state.getSeed(pos),
                    breaking.progress(),
                    parts
            ));
        }
        event.getRenderState().setRenderData(
                BREAKING_TREES,
                trees == null ? null : List.copyOf(trees)
        );
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        List<BreakingTree> trees = event.getLevelRenderState()
                .getRenderData(BREAKING_TREES);
        if (trees == null || trees.isEmpty()) {
            return;
        }

        var camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (BreakingTree tree : trees) {
            BlockPos pos = tree.pos();
            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - camera.x(),
                    pos.getY() - camera.y(),
                    pos.getZ() - camera.z()
            );
            collector.submitBreakingBlockModel(
                    poseStack,
                    BonsaiTreeGeometryCache.fixedPartsModel(tree.parts()),
                    tree.seed(),
                    tree.progress()
            );
            poseStack.popPose();
        }
    }

    private record BreakingTree(
            BlockPos pos,
            long seed,
            int progress,
            List<BlockStateModelPart> parts
    ) {
    }
}
