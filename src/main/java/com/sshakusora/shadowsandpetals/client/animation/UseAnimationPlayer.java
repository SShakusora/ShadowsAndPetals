package com.sshakusora.shadowsandpetals.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;

import java.util.Objects;

/**
 * Shared runtime adapter used by item client extensions and extensible arm-pose
 * transformers. Item code supplies only business state and local time; this
 * class owns all PoseStack and ModelPart application logic.
 */
public final class UseAnimationPlayer {
    private UseAnimationPlayer() {
    }

    public static boolean applyFirstPerson(
            UseAnimationProfile profile,
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm renderedArm,
            HumanoidArm actualUseArm,
            float localTimeSeconds
    ) {
        return applyFirstPerson(
                profile,
                profile.defaultState(),
                poseStack,
                player,
                renderedArm,
                actualUseArm,
                localTimeSeconds);
    }

    public static boolean applyFirstPerson(
            UseAnimationProfile profile,
            AnimationResourceRef.State state,
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm renderedArm,
            HumanoidArm actualUseArm,
            float localTimeSeconds
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(renderedArm, "renderedArm");
        Objects.requireNonNull(actualUseArm, "actualUseArm");
        profile.requireState(state);

        UseAnimationProfile.FirstPersonBinding binding = profile.firstPerson();
        if (binding == null) {
            return false;
        }
        UseAnimationProfile.ResolvedSocket resolved =
                binding.resolve(renderedArm, actualUseArm);
        if (resolved == null) {
            return false;
        }

        RigPose pose = AnimationControllerEvaluator.sample(
                state, requireTime(localTimeSeconds));
        return applyFirstPerson(
                profile, poseStack, player, renderedArm, actualUseArm, pose);
    }

    public static boolean applyFirstPerson(
            UseAnimationProfile profile,
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm renderedArm,
            HumanoidArm actualUseArm,
            RigPose pose
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(pose, "pose");
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(renderedArm, "renderedArm");
        Objects.requireNonNull(actualUseArm, "actualUseArm");

        UseAnimationProfile.FirstPersonBinding binding = profile.firstPerson();
        if (binding == null) {
            return false;
        }
        UseAnimationProfile.ResolvedSocket resolved =
                binding.resolve(renderedArm, actualUseArm);
        if (resolved == null) {
            return false;
        }
        PoseStackRigBinder.apply(
                poseStack, pose, resolved.socket(), resolved.mirrorX());
        return true;
    }

    public static boolean applyThirdPerson(
            UseAnimationProfile profile,
            HumanoidModel<?> model,
            HumanoidRenderState renderState,
            HumanoidArm actualUseArm,
            float localTimeSeconds
    ) {
        return applyThirdPerson(
                profile,
                profile.defaultState(),
                model,
                renderState,
                actualUseArm,
                localTimeSeconds);
    }

    public static boolean applyThirdPerson(
            UseAnimationProfile profile,
            AnimationResourceRef.State state,
            HumanoidModel<?> model,
            HumanoidRenderState renderState,
            HumanoidArm actualUseArm,
            float localTimeSeconds
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(renderState, "renderState");
        Objects.requireNonNull(actualUseArm, "actualUseArm");
        profile.requireState(state);

        UseAnimationProfile.ThirdPersonBinding binding = profile.thirdPerson();
        if (binding == null) {
            return false;
        }

        RigPose pose = AnimationControllerEvaluator.sample(
                state, requireTime(localTimeSeconds));
        return applyThirdPerson(
                profile, model, renderState, actualUseArm, pose);
    }

    public static boolean applyThirdPerson(
            UseAnimationProfile profile,
            HumanoidModel<?> model,
            HumanoidRenderState renderState,
            HumanoidArm actualUseArm,
            RigPose pose
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(renderState, "renderState");
        Objects.requireNonNull(actualUseArm, "actualUseArm");
        Objects.requireNonNull(pose, "pose");

        UseAnimationProfile.ThirdPersonBinding binding = profile.thirdPerson();
        if (binding == null) {
            return false;
        }
        for (UseAnimationProfile.HumanoidBone renderedBone
                : UseAnimationProfile.HumanoidBone.values()) {
            UseAnimationProfile.ResolvedBone resolved =
                    binding.resolve(renderedBone, actualUseArm);
            if (resolved == null) {
                continue;
            }
            ModelPartRigBinder.apply(
                    pose,
                    resolved.bone(),
                    modelPart(model, renderedBone),
                    resolved.mirrorX(),
                    binding.rotationMode());
        }
        return true;
    }

    private static ModelPart modelPart(
            HumanoidModel<?> model,
            UseAnimationProfile.HumanoidBone bone
    ) {
        return switch (bone) {
            case ROOT -> model.root();
            case BODY -> model.body;
            case HEAD -> model.head;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
        };
    }

    private static float requireTime(float localTimeSeconds) {
        if (!Float.isFinite(localTimeSeconds) || localTimeSeconds < 0.0F) {
            throw new IllegalArgumentException(
                    "Animation time must be finite and non-negative");
        }
        return localTimeSeconds;
    }
}
