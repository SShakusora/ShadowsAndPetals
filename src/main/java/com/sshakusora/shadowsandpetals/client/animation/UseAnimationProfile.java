package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * A reusable resource-driven use-animation profile. The profile contains only
 * rendering resources and bindings; item code remains responsible for trigger
 * rules, duration, state selection and local animation time.
 */
public record UseAnimationProfile(
        Identifier id,
        AnimationResourceRef.Rig rig,
        AnimationResourceRef.Controller controller,
        Set<AnimationResourceRef.Clip> clips,
        AnimationResourceRef.State defaultState,
        @Nullable FirstPersonBinding firstPerson,
        @Nullable ThirdPersonBinding thirdPerson
) {
    public UseAnimationProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(clips, "clips");
        Objects.requireNonNull(defaultState, "defaultState");
        clips = Set.copyOf(clips);
        if (clips.isEmpty()) {
            throw new IllegalArgumentException("Use-animation profile " + id + " has no clips");
        }
        if (!defaultState.controller().equals(controller)) {
            throw new IllegalArgumentException(
                    "Use-animation profile " + id
                            + " uses a default state from another controller");
        }
        if (firstPerson == null && thirdPerson == null) {
            throw new IllegalArgumentException(
                    "Use-animation profile " + id
                            + " needs a first- or third-person binding");
        }
        if (firstPerson != null && !firstPerson.rig().equals(rig)) {
            throw new IllegalArgumentException(
                    "First-person binding for " + id + " uses another rig");
        }
        if (thirdPerson != null && !thirdPerson.rig().equals(rig)) {
            throw new IllegalArgumentException(
                    "Third-person binding for " + id + " uses another rig");
        }
    }

    public Set<SAPAnimationRegistry.Target> targets() {
        EnumSet<SAPAnimationRegistry.Target> targets =
                EnumSet.noneOf(SAPAnimationRegistry.Target.class);
        if (firstPerson != null) {
            targets.add(SAPAnimationRegistry.Target.FIRST_PERSON);
        }
        if (thirdPerson != null) {
            targets.add(SAPAnimationRegistry.Target.THIRD_PERSON);
        }
        return Set.copyOf(targets);
    }

    public void requireState(AnimationResourceRef.State state) {
        Objects.requireNonNull(state, "state");
        if (!state.controller().equals(controller)) {
            throw new IllegalArgumentException(
                    "Animation state belongs to controller " + state.controller().id()
                            + ", expected " + controller.id());
        }
    }

    public record FirstPersonBinding(
            HumanoidArm authoredUseArm,
            MirrorPolicy mirrorPolicy,
            Map<HumanoidArm, AnimationResourceRef.Socket> itemSockets
    ) {
        public FirstPersonBinding {
            Objects.requireNonNull(authoredUseArm, "authoredUseArm");
            Objects.requireNonNull(mirrorPolicy, "mirrorPolicy");
            itemSockets = immutableEnumMap(itemSockets, HumanoidArm.class);
            if (itemSockets.isEmpty()) {
                throw new IllegalArgumentException(
                        "A first-person binding needs at least one item socket");
            }
            requireSingleRig(itemSockets.values(), "First-person item sockets");
        }

        public AnimationResourceRef.Rig rig() {
            return itemSockets.values().iterator().next().rig();
        }

        public @Nullable ResolvedSocket resolve(
                HumanoidArm renderedArm,
                HumanoidArm actualUseArm
        ) {
            Objects.requireNonNull(renderedArm, "renderedArm");
            boolean mirrorX = mirrorPolicy.shouldMirror(authoredUseArm, actualUseArm);
            HumanoidArm authoredArm = mirrorX ? renderedArm.getOpposite() : renderedArm;
            AnimationResourceRef.Socket socket = itemSockets.get(authoredArm);
            return socket == null ? null : new ResolvedSocket(socket, mirrorX);
        }
    }

    public record ThirdPersonBinding(
            HumanoidArm authoredUseArm,
            MirrorPolicy mirrorPolicy,
            ModelPartRigBinder.RotationMode rotationMode,
            Map<HumanoidBone, AnimationResourceRef.Bone> bones
    ) {
        public ThirdPersonBinding {
            Objects.requireNonNull(authoredUseArm, "authoredUseArm");
            Objects.requireNonNull(mirrorPolicy, "mirrorPolicy");
            Objects.requireNonNull(rotationMode, "rotationMode");
            bones = immutableEnumMap(bones, HumanoidBone.class);
            if (bones.isEmpty()) {
                throw new IllegalArgumentException(
                        "A third-person binding needs at least one humanoid bone");
            }
            requireSingleRig(bones.values(), "Third-person humanoid bones");
        }

        public AnimationResourceRef.Rig rig() {
            return bones.values().iterator().next().rig();
        }

        public @Nullable ResolvedBone resolve(
                HumanoidBone renderedBone,
                HumanoidArm actualUseArm
        ) {
            Objects.requireNonNull(renderedBone, "renderedBone");
            boolean mirrorX = mirrorPolicy.shouldMirror(authoredUseArm, actualUseArm);
            HumanoidBone authoredBone = mirrorX ? renderedBone.mirrored() : renderedBone;
            AnimationResourceRef.Bone bone = bones.get(authoredBone);
            return bone == null ? null : new ResolvedBone(bone, mirrorX);
        }
    }

    public enum MirrorPolicy {
        NONE {
            @Override
            boolean shouldMirror(HumanoidArm authoredUseArm, HumanoidArm actualUseArm) {
                return false;
            }
        },
        MIRROR_TO_USE_ARM {
            @Override
            boolean shouldMirror(HumanoidArm authoredUseArm, HumanoidArm actualUseArm) {
                return authoredUseArm != actualUseArm;
            }
        };

        abstract boolean shouldMirror(HumanoidArm authoredUseArm, HumanoidArm actualUseArm);
    }

    public enum HumanoidBone {
        ROOT,
        BODY,
        HEAD,
        RIGHT_ARM,
        LEFT_ARM,
        RIGHT_LEG,
        LEFT_LEG;

        public HumanoidBone mirrored() {
            return switch (this) {
                case RIGHT_ARM -> LEFT_ARM;
                case LEFT_ARM -> RIGHT_ARM;
                case RIGHT_LEG -> LEFT_LEG;
                case LEFT_LEG -> RIGHT_LEG;
                default -> this;
            };
        }
    }

    public record ResolvedSocket(
            AnimationResourceRef.Socket socket,
            boolean mirrorX
    ) {
    }

    public record ResolvedBone(
            AnimationResourceRef.Bone bone,
            boolean mirrorX
    ) {
    }

    private static <K extends Enum<K>, V> Map<K, V> immutableEnumMap(
            Map<K, V> values,
            Class<K> keyType
    ) {
        Objects.requireNonNull(values, "values");
        EnumMap<K, V> copy = new EnumMap<>(keyType);
        values.forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, "binding key"),
                Objects.requireNonNull(value, "binding value")));
        return Collections.unmodifiableMap(copy);
    }

    private static void requireSingleRig(
            Iterable<?> references,
            String description
    ) {
        AnimationResourceRef.Rig expected = null;
        for (Object reference : references) {
            AnimationResourceRef.Rig rig = switch (reference) {
                case AnimationResourceRef.Bone bone -> bone.rig();
                case AnimationResourceRef.Socket socket -> socket.rig();
                default -> throw new IllegalArgumentException(
                        description + " contains an unsupported reference");
            };
            if (expected == null) {
                expected = rig;
            } else if (!expected.equals(rig)) {
                throw new IllegalArgumentException(description + " must use one rig");
            }
        }
    }
}
