package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.bonsai.BonsaiTreeGeometryCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;
import java.util.SortedSet;

/** Adds the dynamic bonsai tree to the vanilla block-breaking crack pass. */
public final class BonsaiBreakingOverlay {
    private BonsaiBreakingOverlay() {
    }

    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        RenderBuffers renderBuffers = minecraft.renderBuffers();
        var crumblingBuffers = renderBuffers.crumblingBufferSource();
        for (SortedSet<BlockDestructionProgress> progressSet : event.getLevelRenderer().destructionProgress.values()) {
            if (progressSet == null || progressSet.isEmpty()) {
                continue;
            }

            BlockDestructionProgress progress = progressSet.last();
            int stage = progress.getProgress();
            if (stage < 0 || stage >= ModelBakery.DESTROY_TYPES.size()) {
                continue;
            }

            BlockPos pos = progress.getPos();
            BlockState state = minecraft.level.getBlockState(pos);
            if (!(state.getBlock() instanceof BonsaiBlock)) {
                continue;
            }

            ModelData modelData = minecraft.level.getModelData(pos);
            BonsaiBlockEntity.RenderData renderData = modelData.get(BonsaiBlockEntity.RENDER_DATA);
            if (renderData == null || !renderData.planted()) {
                continue;
            }

            BakedModel treeModel = renderData.dead()
                    ? BlockModelRegistry.BONSAI_DEAD_SHAPES.get(renderData.shape())
                    : BlockModelRegistry.BONSAI_SHAPES.get(renderData.shape());
            if (treeModel == null) {
                continue;
            }

            List<BakedQuad> treeQuads = BonsaiTreeGeometryCache.rotateQuads(
                    BonsaiTreeGeometryCache.getTreeQuads(
                            treeModel, state, modelData, renderData, null),
                    state.getValue(BonsaiBlock.ROTATION));
            if (treeQuads.isEmpty()) {
                continue;
            }

            PoseStack poseStack = event.getPoseStack();
            var camera = event.getCamera().getPosition();
            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - camera.x,
                    pos.getY() - camera.y,
                    pos.getZ() - camera.z
            );

            RenderType destroyType = ModelBakery.DESTROY_TYPES.get(stage);
            VertexConsumer target = crumblingBuffers.getBuffer(destroyType);
            PoseStack.Pose pose = poseStack.last();
            VertexConsumer decal = new SheetedDecalTextureGenerator(target, pose, 1.0F);
            int light = LevelRenderer.getLightColor(minecraft.level, state, pos);
            for (BakedQuad quad : treeQuads) {
                decal.putBulkData(
                        pose,
                        quad,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        light,
                        OverlayTexture.NO_OVERLAY
                );
            }
            poseStack.popPose();
        }
    }
}