package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A bone-local transform in Blockbench coordinates.
 * Translation is measured in model pixels, rotation in radians, and scale is multiplicative.
 */
public final class BoneTransform {
    private final Vector3f translation;
    private final Vector3f rotation;
    private final Vector3f scale;

    public BoneTransform() {
        this(new Vector3f(), new Vector3f(), new Vector3f(1.0F));
    }

    public BoneTransform(Vector3fc translation, Vector3fc rotation, Vector3fc scale) {
        this.translation = new Vector3f(translation);
        this.rotation = new Vector3f(rotation);
        this.scale = new Vector3f(scale);
    }

    public Vector3f translation() {
        return translation;
    }

    public Vector3f rotation() {
        return rotation;
    }

    public Vector3f scale() {
        return scale;
    }

    public BoneTransform copy() {
        return new BoneTransform(translation, rotation, scale);
    }

    public BoneTransform mirroredX() {
        return new BoneTransform(
                new Vector3f(-translation.x, translation.y, translation.z),
                new Vector3f(rotation.x, -rotation.y, -rotation.z),
                scale
        );
    }

    public void addTranslation(float x, float y, float z) {
        translation.add(x, y, z);
    }

    public void addRotation(float x, float y, float z) {
        rotation.add(x, y, z);
    }

    public void addScaleOffset(float x, float y, float z) {
        scale.add(x, y, z);
    }

    public static BoneTransform blend(BoneTransform from, BoneTransform to, float weight) {
        float clampedWeight = Mth.clamp(weight, 0.0F, 1.0F);
        return new BoneTransform(
                new Vector3f(from.translation).lerp(to.translation, clampedWeight),
                new Vector3f(
                        lerpAngle(from.rotation.x, to.rotation.x, clampedWeight),
                        lerpAngle(from.rotation.y, to.rotation.y, clampedWeight),
                        lerpAngle(from.rotation.z, to.rotation.z, clampedWeight)
                ),
                new Vector3f(from.scale).lerp(to.scale, clampedWeight)
        );
    }

    private static float lerpAngle(float from, float to, float weight) {
        float delta = (float) Math.atan2(Math.sin(to - from), Math.cos(to - from));
        return from + delta * weight;
    }
}
