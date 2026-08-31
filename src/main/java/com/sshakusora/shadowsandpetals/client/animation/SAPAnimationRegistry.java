package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class SAPAnimationRegistry {
    private static final Map<AnimationResourceRef.Controller, Registration> REGISTRATIONS =
            new LinkedHashMap<>();
    private static final Map<Identifier, UseAnimationProfile> PROFILES =
            new LinkedHashMap<>();
    private static final Map<Identifier, BlockAnimationDefinition> BLOCK_ANIMATIONS =
            new LinkedHashMap<>();

    private SAPAnimationRegistry() {
    }

    public static synchronized Registration register(
            Set<Target> targets,
            AnimationResourceRef.Rig rig,
            AnimationResourceRef.Controller controller,
            AnimationResourceRef.Clip... clips
    ) {
        return register(new Registration(targets, rig, controller, Set.of(clips)));
    }

    public static synchronized Registration register(Registration registration) {
        Objects.requireNonNull(registration, "registration");
        Registration previous = REGISTRATIONS.putIfAbsent(registration.controller(), registration);
        if (previous != null && !previous.equals(registration)) {
            throw new IllegalStateException(
                    "Animation controller is already registered: " + registration.controller().id());
        }
        return previous == null ? registration : previous;
    }

    public static synchronized UseAnimationProfile register(
            UseAnimationProfile profile
    ) {
        Objects.requireNonNull(profile, "profile");
        UseAnimationProfile previous = PROFILES.get(profile.id());
        if (previous != null && !previous.equals(profile)) {
            throw new IllegalStateException(
                    "Use-animation profile is already registered: " + profile.id());
        }

        register(new Registration(
                profile.targets(),
                profile.rig(),
                profile.controller(),
                profile.clips()));
        if (previous == null) {
            PROFILES.put(profile.id(), profile);
            return profile;
        }
        return previous;
    }

    /**
     * Registers a controller intended for a block-entity renderer.
     */
    public static synchronized BlockAnimationDefinition register(
            BlockAnimationDefinition definition
    ) {
        Objects.requireNonNull(definition, "definition");
        BlockAnimationDefinition previous = BLOCK_ANIMATIONS.get(definition.id());
        if (previous != null && !previous.equals(definition)) {
            throw new IllegalStateException(
                    "Block animation is already registered: " + definition.id());
        }
        register(new Registration(
                Set.of(Target.BLOCK_ENTITY),
                definition.rig(),
                definition.controller(),
                definition.clips()
        ));
        if (previous == null) {
            BLOCK_ANIMATIONS.put(definition.id(), definition);
        }
        return previous == null ? definition : previous;
    }

    public static synchronized @Nullable UseAnimationProfile findProfile(
            Identifier id
    ) {
        return PROFILES.get(id);
    }

    public static synchronized Set<UseAnimationProfile> profiles() {
        return Set.copyOf(PROFILES.values());
    }

    public static synchronized @Nullable BlockAnimationDefinition findBlockAnimation(
            Identifier id
    ) {
        return BLOCK_ANIMATIONS.get(id);
    }

    public static synchronized Set<BlockAnimationDefinition> blockAnimations() {
        return Set.copyOf(BLOCK_ANIMATIONS.values());
    }

    static synchronized Snapshot snapshot() {
        Set<Registration> registrations = Set.copyOf(REGISTRATIONS.values());
        Set<AnimationResourceRef.Rig> rigs = new LinkedHashSet<>();
        Set<AnimationResourceRef.Controller> controllers = new LinkedHashSet<>();
        Set<AnimationResourceRef.Clip> clips = new LinkedHashSet<>();
        for (Registration registration : registrations) {
            rigs.add(registration.rig());
            controllers.add(registration.controller());
            clips.addAll(registration.clips());
        }
        return new Snapshot(
                registrations,
                Set.copyOf(rigs),
                Set.copyOf(controllers),
                Set.copyOf(clips),
                Set.copyOf(PROFILES.values()),
                Set.copyOf(BLOCK_ANIMATIONS.values()));
    }

    public enum Target {
        FIRST_PERSON,
        THIRD_PERSON,
        BLOCK_ENTITY
    }

    public record Registration(
            Set<Target> targets,
            AnimationResourceRef.Rig rig,
            AnimationResourceRef.Controller controller,
            Set<AnimationResourceRef.Clip> clips
    ) {
        public Registration {
            Objects.requireNonNull(targets, "targets");
            Objects.requireNonNull(rig, "rig");
            Objects.requireNonNull(controller, "controller");
            Objects.requireNonNull(clips, "clips");
            if (targets.isEmpty()) {
                throw new IllegalArgumentException("Animation registration needs at least one target");
            }
            targets = Set.copyOf(EnumSet.copyOf(targets));
            clips = Set.copyOf(clips);
        }
    }

    record Snapshot(
            Set<Registration> registrations,
            Set<AnimationResourceRef.Rig> rigs,
            Set<AnimationResourceRef.Controller> controllers,
            Set<AnimationResourceRef.Clip> clips,
            Set<UseAnimationProfile> profiles,
            Set<BlockAnimationDefinition> blockAnimations
    ) {
    }
}
