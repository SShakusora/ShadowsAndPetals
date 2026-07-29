package com.sshakusora.shadowsandpetals.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PoseStackRigBinder {
    private static final float MODEL_PIXELS_PER_BLOCK = 16.0F;

    private PoseStackRigBinder() {
    }

    public static void apply(PoseStack poseStack, RigPose pose, String boneName) {
        apply(poseStack, pose, boneName, false);
    }

    public static void apply(
            PoseStack poseStack,
            RigPose pose,
            AnimationResourceRef.Bone bone
    ) {
        apply(poseStack, pose, bone, false);
    }

    public static void apply(
            PoseStack poseStack,
            RigPose pose,
            AnimationResourceRef.Bone bone,
            boolean mirrorX
    ) {
        if (!pose.rig().id().equals(bone.rig().id())) {
            throw new IllegalArgumentException(
                    "Bone expects rig " + bone.rig().id() + ", got " + pose.rig().id());
        }
        apply(poseStack, pose, bone.name(), mirrorX);
    }

    public static void apply(
            PoseStack poseStack,
            RigPose pose,
            AnimationResourceRef.Socket socket,
            boolean mirrorX
    ) {
        requireRig(pose, socket.rig());
        apply(poseStack, pose, socket.name(), mirrorX);
    }

    /**
     * Applies only the authored transform of one bone/socket, without its
     * parent chain or pivot. Use this when Minecraft already positioned the
     * stack at the authored attachment point, such as a third-person hand.
     */
    public static void applyLocalTransform(
            PoseStack poseStack,
            RigPose pose,
            AnimationResourceRef.Socket socket,
            boolean mirrorX
    ) {
        requireRig(pose, socket.rig());
        int boneIndex = pose.rig().indexOf(socket.name());
        if (boneIndex < 0) {
            throw new IllegalArgumentException(
                    "Rig " + pose.rig().id() + " has no socket " + socket.name());
        }
        applyTransform(
                poseStack,
                mirrorX ? pose.transform(boneIndex).mirroredX() : pose.transform(boneIndex)
        );
    }

    public static void apply(PoseStack poseStack, RigPose pose, String boneName, boolean mirrorX) {
        RigDefinition rig = pose.rig();
        int boneIndex = rig.indexOf(boneName);
        if (boneIndex < 0) {
            throw new IllegalArgumentException("Rig " + rig.id() + " has no bone " + boneName);
        }

        for (int chainIndex : rig.chainTo(boneIndex)) {
            RigDefinition.Bone bone = rig.bone(chainIndex);
            BoneTransform transform = mirrorX
                    ? pose.transform(chainIndex).mirroredX()
                    : pose.transform(chainIndex);
            Vector3f pivot = bone.pivot();
            float pivotX = (mirrorX ? -pivot.x : pivot.x) / MODEL_PIXELS_PER_BLOCK;
            float pivotY = pivot.y / MODEL_PIXELS_PER_BLOCK;
            float pivotZ = pivot.z / MODEL_PIXELS_PER_BLOCK;

            applyTranslation(poseStack, transform);
            poseStack.translate(pivotX, pivotY, pivotZ);
            applyRotationAndScale(poseStack, transform);
            poseStack.translate(-pivotX, -pivotY, -pivotZ);
        }
    }

    private static void applyTransform(PoseStack poseStack, BoneTransform transform) {
        applyTranslation(poseStack, transform);
        applyRotationAndScale(poseStack, transform);
    }

    private static void applyTranslation(PoseStack poseStack, BoneTransform transform) {
        poseStack.translate(
                transform.translation().x / MODEL_PIXELS_PER_BLOCK,
                transform.translation().y / MODEL_PIXELS_PER_BLOCK,
                transform.translation().z / MODEL_PIXELS_PER_BLOCK
        );
    }

    private static void applyRotationAndScale(PoseStack poseStack, BoneTransform transform) {
        if (transform.rotation().lengthSquared() > 0.0F) {
            poseStack.mulPose(new Quaternionf().rotationZYX(
                    transform.rotation().z,
                    transform.rotation().y,
                    transform.rotation().x
            ));
        }
        if (!transform.scale().equals(1.0F, 1.0F, 1.0F)) {
            poseStack.scale(transform.scale().x, transform.scale().y, transform.scale().z);
        }
    }

    private static void requireRig(RigPose pose, AnimationResourceRef.Rig rig) {
        if (!pose.rig().id().equals(rig.id())) {
            throw new IllegalArgumentException(
                    "Animation reference expects rig " + rig.id() + ", got " + pose.rig().id());
        }
    }

}
