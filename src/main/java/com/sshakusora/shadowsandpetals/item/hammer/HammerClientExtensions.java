package com.sshakusora.shadowsandpetals.item.hammer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import com.sshakusora.shadowsandpetals.util.MathUtils;
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

public class HammerClientExtensions implements IClientItemExtensions {
    private static final float STRIKE_PERIOD_TICKS = HammerItem.HAMMER_STRIKE_PERIOD_TICKS;
    private static final float HAMMER_STRIKE_END_PHASE = HammerItem.HAMMER_IMPACT_PHASE;
    private static final float HAMMER_IMPACT_PHASE = HammerItem.HAMMER_IMPACT_PHASE;
    private static final float THIRD_PERSON_WINDUP_END_PHASE = 0.84F;
    private static final float THIRD_PERSON_STRIKE_END_PHASE = 0.94F;
    private static final float THIRD_PERSON_IMPACT_PHASE = THIRD_PERSON_STRIKE_END_PHASE;

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

        if (!isHammerAndChiselUse(player)) {
            return false;
        }
        if (itemInHand.getItem() instanceof HammerItem) {
            applyFirstPersonHammerPose(poseStack, player, arm, partialTick);
            return true;
        }
        if (itemInHand.is(ItemRegistry.CHISEL.get())) {
            applyFirstPersonChiselPose(poseStack, player, arm, partialTick);
            return true;
        }
        return false;
    }

    private static boolean isHammerAndChiselUse(LocalPlayer player) {
        return player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getMainHandItem().getItem() instanceof HammerItem
                && player.getOffhandItem().is(ItemRegistry.CHISEL.get());
    }

    private static void applyFirstPersonChiselPose(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, float partialTick) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        float useTicks = player.getTicksUsingItem() + partialTick;
        float duration = HammerItem.getEffectiveUseDuration(player.getMainHandItem());
        float progress = useProgress(useTicks, duration);
        float impact = impactPulse(useTicks);
        float settle = easeOut(progress);

        poseStack.translate(invert * 0.00F, -0.64F, -1.06F);
        poseStack.translate(invert * -0.04F * settle, 0.02F * settle, -0.08F * settle);
        poseStack.translate(invert * 0.012F * impact, -0.018F * impact, -0.035F * impact);

        if (impact > 0.0F) {
            float tremor = Mth.sin(useTicks * 34.0F) * 0.006F * impact;
            poseStack.translate(invert * tremor, tremor * 0.35F, 0.0F);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(invert * -24.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.mulPose(Axis.ZN.rotationDegrees(invert * -5.0F));
    }

    private static void applyFirstPersonHammerPose(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, float partialTick) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        float useTicks = player.getTicksUsingItem() + partialTick;
        float duration = HammerItem.getEffectiveUseDuration(player.getMainHandItem());
        float progress = useProgress(useTicks, duration);
        float windup = hammerWindup(useTicks);
        float impact = impactPulse(useTicks);
        float settle = easeOut(progress);

        poseStack.translate(invert * 0.27F, -0.22F, -0.52F);
        poseStack.translate(invert * -0.04F * settle, 0.0F, 0.02F * settle);

        if (impact > 0.0F) {
            float tremor = Mth.sin(useTicks * 40.0F) * 0.004F * impact;
            poseStack.translate(invert * tremor, tremor, 0.0F);
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(-38.0F - 46.0F * windup + 16.0F * impact));
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * 8.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(invert * 16.0F));
    }

    private static float useProgress(float useTicks, float duration) {
        return Mth.clamp(useTicks / Math.max(1.0F, duration), 0.0F, 1.0F);
    }

    private static float hammerWindup(float useTicks) {
        return hammerCycle(useTicks, HAMMER_STRIKE_END_PHASE, 1.0F, true);
    }

    private static float impactPulse(float useTicks) {
        return impactPulseAt(useTicks, HAMMER_IMPACT_PHASE);
    }

    private static float thirdPersonHammerWindup(float useTicks) {
        return hammerCycle(useTicks, THIRD_PERSON_WINDUP_END_PHASE, THIRD_PERSON_STRIKE_END_PHASE, false);
    }

    private static float thirdPersonImpactPulse(float useTicks) {
        return impactPulseAt(useTicks, THIRD_PERSON_IMPACT_PHASE);
    }

    private static float hammerCycle(float useTicks, float riseEndPhase, float fallEndPhase, boolean fastRise) {
        float phase = strikePhase(useTicks);
        if (phase < riseEndPhase) {
            float rise = phase / riseEndPhase;
            return fastRise ? MathUtils.easeOutCubic(rise) : rise;
        }
        if (phase < fallEndPhase) {
            float fall = (phase - riseEndPhase) / (fallEndPhase - riseEndPhase);
            return 1.0F - (fastRise ? fall : MathUtils.easeOutCubic(fall));
        }
        return 0.0F;
    }

    private static float impactPulseAt(float useTicks, float impactPhase) {
        float phase = strikePhase(useTicks);
        float pulse = 1.0F - Mth.abs(phase - impactPhase) / 0.07F;
        return easeOut(Mth.clamp(pulse, 0.0F, 1.0F));
    }

    private static float strikePhase(float useTicks) {
        float phase = (useTicks % STRIKE_PERIOD_TICKS) / STRIKE_PERIOD_TICKS;
        return phase < 0.0F ? phase + 1.0F : phase;
    }

    private static float easeOut(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - (1.0F - value) * (1.0F - value);
    }

    static void applyThirdPersonHammerPose(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        boolean rightArm = arm == HumanoidArm.RIGHT;
        var hammerArm = rightArm ? model.rightArm : model.leftArm;
        var chiselArm = rightArm ? model.leftArm : model.rightArm;
        int invert = rightArm ? 1 : -1;

        float windup = thirdPersonHammerWindup(state.ageInTicks);
        float impact = thirdPersonImpactPulse(state.ageInTicks);

        hammerArm.xRot = -1.10F - 0.70F * windup + 0.25F * impact;
        hammerArm.yRot = invert * (-0.28F - 0.18F * impact);
        hammerArm.zRot = invert * (0.22F + 0.42F * windup - 0.18F * impact);

        chiselArm.xRot = -1.30F + 0.06F * impact;
        chiselArm.yRot = invert * 0.38F;
        chiselArm.zRot = invert * -0.24F;
    }
}
