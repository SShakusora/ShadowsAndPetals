package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.AbstractCurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainSide;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LargeCurtainBlock;
import com.sshakusora.shadowsandpetals.blockentity.LargeCurtainBlockEntity;
import com.sshakusora.shadowsandpetals.client.animation.*;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.registry.StandaloneBlockModelSet;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the four-block curtain as one animated rig from its anchor block.
 * The other three quadrants are invisible only during the transition; their
 * normal static models resume when the animation flag is cleared.
 */
public class LargeCurtainBlockEntityRenderer implements BlockEntityRenderer<LargeCurtainBlockEntity> {
    private static final float FALLBACK_END_POSE_SECONDS = 1.0F;

    private final BlockRenderDispatcher blockRenderer;
    private final Map<CurtainVariant, @Nullable AnimatedBlockModel> cachedModels = new HashMap<>();

    private record CurtainVariant(boolean left, DyeColor color) {
    }

    public LargeCurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public AABB getRenderBoundingBox(LargeCurtainBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.5D);
    }

    @Override
    public void render(LargeCurtainBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(AbstractCurtainBlock.ANIMATING)
                || !state.getValue(LargeCurtainBlock.ANCHOR)
                || blockEntity.getLevel() == null) {
            return;
        }

        boolean left = state.getValue(AbstractCurtainBlock.SIDE) == CurtainSide.LEFT;
        DyeColor color = dyeColorOf(state);
        BlockAnimationDefinition definition = left
                ? SAPAnimations.LARGE_CURTAIN_LEFT : SAPAnimations.LARGE_CURTAIN_RIGHT;
        AnimatedBlockModel model = resolveModel(left, definition.rig(), color);
        if (model == null) {
            return;
        }

        boolean stateOpen = state.getValue(AbstractCurtainBlock.OPEN);
        boolean beSynced = blockEntity.isOpen() == stateOpen;
        float seconds = blockEntity.transitionTimeSeconds(
                blockEntity.getLevel().getGameTime(), partialTick);
        if (!beSynced || seconds < 0.0F || seconds > FALLBACK_END_POSE_SECONDS) {
            seconds = FALLBACK_END_POSE_SECONDS;
        }
        RigPose pose = AnimationControllerEvaluator.sample(
                definition.controller().id(), stateOpen ? "open" : "closed", seconds);

        poseStack.pushPose();
        Direction facing = state.getValue(AbstractCurtainBlock.FACING);
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180.0F));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        if (left) {
            // The authored left rig is mirrored around the adjacent cell.
            poseStack.translate(1.0D, 0.0D, 0.0D);
        }
        model.render(pose, poseStack, blockRenderer, state, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private AnimatedBlockModel resolveModel(
            boolean left,
            AnimationResourceRef.Rig rig,
            DyeColor color
    ) {
        CurtainVariant variant = new CurtainVariant(left, color);
        if (cachedModels.containsKey(variant)) {
            return cachedModels.get(variant);
        }
        StandaloneBlockModelSet<BlockModelRegistry.CurtainBoneKey> modelSet = left
                ? BlockModelRegistry.LARGE_CURTAIN_LEFT : BlockModelRegistry.LARGE_CURTAIN_RIGHT;
        List<AnimatedBlockModel.Binding> bindings = new ArrayList<>(BlockModelRegistry.LARGE_CURTAIN_BONES.length);
        for (String bone : BlockModelRegistry.LARGE_CURTAIN_BONES) {
            BakedModel model = modelSet.get(new BlockModelRegistry.CurtainBoneKey(color, bone));
            if (model != null) {
                bindings.add(new AnimatedBlockModel.Binding(rig, bone, model, RenderType.cutout(), false));
            }
        }
        AnimatedBlockModel result = bindings.isEmpty() ? null : new AnimatedBlockModel(rig, bindings);
        cachedModels.put(variant, result);
        return result;
    }

    private static DyeColor dyeColorOf(BlockState blockState) {
        Block block = blockState.getBlock();
        for (DyeColor color : DyeColor.values()) {
            if (block == BlockRegistry.LARGE_CURTAINS.get(color).get()) {
                return color;
            }
        }
        return DyeColor.WHITE;
    }
}
