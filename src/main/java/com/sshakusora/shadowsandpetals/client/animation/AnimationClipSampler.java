package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.entity.animation.AnimationTarget;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;
import net.neoforged.neoforge.client.entity.animation.json.AnimationTypeManager;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public final class AnimationClipSampler {
    private AnimationClipSampler() {
    }

    public static RigPose sample(
            Identifier clipId,
            RigDefinition rig,
            float timeSeconds,
            ClipWrap wrap
    ) {
        AnimationDefinition definition = AnimationLoader.INSTANCE.getAnimation(clipId);
        return definition == null ? rig.restPose() : sample(definition, rig, timeSeconds, wrap);
    }

    public static RigPose sample(
            AnimationDefinition definition,
            RigDefinition rig,
            float timeSeconds,
            ClipWrap wrap
    ) {
        RigPose pose = rig.restPose();
        if (definition.lengthInSeconds() <= 0.0F) {
            return pose;
        }

        float resolvedTime = resolveTime(definition, timeSeconds, wrap);
        Vector3f scratch = new Vector3f();
        for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            int boneIndex = rig.indexOf(entry.getKey());
            if (boneIndex < 0) {
                continue;
            }
            BoneTransform transform = pose.transform(boneIndex);
            for (AnimationChannel channel : entry.getValue()) {
                AnimationTarget target = AnimationTypeManager.getTargetFromChannelTarget(channel.target());
                if (target == null || channel.keyframes().length == 0) {
                    continue;
                }
                sampleChannel(channel, resolvedTime, scratch);
                if (target == AnimationTarget.POSITION) {
                    // NeoForge stores entity-animation Y in ModelPart space; RigPose uses Blockbench/PoseStack space.
                    transform.addTranslation(scratch.x, -scratch.y, scratch.z);
                } else if (target == AnimationTarget.ROTATION) {
                    transform.addRotation(scratch.x, scratch.y, scratch.z);
                } else if (target == AnimationTarget.SCALE) {
                    transform.addScaleOffset(scratch.x, scratch.y, scratch.z);
                }
            }
        }
        return pose;
    }

    private static float resolveTime(AnimationDefinition definition, float timeSeconds, ClipWrap wrap) {
        boolean looping = wrap == ClipWrap.LOOP
                || wrap == ClipWrap.DEFINITION && definition.looping();
        if (looping) {
            return Mth.positiveModulo(timeSeconds, definition.lengthInSeconds());
        }
        return Mth.clamp(timeSeconds, 0.0F, definition.lengthInSeconds());
    }

    private static void sampleChannel(AnimationChannel channel, float timeSeconds, Vector3f output) {
        Keyframe[] keyframes = channel.keyframes();
        int previousIndex = findPreviousKeyframe(keyframes, timeSeconds);
        int nextIndex = Math.min(keyframes.length - 1, previousIndex + 1);
        Keyframe previous = keyframes[previousIndex];
        Keyframe next = keyframes[nextIndex];
        float alpha = nextIndex == previousIndex
                ? 0.0F
                : Mth.clamp(
                        (timeSeconds - previous.timestamp()) / (next.timestamp() - previous.timestamp()),
                        0.0F,
                        1.0F
                );
        next.interpolation().apply(output, alpha, keyframes, previousIndex, nextIndex, 1.0F);
    }

    private static int findPreviousKeyframe(Keyframe[] keyframes, float timeSeconds) {
        int low = 0;
        int high = keyframes.length;
        while (low < high) {
            int middle = low + high >>> 1;
            if (timeSeconds <= keyframes[middle].timestamp()) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return Math.max(0, low - 1);
    }
}
