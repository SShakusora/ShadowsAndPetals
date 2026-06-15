package com.sshakusora.shadowsandpetals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.item.HammerItem;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

/**
 * Client-side animations for the Hammer + Chisel dual-wield action.
 */
// TODO: Need more configuration
public class HammerClientExtensions implements IClientItemExtensions {
    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (!(stack.getItem() instanceof HammerItem)) {
            return null;
        }
        if (hand != InteractionHand.MAIN_HAND || !entity.isUsingItem() || entity.getUsedItemHand() != hand) {
            return null;
        }
        if (!entity.getOffhandItem().is(ItemRegistry.CHISEL.get())) {
            return null;
        }
        return HammerArmPoseEnumExtensions.getHammerAndChiselPose();
    }

    @Override
    public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack itemInHand,
            float partialTick,
            float equipProcess,
            float swingProcess) {

        if (!(itemInHand.getItem() instanceof HammerItem)) {
            return false;
        }
        if (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (!player.getOffhandItem().is(ItemRegistry.CHISEL.get())) {
            return false;
        }

        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;

        poseStack.translate(invert * 0.56F, -0.52F, -0.72F);    //Translate to Vanilla arm position
        poseStack.translate(invert * -0.18F, 0.08F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * 22.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -6.0F));

        float useTicks = player.getTicksUsingItem();
        float duration = 30.0F;
        float progress = Mth.clamp(useTicks / duration, 0.0F, 1.0F);

        float strikesPerSecond = 3.0F;
        float totalCycles = progress * duration * strikesPerSecond / 20.0F;
        float strikePhase = totalCycles - Mth.floor(totalCycles);

        float strikeIntensity;
        if (strikePhase < 0.15F) {
            strikeIntensity = strikePhase / 0.15F;
        } else if (strikePhase < 0.35F) {
            strikeIntensity = 1.0F - (strikePhase - 0.15F) / 0.2F;
        } else {
            strikeIntensity = 0.0F;
        }

        poseStack.translate(0.0F, -strikeIntensity * 0.12F, strikeIntensity * 0.08F);
        poseStack.mulPose(Axis.XP.rotationDegrees(strikeIntensity * 35.0F));

        if (strikeIntensity > 0.0F && progress > 0.05F) {
            float shake = Mth.sin(useTicks * 25.0F) * 0.006F * strikeIntensity;
            poseStack.translate(0.0F, shake, 0.0F);
        }

        poseStack.translate(0.0F, 0.0F, progress * 0.02F);
        poseStack.scale(1.0F, 1.0F, 1.0F + progress * 0.1F);
        poseStack.mulPose(Axis.YN.rotationDegrees(invert * 40.0F));

        return true;
    }

    static void applyThirdPersonHammerPose(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        boolean rightArm = arm == HumanoidArm.RIGHT;
        var hammerArm = rightArm ? model.rightArm : model.leftArm;
        var chiselArm = rightArm ? model.leftArm : model.rightArm;
        int invert = rightArm ? 1 : -1;

        float cycle = (state.ageInTicks % 6.0F) / 6.0F;
        float strike = cycle < 0.25F ? cycle / 0.25F : Math.max(0.0F, 1.0F - (cycle - 0.25F) / 0.3F);

        hammerArm.xRot = -1.85F + strike * 0.9F;
        hammerArm.yRot = invert * -0.25F;
        hammerArm.zRot = invert * 0.18F;

        chiselArm.xRot = -1.1F;
        chiselArm.yRot = invert * 0.35F;
        chiselArm.zRot = invert * -0.2F;
    }
}
