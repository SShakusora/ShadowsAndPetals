package com.sshakusora.shadowsandpetals.item.hammer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.shadowsandpetals.client.animation.SAPAnimations;
import com.sshakusora.shadowsandpetals.client.animation.UseAnimationPlayer;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

public class HammerClientExtensions implements IClientItemExtensions {
    private static final float TICKS_PER_SECOND = 20.0F;

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
        if (!(itemInHand.getItem() instanceof HammerItem) && !itemInHand.is(ItemRegistry.CHISEL.get())) {
            return false;
        }

        float localTimeSeconds = (player.getTicksUsingItem() + partialTick) / TICKS_PER_SECOND;
        return UseAnimationPlayer.applyFirstPerson(
                SAPAnimations.HAMMER,
                poseStack,
                player,
                arm,
                player.getMainArm(),
                localTimeSeconds);
    }

    private static boolean isHammerAndChiselUse(LocalPlayer player) {
        return player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getMainHandItem().getItem() instanceof HammerItem
                && player.getOffhandItem().is(ItemRegistry.CHISEL.get());
    }

    static void applyThirdPersonHammerPose(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        UseAnimationPlayer.applyThirdPerson(
                SAPAnimations.HAMMER,
                model,
                state,
                arm,
                state.ticksUsingItem(arm) / TICKS_PER_SECOND);
    }
}
