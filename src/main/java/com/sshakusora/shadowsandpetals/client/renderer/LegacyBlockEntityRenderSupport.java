package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * Small rendering bridge for the 1.21.1 renderer API.
 *
 * <p>The 26.x branch submits block-model parts through the render-state
 * pipeline.  1.21.1 still exposes the same baked geometry through
 * {@link BlockRenderDispatcher} and {@link BakedModel}, so block-entity
 * renderers can keep their own animation math while sharing this bridge for
 * model lookup and submission.</p>
 */
final class LegacyBlockEntityRenderSupport {
    private LegacyBlockEntityRenderSupport() {
    }

    static void renderBlock(
            BlockRenderDispatcher dispatcher,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        dispatcher.renderSingleBlock(state, poseStack, buffers, light, overlay);
    }

    static void renderStandalone(
            BlockRenderDispatcher dispatcher,
            ResourceLocation modelId,
            @Nullable BlockState tintState,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        BakedModel model = Minecraft.getInstance().getModelManager()
                .getModel(ModelResourceLocation.standalone(modelId));
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
        dispatcher.getModelRenderer().renderModel(
                poseStack.last(), consumer, tintState, model,
                1.0F, 1.0F, 1.0F, light, overlay,
                ModelData.EMPTY, RenderType.cutout()
        );
    }

    static void renderItem(
            ItemRenderer renderer,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffers,
            @Nullable Level level,
            int light,
            int overlay
    ) {
        if (stack.isEmpty()) {
            return;
        }
        renderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                light,
                overlay,
                poseStack,
                buffers,
                level,
                0
        );
    }

    static int blockLight(@Nullable Level level, BlockPos pos) {
        if (level == null) {
            return 0xF000F0;
        }
        return net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos);
    }
}
