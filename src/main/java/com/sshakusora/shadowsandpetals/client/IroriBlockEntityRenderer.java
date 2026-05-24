package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.blockentity.IroriBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Deprecated
public class IroriBlockEntityRenderer implements BlockEntityRenderer<IroriBlockEntity, IroriBlockEntityRenderer.State> {
    private static final int MIN_FIREWOOD_COUNT = 4;
    private static final int FIREWOOD_COUNT_VARIANCE = 4;
    // 控制整堆柴火在围炉中的基础高度。
    private static final double FIREWOOD_BASE_Y = 10.0D / 16.0D;
    // 控制柴火落地端距离围炉中心的半径；越大越像外圈落地、向中心搭靠。
    private static final double FIREWOOD_BASE_RADIUS = 0.20D;
    // 控制向中心额外收拢的幅度，让构图更集中。
    private static final double FIREWOOD_INWARD_PULL = 0.03D;
    // 控制不同柴火之间的轻微高低差，避免完全重叠。
    private static final double FIREWOOD_Y_JITTER = 0.02D;
    // 控制柴火沿圆周分布时的额外角度扰动，减轻过于均匀的环形排列。
    private static final double FIREWOOD_ANGLE_JITTER = 0.65D;
    // 控制柴火沿切线方向的偏移，打散过于笔直的朝心排布。
    private static final double FIREWOOD_TANGENT_JITTER = 0.045D;
    // 控制柴火顶端支撑点沿切线方向的扰动，避免所有柴火尖端汇聚到同一圈上。
    private static final double FIREWOOD_APEX_TANGENT_JITTER = 0.085D;
    // 控制柴火顶端支撑点沿朝心方向的扰动，打散过于整齐的顶点深度。
    private static final double FIREWOOD_APEX_DEPTH_JITTER = 0.25D;
    // 控制单根柴火模型在本地坐标中的横向居中偏移基准。
    private static final double FIREWOOD_WIDTH = 3.0D / 16.0D;
    // 控制柴火向中心寻找支撑点时的最小收拢量。
    private static final double FIREWOOD_SUPPORT_INSET_MIN = 0.04D;
    // 控制柴火向中心寻找支撑点时的最大收拢量，避免过度塌向中心。
    private static final double FIREWOOD_SUPPORT_INSET_MAX = 0.12D;

    public IroriBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            IroriBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.firewood.clear();
        state.renderFirewood = blockEntity.isMaster();
        if (!state.renderFirewood) {
            return;
        }

        IroriBlockEntity.IroriRegion region = blockEntity.getRegion();
        state.offsetX = region.centerX() - blockEntity.getBlockPos().getX();
        state.offsetZ = region.centerZ() - blockEntity.getBlockPos().getZ();
        long seed = blockEntity.getBlockPos().asLong() ^ ((long) region.width() << 32) ^ region.depth();
        Random random = new Random(seed);
        int count = MIN_FIREWOOD_COUNT + random.nextInt(FIREWOOD_COUNT_VARIANCE);
        double radiusScale = 1.0D + Math.min(region.width(), region.depth()) * 0.06D;
        double angleStep = Math.PI * 2.0D / count;
        double baseAngle = random.nextDouble() * Math.PI * 2.0D;
        for (int index = 0; index < count; index++) {
            int length = 7 + random.nextInt(3);
            BlockModelRegistry.IroriFirewoodModel firewoodModel = BlockModelRegistry.IroriFirewoodModel.byLitAndLength(blockEntity.isLit(), length);
            BlockStateModel model = BlockModelRegistry.getIroriFirewoodModel(firewoodModel);
            if (model == null) {
                continue;
            }

            FirewoodRender firewood = new FirewoodRender(firewoodModel.length());
            model.collectParts(RandomSource.create(seed + index * 31L), firewood.modelParts);
            firewood.hasTranslucency = model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT);
            double angle = baseAngle + angleStep * index + (random.nextDouble() - 0.5D) * FIREWOOD_ANGLE_JITTER;
            double radius = FIREWOOD_BASE_RADIUS * radiusScale + (random.nextDouble() - 0.5D) * 0.045D;
            double tangentJitter = (random.nextDouble() - 0.5D) * FIREWOOD_TANGENT_JITTER;
            // 控制单根柴火绕 Y 轴的水平旋转角度；这里改为基本朝向围炉中心。
            firewood.yaw = (float) Math.toDegrees(Math.atan2(-Math.sin(angle), -Math.cos(angle))) + (float) ((random.nextDouble() - 0.5D) * 18.0D);
            // 控制单根柴火前后倾斜角度；不同长度会落在不同角度范围。
            firewood.pitch = randomPitch(length, random);
            // 控制单根柴火左右侧倾角度；这里减小幅度，避免从三角构图里倒出去。
            firewood.roll = (float) ((random.nextDouble() - 0.5D) * 6.0D);
            // 控制单根柴火落地端在围炉中的 X 方向位置。
            firewood.offsetX = Math.cos(angle) * radius - Math.cos(angle) * FIREWOOD_INWARD_PULL - Math.sin(angle) * tangentJitter;
            // 控制单根柴火落地端在围炉中的 Z 方向位置。
            firewood.offsetZ = Math.sin(angle) * radius - Math.sin(angle) * FIREWOOD_INWARD_PULL + Math.cos(angle) * tangentJitter;
            // 控制单根柴火落地端的高度微扰，让几根柴火不会完全压在同一平面上。
            firewood.offsetY = layeredHeightOffset(index, random);
            // 控制柴火向中心收拢寻找支撑点的距离；柴火更少、长度更长、倾角更大时会收得更深。
            firewood.supportInset = computeSupportInset(count, firewood.pitch, firewood.length);
            // 控制顶端支撑点的切线方向错位，避免尖顶全部重合。
            firewood.apexOffsetX = -Math.sin(angle) * ((random.nextDouble() - 0.5D) * FIREWOOD_APEX_TANGENT_JITTER);
            firewood.apexOffsetZ = Math.cos(angle) * ((random.nextDouble() - 0.5D) * FIREWOOD_APEX_TANGENT_JITTER);
            // 控制顶端支撑点沿朝心方向的前后错位，进一步打散顶点位置。
            double apexDepthJitter = (random.nextDouble() - 0.5D) * FIREWOOD_APEX_DEPTH_JITTER;
            firewood.apexOffsetX += -Math.cos(angle) * apexDepthJitter;
            firewood.apexOffsetZ += -Math.sin(angle) * apexDepthJitter;
            state.firewood.add(firewood);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.renderFirewood) {
            return;
        }

        for (FirewoodRender firewood : state.firewood) {
            poseStack.pushPose();
            // 控制整根柴火落地端的位置：这里的点会贴在围炉上表面，作为整根柴火的支点。
            poseStack.translate(state.offsetX + firewood.apexOffsetX, FIREWOOD_BASE_Y + firewood.offsetY, state.offsetZ + firewood.apexOffsetZ);
            // 控制水平朝向旋转，使柴火整体朝向围炉中心。
            poseStack.mulPose(Axis.YP.rotationDegrees(firewood.yaw));
            // 控制前后倾斜；因为支点在柴火短边一端，所以会形成“一端落地、一端抬起”的效果。
            poseStack.translate(0, 0, firewood.length / 16.0D);
            poseStack.translate(
                    firewood.offsetX,
                    0,
                    firewood.offsetZ - firewood.supportInset
            );
            poseStack.mulPose(Axis.XP.rotationDegrees(firewood.pitch));
            poseStack.translate(0, 0, -firewood.length / 16.0D);
            // 控制左右侧倾。
            poseStack.mulPose(Axis.ZP.rotationDegrees(firewood.roll));
            // 控制模型局部原点：X 保持底面居中，Z 改成从短边起算，让旋转支点落在柴火的一端。
            poseStack.translate(-FIREWOOD_WIDTH / 2.0D, 0.0D, 0.0D);
            submitNodeCollector.submitMultiLayerBlockModel(
                    poseStack,
                    firewood.modelParts,
                    firewood.hasTranslucency,
                    BlockModelRenderState.EMPTY_TINTS,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );
            poseStack.popPose();
        }
    }

    private static float randomPitch(int length, Random random) {
        // 这里控制不同长度柴火允许的倾角范围；越长的柴火范围越大，更容易形成上翘支撑。
        float min = switch (length) {
            case 7 -> 26.0F;
            case 8 -> 32.0F;
            default -> 38.0F;
        };
        float max = switch (length) {
            case 7 -> 34.0F;
            case 8 -> 40.0F;
            default -> 46.0F;
        };
        return min + random.nextFloat() * (max - min);
    }

    private static double computeSupportInset(int count, float pitch, int length) {
        double sparseFactor = 1.0D - Math.min(1.0D, (count - MIN_FIREWOOD_COUNT) / (double) FIREWOOD_COUNT_VARIANCE);
        double pitchFactor = Math.max(0.0D, (pitch - 26.0D) / 20.0D);
        double lengthFactor = Math.max(0.0D, (length - 7) / 2.0D);

        double inset = FIREWOOD_SUPPORT_INSET_MIN
                + sparseFactor * 0.04D
                + pitchFactor * 0.025D
                + lengthFactor * 0.015D;
        return Math.min(FIREWOOD_SUPPORT_INSET_MAX, inset);
    }

    private static double layeredHeightOffset(int index, Random random) {
        double tierBase = switch (index % 3) {
            case 0 -> -0.004D;
            case 1 -> 0.006D;
            default -> 0.015D;
        };
        return tierBase + (random.nextDouble() - 0.5D) * FIREWOOD_Y_JITTER;
    }

    public static class State extends BlockEntityRenderState {
        public final List<FirewoodRender> firewood = new ArrayList<>();
        public boolean renderFirewood;
        public double offsetX = 0.5D;
        public double offsetZ = 0.5D;
    }

    public static class FirewoodRender {
        public final List<BlockStateModelPart> modelParts = new ArrayList<>();
        public final int length;
        public boolean hasTranslucency;
        public float yaw;
        public float pitch;
        public float roll;
        public double offsetX;
        public double offsetY;
        public double offsetZ;
        public double supportInset;
        public double apexOffsetX;
        public double apexOffsetZ;

        private FirewoodRender(int length) {
            this.length = length;
        }
    }
}
