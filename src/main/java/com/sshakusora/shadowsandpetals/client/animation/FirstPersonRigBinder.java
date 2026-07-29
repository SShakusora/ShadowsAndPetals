package com.sshakusora.shadowsandpetals.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Rendering adapter from a rig pose to Minecraft's first-person arm and item
 * submission APIs. The rig rest pose owns the view-space calibration.
 */
public final class FirstPersonRigBinder {
    private FirstPersonRigBinder() {
    }

    public static void renderArm(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            LocalPlayer player,
            HumanoidArm arm,
            RigPose pose,
            AnimationResourceRef.Bone bone,
            boolean mirrorX
    ) {
        renderArm(poseStack, collector, packedLight, player, arm, pose, bone.name(), mirrorX);
    }

    public static void renderArm(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            LocalPlayer player,
            HumanoidArm arm,
            RigPose pose,
            String boneName,
            boolean mirrorX
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        PoseStackRigBinder.apply(poseStack, pose, boneName, mirrorX);

        AvatarRenderer<AbstractClientPlayer> renderer =
                minecraft.getEntityRenderDispatcher().getPlayerRenderer(player);
        var skin = player.getSkin().body().texturePath();
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(
                    poseStack,
                    collector,
                    packedLight,
                    skin,
                    player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE),
                    player
            );
        } else {
            renderer.renderLeftHand(
                    poseStack,
                    collector,
                    packedLight,
                    skin,
                    player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE),
                    player
            );
        }
        poseStack.popPose();
    }

    public static void renderItem(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack item,
            RigPose pose,
            AnimationResourceRef.Bone bone,
            boolean mirrorX
    ) {
        renderItem(poseStack, collector, packedLight, player, arm, item, pose, bone.name(), mirrorX);
    }

    public static void renderItem(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack item,
            RigPose pose,
            AnimationResourceRef.Socket socket,
            boolean mirrorX
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        PoseStackRigBinder.apply(poseStack, pose, socket, mirrorX);
        submitItem(minecraft, poseStack, collector, packedLight, player, arm, item);
        poseStack.popPose();
    }

    public static void renderItem(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack item,
            RigPose pose,
            String boneName,
            boolean mirrorX
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        PoseStackRigBinder.apply(poseStack, pose, boneName, mirrorX);
        submitItem(minecraft, poseStack, collector, packedLight, player, arm, item);
        poseStack.popPose();
    }

    private static void submitItem(
            Minecraft minecraft,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack item
    ) {
        minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                player,
                item,
                arm == HumanoidArm.RIGHT
                        ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack,
                collector,
                packedLight
        );
    }
}
