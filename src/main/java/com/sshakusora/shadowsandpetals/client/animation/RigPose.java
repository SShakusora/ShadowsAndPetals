package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.util.Mth;

import java.util.Set;

public final class RigPose {
    private final RigDefinition rig;
    private final BoneTransform[] transforms;

    private RigPose(RigDefinition rig, BoneTransform[] transforms) {
        this.rig = rig;
        this.transforms = transforms;
    }

    static RigPose rest(RigDefinition rig) {
        BoneTransform[] transforms = new BoneTransform[rig.boneCount()];
        for (int index = 0; index < transforms.length; index++) {
            transforms[index] = rig.bone(index).restTransform().copy();
        }
        return new RigPose(rig, transforms);
    }

    public RigDefinition rig() {
        return rig;
    }

    public BoneTransform transform(int index) {
        return transforms[index];
    }

    public BoneTransform transform(String boneName) {
        int index = rig.indexOf(boneName);
        if (index < 0) {
            throw new IllegalArgumentException("Rig " + rig.id() + " has no bone " + boneName);
        }
        return transforms[index];
    }

    public RigPose copy() {
        BoneTransform[] copied = new BoneTransform[transforms.length];
        for (int index = 0; index < transforms.length; index++) {
            copied[index] = transforms[index].copy();
        }
        return new RigPose(rig, copied);
    }

    public static RigPose blend(RigPose from, RigPose to, float weight, Set<String> boneMask) {
        requireSameRig(from, to);
        RigPose result = from.copy();
        for (int index = 0; index < result.transforms.length; index++) {
            if (boneMask.isEmpty() || boneMask.contains(result.rig.bone(index).name())) {
                result.transforms[index] = BoneTransform.blend(from.transforms[index], to.transforms[index], weight);
            }
        }
        return result;
    }

    public static RigPose additive(RigPose base, RigPose overlay, float weight, Set<String> boneMask) {
        requireSameRig(base, overlay);
        float clampedWeight = Mth.clamp(weight, 0.0F, 1.0F);
        RigPose result = base.copy();
        for (int index = 0; index < result.transforms.length; index++) {
            if (!boneMask.isEmpty() && !boneMask.contains(result.rig.bone(index).name())) {
                continue;
            }
            BoneTransform target = result.transforms[index];
            BoneTransform rest = result.rig.bone(index).restTransform();
            BoneTransform layer = overlay.transforms[index];
            target.translation().add(
                    (layer.translation().x - rest.translation().x) * clampedWeight,
                    (layer.translation().y - rest.translation().y) * clampedWeight,
                    (layer.translation().z - rest.translation().z) * clampedWeight
            );
            target.rotation().add(
                    (layer.rotation().x - rest.rotation().x) * clampedWeight,
                    (layer.rotation().y - rest.rotation().y) * clampedWeight,
                    (layer.rotation().z - rest.rotation().z) * clampedWeight
            );
            target.scale().mul(
                    1.0F + (layer.scale().x / rest.scale().x - 1.0F) * clampedWeight,
                    1.0F + (layer.scale().y / rest.scale().y - 1.0F) * clampedWeight,
                    1.0F + (layer.scale().z / rest.scale().z - 1.0F) * clampedWeight
            );
        }
        return result;
    }

    private static void requireSameRig(RigPose first, RigPose second) {
        if (first.rig != second.rig && !first.rig.id().equals(second.rig.id())) {
            throw new IllegalArgumentException("Cannot mix poses from " + first.rig.id() + " and " + second.rig.id());
        }
    }
}
