package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record AnimationControllerDefinition(
        Identifier id,
        Identifier rig,
        String initialState,
        Map<String, State> states,
        List<Transition> transitions
) {
    public AnimationControllerDefinition {
        states = Map.copyOf(states);
        transitions = List.copyOf(transitions);
        if (states.isEmpty()) {
            throw new IllegalArgumentException("Controller " + id + " has no states");
        }
        if (!states.containsKey(initialState)) {
            throw new IllegalArgumentException("Controller " + id + " has no initial state " + initialState);
        }
        if (states.keySet().stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Controller " + id + " has a blank state name");
        }
    }

    public State state(String name) {
        State result = states.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Controller " + id + " has no state " + name);
        }
        return result;
    }

    public float transitionDuration(String from, String to) {
        for (Transition transition : transitions) {
            if (transition.from().equals(from) && transition.to().equals(to)) {
                return transition.duration();
            }
        }
        return 0.0F;
    }

    public record State(
            @Nullable Identifier clip,
            float speed,
            ClipWrap wrap,
            Set<String> mask,
            boolean additive,
            List<EventMarker> events
    ) {
        public State {
            mask = Set.copyOf(mask);
            events = List.copyOf(events);
            if (!Float.isFinite(speed) || speed < 0.0F) {
                throw new IllegalArgumentException("Animation state has invalid speed " + speed);
            }
            float previousTime = -1.0F;
            for (EventMarker event : events) {
                if (!Float.isFinite(event.time())
                        || event.time() < previousTime
                        || event.time() < 0.0F) {
                    throw new IllegalArgumentException(
                            "Animation state has invalid event time " + event.time());
                }
                previousTime = event.time();
            }
        }
    }

    public record Transition(String from, String to, float duration) {
        public Transition {
            if (from.isBlank() || to.isBlank()
                    || !Float.isFinite(duration) || duration < 0.0F) {
                throw new IllegalArgumentException("Invalid animation transition");
            }
        }
    }

    public record EventMarker(float time, Identifier id) {
    }
}
