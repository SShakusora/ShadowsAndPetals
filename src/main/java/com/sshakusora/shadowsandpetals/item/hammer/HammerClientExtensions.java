package com.sshakusora.shadowsandpetals.item.hammer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.shadowsandpetals.client.animation.SAPAnimations;
import com.sshakusora.shadowsandpetals.client.animation.UseAnimationPlaybackManager;
import com.sshakusora.shadowsandpetals.client.animation.UseAnimationPlayer;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
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

public class HammerClientExtensions implements IClientItemExtensions {
    private static final float TICKS_PER_SECOND = 20.0F;

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (!(stack.getItem() instanceof HammerItem) || hand != InteractionHand.MAIN_HAND) {
            return null;
        }
        var playback = UseAnimationPlaybackManager.INSTANCE.observe(
                entity.getId(),
                SAPAnimations.HAMMER,
                isHammerAndChiselUse(entity),
                entity.getMainArm(),
                absoluteTime(entity, 0.0F));
        return playback == null
                ? null
                : HammerArmPoseEnumExtensions.getHammerAndChiselPose();
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

        if (!(itemInHand.getItem() instanceof HammerItem) && !itemInHand.is(ItemRegistry.CHISEL.get())) {
            return false;
        }

        float nowSeconds = absoluteTime(player, partialTick);
        var playback = UseAnimationPlaybackManager.INSTANCE.observe(
                player.getId(),
                SAPAnimations.HAMMER,
                isHammerAndChiselUse(player),
                player.getMainArm(),
                nowSeconds);
        if (playback == null) {
            return false;
        }
        return UseAnimationPlayer.applyFirstPerson(
                SAPAnimations.HAMMER,
                poseStack,
                player,
                arm,
                playback.actualUseArm(),
                playback.sample(nowSeconds));
    }

    private static boolean isHammerAndChiselUse(LivingEntity entity) {
        return entity.isUsingItem()
                && entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                && entity.getMainHandItem().getItem() instanceof HammerItem
                && entity.getOffhandItem().is(ItemRegistry.CHISEL.get());
    }

    static void applyThirdPersonHammerPose(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        float nowSeconds = state.ageInTicks / TICKS_PER_SECOND;
        var playback = UseAnimationPlaybackManager.INSTANCE.find(
                avatarState.id, SAPAnimations.HAMMER, nowSeconds);
        if (playback == null) {
            return;
        }
        UseAnimationPlayer.applyThirdPerson(
                SAPAnimations.HAMMER,
                model,
                state,
                playback.actualUseArm(),
                playback.sample(nowSeconds));
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
                    SAPAnimations.HAMMER,
                    isHammerAndChiselUse(player),
                    player.getMainArm(),
                    absoluteTime(player, 0.0F));
        }
        UseAnimationPlaybackManager.INSTANCE.retainEntities(
                SAPAnimations.HAMMER, livePlayerIds);
    }

    private static float absoluteTime(LivingEntity entity, float partialTick) {
        return (entity.tickCount + partialTick) / TICKS_PER_SECOND;
    }
}
