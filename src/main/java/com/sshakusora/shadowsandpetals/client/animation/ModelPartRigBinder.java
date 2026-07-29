package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.client.model.geom.ModelPart;

public final class ModelPartRigBinder {
    private ModelPartRigBinder() {
    }

    public static void apply(
            RigPose pose,
            AnimationResourceRef.Bone bone,
            ModelPart part,
            boolean mirrorX,
            RotationMode rotationMode
    ) {
        if (!pose.rig().id().equals(bone.rig().id())) {
            throw new IllegalArgumentException(
                    "Bone expects rig " + bone.rig().id() + ", got " + pose.rig().id());
        }
        apply(pose, bone.name(), part, mirrorX, rotationMode);
    }

    public static void apply(
            RigPose pose,
            String boneName,
            ModelPart part,
            boolean mirrorX,
            RotationMode rotationMode
    ) {
        BoneTransform transform = mirrorX
                ? pose.transform(boneName).mirroredX()
                : pose.transform(boneName);

        part.x += transform.translation().x;
        part.y -= transform.translation().y;
        part.z += transform.translation().z;
        if (rotationMode == RotationMode.REPLACE) {
            part.setRotation(transform.rotation().x, transform.rotation().y, transform.rotation().z);
        } else {
            part.xRot += transform.rotation().x;
            part.yRot += transform.rotation().y;
            part.zRot += transform.rotation().z;
        }
        part.xScale *= transform.scale().x;
        part.yScale *= transform.scale().y;
        part.zScale *= transform.scale().z;
    }

    public enum RotationMode {
        ADDITIVE,
        REPLACE
    }
}
