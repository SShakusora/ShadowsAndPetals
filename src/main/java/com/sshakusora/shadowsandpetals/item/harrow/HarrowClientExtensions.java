package com.sshakusora.shadowsandpetals.item.harrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.shadowsandpetals.client.animation.SAPAnimations;
import com.sshakusora.shadowsandpetals.client.animation.UseAnimationPlaybackManager;
import com.sshakusora.shadowsandpetals.client.animation.UseAnimationPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class HarrowClientExtensions implements IClientItemExtensions {
    private static final float TICKS_PER_SECOND = 20.0F;

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(
            LivingEntity entity,
            InteractionHand hand,
            ItemStack stack
    ) {
        if (!(stack.getItem() instanceof HarrowItem)
                || entity.getUsedItemHand() != hand) {
            return null;
        }
        var playback = UseAnimationPlaybackManager.INSTANCE.observe(
                entity.getId(),
                SAPAnimations.HARROW,
                isHarrowDiggingUse(entity),
                usedArm(entity),
                absoluteTime(entity, 0.0F));
        return playback == null
                ? null
                : HarrowArmPoseEnumExtensions.getHarrowDiggingPose();
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
                || player.getUsedItemHand() != handForArm(player, arm)) {
            return false;
        }

        float nowSeconds = absoluteTime(player, partialTick);
        var playback = UseAnimationPlaybackManager.INSTANCE.observe(
                player.getId(),
                SAPAnimations.HARROW,
                isHarrowDiggingUse(player),
                usedArm(player),
                nowSeconds);
        if (playback == null) {
            return false;
        }
        return UseAnimationPlayer.applyFirstPerson(
                SAPAnimations.HARROW,
                poseStack,
                player,
                arm,
                playback.actualUseArm(),
                playback.sample(nowSeconds));
    }

    private static InteractionHand handForArm(LocalPlayer player, HumanoidArm arm) {
        return player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    static void applyThirdPersonHarrowPose(
            HumanoidModel<?> model,
            HumanoidRenderState state,
            HumanoidArm arm
    ) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        float nowSeconds = state.ageInTicks / TICKS_PER_SECOND;
        var playback = UseAnimationPlaybackManager.INSTANCE.find(
                avatarState.id, SAPAnimations.HARROW, nowSeconds);
        if (playback == null) {
            return;
        }
        UseAnimationPlayer.applyThirdPerson(
                SAPAnimations.HARROW,
                model,
                state,
                playback.actualUseArm(),
                playback.sample(nowSeconds));
    }

    private static boolean isHarrowDiggingUse(LivingEntity entity) {
        return entity.isUsingItem()
                && entity.getUseItem().getItem() instanceof HarrowItem
                && entity.getUseItem().getUseAnimation()
                == HarrowUseAnimationEnumExtensions.getHarrowDigging();
    }

    private static HumanoidArm usedArm(LivingEntity entity) {
        return entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            UseAnimationPlaybackManager.INSTANCE.clear();
            return;
        }

        Set<Integer> livePlayerIds = new HashSet<>();
        for (var player : minecraft.level.players()) {
            livePlayerIds.add(player.getId());
            UseAnimationPlaybackManager.INSTANCE.observe(
                    player.getId(),
                    SAPAnimations.HARROW,
                    isHarrowDiggingUse(player),
                    usedArm(player),
                    absoluteTime(player, 0.0F));
        }
        UseAnimationPlaybackManager.INSTANCE.retainEntities(
                SAPAnimations.HARROW, livePlayerIds);
    }

    private static float absoluteTime(LivingEntity entity, float partialTick) {
        return (entity.tickCount + partialTick) / TICKS_PER_SECOND;
    }
}
