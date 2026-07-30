package com.sshakusora.shadowsandpetals.client.animation.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.animation.AnimationResourceRef;
import com.sshakusora.shadowsandpetals.client.animation.ModelPartRigBinder;
import com.sshakusora.shadowsandpetals.client.animation.SAPAnimationRegistry;
import com.sshakusora.shadowsandpetals.client.animation.UseAnimationProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

import java.util.*;
import java.util.function.Consumer;

/**
 * Fluent builder for reusable, resource-driven item-use animation profiles.
 *
 * <p>Resource paths passed as strings use the Shadows & Petals namespace.
 * Bone and socket names are resolved against the configured rig when the
 * profile is registered.</p>
 */
public final class RegUseAnimationBuilder {
    private final Identifier profileId;
    private AnimationResourceRef.Rig rig;
    private AnimationResourceRef.Controller controller;
    private final Set<AnimationResourceRef.Clip> clips = new LinkedHashSet<>();
    private String defaultState;
    private FirstPersonBindingBuilder firstPerson;
    private ThirdPersonBindingBuilder thirdPerson;
    private boolean registered;

    public RegUseAnimationBuilder(Identifier profileId) {
        this.profileId = Objects.requireNonNull(profileId, "profileId");
    }

    /**
     * Selects a rig from the Shadows & Petals namespace.
     */
    public RegUseAnimationBuilder rig(String path) {
        return rig(ShadowsAndPetals.asResource(path));
    }

    /**
     * Selects the rig used by every binding in this profile.
     */
    public RegUseAnimationBuilder rig(Identifier id) {
        this.rig = new AnimationResourceRef.Rig(Objects.requireNonNull(id, "id"));
        return this;
    }

    /**
     * Selects a controller from the Shadows & Petals namespace.
     */
    public RegUseAnimationBuilder controller(String path) {
        return controller(ShadowsAndPetals.asResource(path));
    }

    /**
     * Selects the controller that owns this profile's states.
     */
    public RegUseAnimationBuilder controller(Identifier id) {
        this.controller = new AnimationResourceRef.Controller(
                Objects.requireNonNull(id, "id"));
        return this;
    }

    /**
     * Adds an entity-animation clip from the Shadows & Petals namespace.
     */
    public RegUseAnimationBuilder clip(String path) {
        return clip(ShadowsAndPetals.asResource(path));
    }

    /**
     * Adds an entity-animation clip required by this profile.
     */
    public RegUseAnimationBuilder clip(Identifier id) {
        clips.add(new AnimationResourceRef.Clip(Objects.requireNonNull(id, "id")));
        return this;
    }

    /**
     * Selects the controller state used when callers do not choose another state.
     */
    public RegUseAnimationBuilder defaultState(String name) {
        this.defaultState = requireName(name, "default state");
        return this;
    }

    /**
     * Enables first-person rendering with the conventional right/left item
     * sockets and mirroring to the active use arm.
     */
    public RegUseAnimationBuilder firstPerson() {
        return firstPerson(binding -> {
        });
    }

    /**
     * Configures first-person item-socket bindings.
     *
     * <p>If no sockets are supplied, {@code first_person_right_item} and
     * {@code first_person_left_item} are used.</p>
     */
    public RegUseAnimationBuilder firstPerson(
            Consumer<FirstPersonBindingBuilder> configurator
    ) {
        Objects.requireNonNull(configurator, "configurator");
        FirstPersonBindingBuilder binding = new FirstPersonBindingBuilder();
        configurator.accept(binding);
        this.firstPerson = binding;
        return this;
    }

    /**
     * Enables third-person rendering with the conventional right/left arm
     * bones, active-arm mirroring and replacement rotation.
     */
    public RegUseAnimationBuilder thirdPerson() {
        return thirdPerson(binding -> {
        });
    }

    /**
     * Configures third-person humanoid-model bindings.
     *
     * <p>If no bones are supplied, {@code right_arm} and {@code left_arm}
     * are bound to their matching humanoid arms.</p>
     */
    public RegUseAnimationBuilder thirdPerson(
            Consumer<ThirdPersonBindingBuilder> configurator
    ) {
        Objects.requireNonNull(configurator, "configurator");
        ThirdPersonBindingBuilder binding = new ThirdPersonBindingBuilder();
        configurator.accept(binding);
        this.thirdPerson = binding;
        return this;
    }

    /**
     * Builds, validates and registers this use-animation profile.
     */
    public UseAnimationProfile register() {
        if (registered) {
            throw new IllegalStateException(
                    "Use-animation builder has already registered " + profileId);
        }

        Identifier conventionalResourceId = conventionalResourceId();
        AnimationResourceRef.Rig resolvedRig = rig != null
                ? rig
                : new AnimationResourceRef.Rig(conventionalResourceId);
        AnimationResourceRef.Controller resolvedController = controller != null
                ? controller
                : new AnimationResourceRef.Controller(conventionalResourceId);
        Set<AnimationResourceRef.Clip> resolvedClips = clips.isEmpty()
                ? Set.of(new AnimationResourceRef.Clip(conventionalResourceId))
                : Set.copyOf(clips);
        String resolvedDefaultState = defaultState != null
                ? defaultState
                : conventionalResourceId.getPath();
        if (firstPerson == null && thirdPerson == null) {
            throw new IllegalStateException(
                    "Use-animation profile " + profileId
                            + " needs a first- or third-person binding");
        }

        UseAnimationProfile profile = new UseAnimationProfile(
                profileId,
                resolvedRig,
                resolvedController,
                resolvedClips,
                new AnimationResourceRef.State(
                        resolvedController, resolvedDefaultState),
                firstPerson == null ? null : firstPerson.build(resolvedRig),
                thirdPerson == null ? null : thirdPerson.build(resolvedRig));
        UseAnimationProfile registeredProfile = SAPAnimationRegistry.register(profile);
        registered = true;
        return registeredProfile;
    }

    /**
     * Builder for a profile's first-person item sockets.
     */
    public static final class FirstPersonBindingBuilder {
        private HumanoidArm authoredArm = HumanoidArm.RIGHT;
        private UseAnimationProfile.MirrorPolicy mirrorPolicy =
                UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM;
        private final Map<HumanoidArm, String> itemSockets =
                new EnumMap<>(HumanoidArm.class);

        public FirstPersonBindingBuilder authoredArm(HumanoidArm arm) {
            this.authoredArm = Objects.requireNonNull(arm, "arm");
            return this;
        }

        public FirstPersonBindingBuilder mirrorPolicy(
                UseAnimationProfile.MirrorPolicy mirrorPolicy
        ) {
            this.mirrorPolicy = Objects.requireNonNull(mirrorPolicy, "mirrorPolicy");
            return this;
        }

        public FirstPersonBindingBuilder mirrorToUseArm() {
            return mirrorPolicy(UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM);
        }

        public FirstPersonBindingBuilder itemSocket(
                HumanoidArm arm,
                String socketName
        ) {
            itemSockets.put(
                    Objects.requireNonNull(arm, "arm"),
                    requireName(socketName, "item socket"));
            return this;
        }

        private UseAnimationProfile.FirstPersonBinding build(
                AnimationResourceRef.Rig rig
        ) {
            if (itemSockets.isEmpty()) {
                itemSockets.put(HumanoidArm.RIGHT, "first_person_right_item");
                itemSockets.put(HumanoidArm.LEFT, "first_person_left_item");
            }
            Map<HumanoidArm, AnimationResourceRef.Socket> resolvedSockets =
                    new EnumMap<>(HumanoidArm.class);
            itemSockets.forEach((arm, name) -> resolvedSockets.put(
                    arm, new AnimationResourceRef.Socket(rig, name)));
            return new UseAnimationProfile.FirstPersonBinding(
                    authoredArm,
                    mirrorPolicy,
                    resolvedSockets);
        }
    }

    /**
     * Builder for a profile's third-person humanoid-model bones.
     */
    public static final class ThirdPersonBindingBuilder {
        private HumanoidArm authoredArm = HumanoidArm.RIGHT;
        private UseAnimationProfile.MirrorPolicy mirrorPolicy =
                UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM;
        private ModelPartRigBinder.RotationMode rotationMode =
                ModelPartRigBinder.RotationMode.REPLACE;
        private final Map<UseAnimationProfile.HumanoidBone, String> bones =
                new EnumMap<>(UseAnimationProfile.HumanoidBone.class);

        public ThirdPersonBindingBuilder authoredArm(HumanoidArm arm) {
            this.authoredArm = Objects.requireNonNull(arm, "arm");
            return this;
        }

        public ThirdPersonBindingBuilder mirrorPolicy(
                UseAnimationProfile.MirrorPolicy mirrorPolicy
        ) {
            this.mirrorPolicy = Objects.requireNonNull(mirrorPolicy, "mirrorPolicy");
            return this;
        }

        public ThirdPersonBindingBuilder mirrorToUseArm() {
            return mirrorPolicy(UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM);
        }

        public ThirdPersonBindingBuilder rotationMode(
                ModelPartRigBinder.RotationMode rotationMode
        ) {
            this.rotationMode = Objects.requireNonNull(rotationMode, "rotationMode");
            return this;
        }

        public ThirdPersonBindingBuilder bone(
                UseAnimationProfile.HumanoidBone humanoidBone,
                String rigBoneName
        ) {
            bones.put(
                    Objects.requireNonNull(humanoidBone, "humanoidBone"),
                    requireName(rigBoneName, "rig bone"));
            return this;
        }

        private UseAnimationProfile.ThirdPersonBinding build(
                AnimationResourceRef.Rig rig
        ) {
            if (bones.isEmpty()) {
                bones.put(UseAnimationProfile.HumanoidBone.RIGHT_ARM, "right_arm");
                bones.put(UseAnimationProfile.HumanoidBone.LEFT_ARM, "left_arm");
            }
            Map<UseAnimationProfile.HumanoidBone, AnimationResourceRef.Bone>
                    resolvedBones =
                    new EnumMap<>(UseAnimationProfile.HumanoidBone.class);
            bones.forEach((bone, name) -> resolvedBones.put(
                    bone, new AnimationResourceRef.Bone(rig, name)));
            return new UseAnimationProfile.ThirdPersonBinding(
                    authoredArm,
                    mirrorPolicy,
                    rotationMode,
                    resolvedBones);
        }
    }

    private Identifier conventionalResourceId() {
        return Identifier.fromNamespaceAndPath(
                profileId.getNamespace(),
                "animation/" + profileId.getPath());
    }

    private static String requireName(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Animation " + description + " name cannot be blank");
        }
        return name;
    }
}
