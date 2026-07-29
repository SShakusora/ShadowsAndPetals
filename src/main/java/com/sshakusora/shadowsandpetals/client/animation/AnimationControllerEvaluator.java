package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stateless evaluation entry point for resource-defined animation controllers.
 */
public final class AnimationControllerEvaluator {
    private AnimationControllerEvaluator() {
    }

    public static RigPose sample(
            AnimationResourceRef.State state,
            float localTimeSeconds
    ) {
        return sample(state.controller().id(), state.name(), localTimeSeconds);
    }

    public static RigPose sample(
            Identifier controllerId,
            String stateName,
            float localTimeSeconds
    ) {
        AnimationControllerDefinition controller =
                SAPAnimationResources.INSTANCE.controller(controllerId);
        return sample(controller, stateName, localTimeSeconds);
    }

    public static RigPose sample(
            AnimationControllerDefinition controller,
            String stateName,
            float localTimeSeconds
    ) {
        RigDefinition rig = SAPAnimationResources.INSTANCE.rig(controller.rig());
        return sampleState(controller.state(stateName), rig, localTimeSeconds);
    }

    public static RigPose apply(
            Identifier controllerId,
            String stateName,
            float localTimeSeconds,
            RigPose basePose,
            float weight
    ) {
        AnimationControllerDefinition controller =
                SAPAnimationResources.INSTANCE.controller(controllerId);
        return apply(controller, stateName, localTimeSeconds, basePose, weight);
    }

    public static RigPose apply(
            AnimationControllerDefinition controller,
            String stateName,
            float localTimeSeconds,
            RigPose basePose,
            float weight
    ) {
        RigDefinition rig = SAPAnimationResources.INSTANCE.rig(controller.rig());
        AnimationControllerDefinition.State state = controller.state(stateName);
        RigPose sampled = sampleState(state, rig, localTimeSeconds);
        return applySampledState(state, basePose, sampled, weight);
    }

    static RigPose sampleState(
            AnimationControllerDefinition.State state,
            RigDefinition rig,
            float localTimeSeconds
    ) {
        RigPose restPose = rig.restPose();
        if (state.clip() == null) {
            return restPose;
        }

        float clipTime = Math.max(0.0F, localTimeSeconds) * state.speed();
        RigPose sampled = AnimationClipSampler.sample(
                state.clip(), rig, clipTime, state.wrap());
        if (state.mask().isEmpty()) {
            return sampled;
        }
        return AnimationMixer.blend(restPose, sampled, 1.0F, state.mask());
    }

    static RigPose applySampledState(
            AnimationControllerDefinition.State state,
            RigPose basePose,
            RigPose sampledPose,
            float weight
    ) {
        if (state.additive()) {
            return AnimationMixer.additive(basePose, sampledPose, weight, state.mask());
        }
        return AnimationMixer.blend(basePose, sampledPose, weight, state.mask());
    }

    static List<Identifier> eventsBetween(
            AnimationControllerDefinition.State state,
            float fromLocalSeconds,
            float toLocalSeconds
    ) {
        if (state.clip() == null
                || state.events().isEmpty()
                || state.speed() <= 0.0F
                || toLocalSeconds <= fromLocalSeconds) {
            return List.of();
        }

        AnimationDefinition clip = AnimationLoader.INSTANCE.getAnimation(state.clip());
        if (clip == null || clip.lengthInSeconds() <= 0.0F) {
            return List.of();
        }

        return eventsBetween(clip, state, fromLocalSeconds, toLocalSeconds);
    }

    static List<Identifier> eventsBetween(
            AnimationDefinition clip,
            AnimationControllerDefinition.State state,
            float fromLocalSeconds,
            float toLocalSeconds
    ) {
        if (state.events().isEmpty()
                || state.speed() <= 0.0F
                || clip.lengthInSeconds() <= 0.0F
                || toLocalSeconds <= fromLocalSeconds
                || toLocalSeconds < 0.0F) {
            return List.of();
        }

        boolean includesStateStart = fromLocalSeconds < 0.0F && toLocalSeconds >= 0.0F;
        float fromClipTime = Math.max(0.0F, fromLocalSeconds) * state.speed();
        float toClipTime = Math.max(0.0F, toLocalSeconds) * state.speed();
        boolean looping = state.wrap() == ClipWrap.LOOP
                || state.wrap() == ClipWrap.DEFINITION && clip.looping();
        List<EventOccurrence> occurrences = new ArrayList<>();

        if (!looping) {
            float clampedTo = Math.min(toClipTime, clip.lengthInSeconds());
            for (AnimationControllerDefinition.EventMarker marker : state.events()) {
                if ((marker.time() > fromClipTime
                        || includesStateStart && marker.time() == 0.0F)
                        && marker.time() <= clampedTo) {
                    occurrences.add(new EventOccurrence(marker.time(), marker.id()));
                }
            }
            return orderedEventIds(occurrences);
        }

        double length = clip.lengthInSeconds();
        for (AnimationControllerDefinition.EventMarker marker : state.events()) {
            if (includesStateStart && marker.time() == 0.0F) {
                occurrences.add(new EventOccurrence(0.0, marker.id()));
            }
            long firstCycle = (long) Math.floor((fromClipTime - marker.time()) / length) + 1L;
            long lastCycle = (long) Math.floor((toClipTime - marker.time()) / length);
            for (long cycle = Math.max(0L, firstCycle); cycle <= lastCycle; cycle++) {
                occurrences.add(new EventOccurrence(
                        marker.time() + cycle * length,
                        marker.id()
                ));
            }
        }
        return orderedEventIds(occurrences);
    }

    private static List<Identifier> orderedEventIds(
            List<EventOccurrence> occurrences
    ) {
        occurrences.sort(Comparator.comparingDouble(EventOccurrence::time));
        return occurrences.stream().map(EventOccurrence::id).toList();
    }

    private record EventOccurrence(double time, Identifier id) {
    }
}
