package com.sshakusora.shadowsandpetals.item.harrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

public final class HarrowClientExtensions implements IClientItemExtensions {
    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(
            LivingEntity entity,
            InteractionHand hand,
            ItemStack stack
    ) {
        if (!(stack.getItem() instanceof HarrowItem)
                || !entity.isUsingItem()
                || entity.getUsedItemHand() != hand) {
            return null;
        }
        return HarrowArmPoseEnumExtensions.getHarrowDiggingPose();
    }

    @Override
    public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack itemInHand,
            float partialTick,
            float equipProcess,
            float swingProcess
    ) {
        if (!(itemInHand.getItem() instanceof HarrowItem)
                || !player.isUsingItem()
                || player.getUsedItemHand() != handForArm(player, arm)) {
            return false;
        }

        applyFirstPersonHarrowPose(poseStack, player, arm, partialTick, equipProcess);
        return true;
    }

    private static InteractionHand handForArm(LocalPlayer player, HumanoidArm arm) {
        return player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    static void applyThirdPersonHarrowPose(
            HumanoidModel<?> model,
            HumanoidRenderState state,
            HumanoidArm arm
    ) {
        if (!state.isUsingItem) {
            return;
        }
        var modelArm = model.getArm(arm);
        modelArm.xRot = modelArm.xRot * 0.5F - (float) (Math.PI / 5.0);
        modelArm.yRot = 0.0F;
    }

    private static void applyFirstPersonHarrowPose(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            float partialTick,
            float equipProcess
    ) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(direction * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);

        float remainingTicks = player.getUseItemRemainingTicks() % 10;
        float cycleProgress = 1.0F - (remainingTicks - partialTick + 1.0F) / 10.0F;
        float swipeAngle = -15.0F + 75.0F * (float) Math.cos(cycleProgress * 2.0F * Math.PI);
        if (arm == HumanoidArm.LEFT) {
            poseStack.translate(0.1F, 0.83F, 0.35F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(swipeAngle));
            poseStack.translate(-0.3F, 0.22F, 0.35F);
        } else {
            poseStack.translate(-0.25F, 0.22F, 0.35F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(swipeAngle));
        }
    }
}
