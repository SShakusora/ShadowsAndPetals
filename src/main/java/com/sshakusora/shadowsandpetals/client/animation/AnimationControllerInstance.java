package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AnimationControllerInstance {
    private static final ResourceLookup RELOADABLE_RESOURCES = new ResourceLookup() {
        @Override
        public AnimationControllerDefinition controller(ResourceLocation id) {
            return SAPAnimationResources.INSTANCE.controller(id);
        }

        @Override
        public RigDefinition rig(ResourceLocation id) {
            return SAPAnimationResources.INSTANCE.rig(id);
        }
    };

    private final ResourceLocation controllerId;
    private final ResourceLookup resources;
    private final StateSampler stateSampler;
    private String currentState;
    private float stateStartSeconds;
    private float transitionStartSeconds;
    private float transitionDurationSeconds;
    private @Nullable RigPose transitionFromPose;

    public AnimationControllerInstance(
            AnimationResourceRef.Controller controller,
            float nowSeconds
    ) {
        this(controller.id(), nowSeconds);
    }

    public AnimationControllerInstance(ResourceLocation controllerId, float nowSeconds) {
        this(controllerId, nowSeconds, RELOADABLE_RESOURCES,
                AnimationControllerEvaluator::sampleState);
    }

    AnimationControllerInstance(
            ResourceLocation controllerId,
            float nowSeconds,
            ResourceLookup resources,
            StateSampler stateSampler
    ) {
        this.controllerId = controllerId;
        this.resources = resources;
        this.stateSampler = stateSampler;
        AnimationControllerDefinition definition = resources.controller(controllerId);
        this.currentState = definition.initialState();
        this.stateStartSeconds = nowSeconds;
        this.transitionStartSeconds = nowSeconds;
    }

    public ResourceLocation controllerId() {
        return controllerId;
    }

    public String currentState() {
        return currentState;
    }

    public void play(String state, float nowSeconds) {
        AnimationControllerDefinition definition = definition(nowSeconds);
        definition.state(state);
        if (currentState.equals(state)) {
            return;
        }
        RigDefinition rig = resources.rig(definition.rig());
        transitionFromPose = sample(definition, rig, nowSeconds);
        transitionDurationSeconds = definition.transitionDuration(currentState, state);
        currentState = state;
        stateStartSeconds = nowSeconds;
        transitionStartSeconds = nowSeconds;
    }

    public void play(AnimationResourceRef.State state, float nowSeconds) {
        if (!state.controller().id().equals(controllerId)) {
            throw new IllegalArgumentException(
                    "State belongs to controller " + state.controller().id()
                            + ", instance uses " + controllerId);
        }
        play(state.name(), nowSeconds);
    }

    public RigPose sample(float nowSeconds) {
        AnimationControllerDefinition definition = definition(nowSeconds);
        RigDefinition rig = resources.rig(definition.rig());
        return sample(definition, rig, nowSeconds);
    }

    public RigPose applyTo(RigPose basePose, float nowSeconds, float weight) {
        AnimationControllerDefinition definition = definition(nowSeconds);
        RigDefinition rig = resources.rig(definition.rig());
        RigPose sampled = sample(definition, rig, nowSeconds);
        return AnimationControllerEvaluator.applySampledState(
                definition.state(currentState), basePose, sampled, weight);
    }

    private RigPose sample(
            AnimationControllerDefinition definition,
            RigDefinition rig,
            float nowSeconds
    ) {
        if (transitionFromPose != null && transitionFromPose.rig() != rig) {
            clearTransition();
        }
        AnimationControllerDefinition.State current = definition.state(currentState);
        RigPose currentPose = stateSampler.sample(
                current, rig, nowSeconds - stateStartSeconds);
        if (transitionFromPose == null || transitionDurationSeconds <= 0.0F) {
            clearTransition();
            return currentPose;
        }
        float transitionProgress =
                (nowSeconds - transitionStartSeconds) / transitionDurationSeconds;
        if (transitionProgress >= 1.0F) {
            clearTransition();
            return currentPose;
        }
        return AnimationMixer.blend(transitionFromPose, currentPose, transitionProgress);
    }

    public List<ResourceLocation> eventsBetween(float fromSeconds, float toSeconds) {
        AnimationControllerDefinition definition = definition(toSeconds);
        AnimationControllerDefinition.State state = definition.state(currentState);
        return AnimationControllerEvaluator.eventsBetween(
                state,
                fromSeconds - stateStartSeconds,
                toSeconds - stateStartSeconds
        );
    }

    private AnimationControllerDefinition definition(float nowSeconds) {
        AnimationControllerDefinition definition = resources.controller(controllerId);
        if (!definition.states().containsKey(currentState)) {
            currentState = definition.initialState();
            stateStartSeconds = nowSeconds;
            clearTransition();
        }
        return definition;
    }

    private void clearTransition() {
        transitionFromPose = null;
        transitionDurationSeconds = 0.0F;
    }

    interface ResourceLookup {
        AnimationControllerDefinition controller(ResourceLocation id);

        RigDefinition rig(ResourceLocation id);
    }

    @FunctionalInterface
    interface StateSampler {
        RigPose sample(
                AnimationControllerDefinition.State state,
                RigDefinition rig,
                float localTimeSeconds
        );
    }
}