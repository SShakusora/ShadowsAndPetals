package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.AbstractCurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainSide;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LongCurtainBlock;
import com.sshakusora.shadowsandpetals.blockentity.AbstractCurtainBlockEntity;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the two-block curtain's per-bone baked models during its short
 * transition window. Outside that window the normal block-state model is used
 * by the chunk renderer.
 */
public class CurtainBlockEntityRenderer implements BlockEntityRenderer<AbstractCurtainBlockEntity> {
    private static final float FALLBACK_END_POSE_SECONDS = 1.0F;

    private final BlockRenderDispatcher blockRenderer;
    private final Map<CurtainVariant, @Nullable AnimatedBlockModel> cachedModels = new HashMap<>();

    private record CurtainVariant(boolean upper, boolean left, DyeColor color) {
    }

    public CurtainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public AABB getRenderBoundingBox(AbstractCurtainBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(0.25D);
    }

    @Override
    public void render(AbstractCurtainBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(AbstractCurtainBlock.ANIMATING) || blockEntity.getLevel() == null) {
            return;
        }

        boolean upper = !(state.getBlock() instanceof LongCurtainBlock)
                || state.getValue(LongCurtainBlock.HALF) == DoubleBlockHalf.UPPER;
        boolean left = state.getValue(AbstractCurtainBlock.SIDE) == CurtainSide.LEFT;
        DyeColor color = dyeColorOf(state);
        BlockAnimationDefinition definition = definition(upper, left);
        String[] bones = upper ? BlockModelRegistry.CURTAIN_UPPER_BONES
                : BlockModelRegistry.CURTAIN_LOWER_BONES;
        boolean longCurtain = state.getBlock() instanceof LongCurtainBlock;
        StandaloneBlockModelSet<BlockModelRegistry.CurtainBoneKey> modelSet = modelSet(upper, left, longCurtain);
        AnimatedBlockModel model = resolveModel(upper, left, definition.rig(), bones, modelSet, color);
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
        model.render(pose, poseStack, blockRenderer, state, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private AnimatedBlockModel resolveModel(
            boolean upper,
            boolean left,
            AnimationResourceRef.Rig rig,
            String[] bones,
            StandaloneBlockModelSet<BlockModelRegistry.CurtainBoneKey> modelSet,
            DyeColor color
    ) {
        CurtainVariant variant = new CurtainVariant(upper, left, color);
        if (cachedModels.containsKey(variant)) {
            return cachedModels.get(variant);
        }

        List<AnimatedBlockModel.Binding> bindings = new ArrayList<>(bones.length);
        for (String bone : bones) {
            BakedModel model = modelSet.get(new BlockModelRegistry.CurtainBoneKey(color, bone));
            if (model != null) {
                bindings.add(new AnimatedBlockModel.Binding(
                        rig, bone, model, RenderType.cutout(), false));
            }
        }
        AnimatedBlockModel result = bindings.isEmpty() ? null : new AnimatedBlockModel(rig, bindings);
        cachedModels.put(variant, result);
        return result;
    }

    private static BlockAnimationDefinition definition(boolean upper, boolean left) {
        if (upper) {
            return left ? SAPAnimations.CURTAIN_UPPER_LEFT : SAPAnimations.CURTAIN_UPPER_RIGHT;
        }
        return left ? SAPAnimations.CURTAIN_LOWER_LEFT : SAPAnimations.CURTAIN_LOWER_RIGHT;
    }

    private static StandaloneBlockModelSet<BlockModelRegistry.CurtainBoneKey> modelSet(
            boolean upper, boolean left, boolean longCurtain) {
        if (upper) {
            if (longCurtain) {
                return left ? BlockModelRegistry.LONG_CURTAIN_UPPER_LEFT
                        : BlockModelRegistry.LONG_CURTAIN_UPPER_RIGHT;
            }
            return left ? BlockModelRegistry.CURTAIN_UPPER_LEFT : BlockModelRegistry.CURTAIN_UPPER_RIGHT;
        }
        if (longCurtain) {
            return left ? BlockModelRegistry.LONG_CURTAIN_LOWER_LEFT
                    : BlockModelRegistry.LONG_CURTAIN_LOWER_RIGHT;
        }
        return left ? BlockModelRegistry.CURTAIN_LOWER_LEFT : BlockModelRegistry.CURTAIN_LOWER_RIGHT;
    }

    private static DyeColor dyeColorOf(BlockState blockState) {
        Block block = blockState.getBlock();
        for (DyeColor color : DyeColor.values()) {
            if (block == BlockRegistry.CURTAINS.get(color).get()
                    || block == BlockRegistry.LONG_CURTAINS.get(color).get()) {
                return color;
            }
        }
        return DyeColor.WHITE;
    }
}
