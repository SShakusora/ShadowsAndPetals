package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseAnimationProfileTest {
    private static final AnimationResourceRef.Rig RIG =
            new AnimationResourceRef.Rig(id("test_rig"));
    private static final AnimationResourceRef.Controller CONTROLLER =
            new AnimationResourceRef.Controller(id("test_controller"));
    private static final AnimationResourceRef.Clip CLIP =
            new AnimationResourceRef.Clip(id("test_clip"));
    private static final AnimationResourceRef.State STATE =
            new AnimationResourceRef.State(CONTROLLER, "use");
    private static final AnimationResourceRef.Socket RIGHT_SOCKET =
            new AnimationResourceRef.Socket(RIG, "first_person_right_item");
    private static final AnimationResourceRef.Socket LEFT_SOCKET =
            new AnimationResourceRef.Socket(RIG, "first_person_left_item");
    private static final AnimationResourceRef.Bone RIGHT_ARM =
            new AnimationResourceRef.Bone(RIG, "right_arm");
    private static final AnimationResourceRef.Bone LEFT_ARM =
            new AnimationResourceRef.Bone(RIG, "left_arm");

    @Test
    void mirrorsFirstPersonSocketsToTheActualUseArm() {
        var binding = new UseAnimationProfile.FirstPersonBinding(
                HumanoidArm.RIGHT,
                UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM,
                Map.of(
                        HumanoidArm.RIGHT, RIGHT_SOCKET,
                        HumanoidArm.LEFT, LEFT_SOCKET));

        var rightHanded = binding.resolve(HumanoidArm.RIGHT, HumanoidArm.RIGHT);
        assertNotNull(rightHanded);
        assertSame(RIGHT_SOCKET, rightHanded.socket());
        assertFalse(rightHanded.mirrorX());

        var mirroredMainHand = binding.resolve(HumanoidArm.LEFT, HumanoidArm.LEFT);
        assertNotNull(mirroredMainHand);
        assertSame(RIGHT_SOCKET, mirroredMainHand.socket());
        assertTrue(mirroredMainHand.mirrorX());

        var mirroredOffhand = binding.resolve(HumanoidArm.RIGHT, HumanoidArm.LEFT);
        assertNotNull(mirroredOffhand);
        assertSame(LEFT_SOCKET, mirroredOffhand.socket());
        assertTrue(mirroredOffhand.mirrorX());
    }

    @Test
    void permitsAOneSidedFirstPersonExport() {
        var binding = new UseAnimationProfile.FirstPersonBinding(
                HumanoidArm.RIGHT,
                UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM,
                Map.of(HumanoidArm.RIGHT, RIGHT_SOCKET));

        var mirrored = binding.resolve(HumanoidArm.LEFT, HumanoidArm.LEFT);
        assertNotNull(mirrored);
        assertSame(RIGHT_SOCKET, mirrored.socket());
        assertNull(binding.resolve(HumanoidArm.LEFT, HumanoidArm.RIGHT));
    }

    @Test
    void mirrorsOnlyDeclaredThirdPersonBones() {
        var binding = new UseAnimationProfile.ThirdPersonBinding(
                HumanoidArm.RIGHT,
                UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM,
                ModelPartRigBinder.RotationMode.REPLACE,
                Map.of(
                        UseAnimationProfile.HumanoidBone.RIGHT_ARM, RIGHT_ARM,
                        UseAnimationProfile.HumanoidBone.LEFT_ARM, LEFT_ARM));

        var mirrored = binding.resolve(
                UseAnimationProfile.HumanoidBone.LEFT_ARM,
                HumanoidArm.LEFT);
        assertNotNull(mirrored);
        assertSame(RIGHT_ARM, mirrored.bone());
        assertTrue(mirrored.mirrorX());
        assertNull(binding.resolve(
                UseAnimationProfile.HumanoidBone.HEAD,
                HumanoidArm.LEFT));
    }

    @Test
    void rejectsBindingsThatMixRigs() {
        var otherRig = new AnimationResourceRef.Rig(id("other_rig"));
        var otherSocket = new AnimationResourceRef.Socket(otherRig, "left_item");

        assertThrows(
                IllegalArgumentException.class,
                () -> new UseAnimationProfile.FirstPersonBinding(
                        HumanoidArm.RIGHT,
                        UseAnimationProfile.MirrorPolicy.NONE,
                        Map.of(
                                HumanoidArm.RIGHT, RIGHT_SOCKET,
                                HumanoidArm.LEFT, otherSocket)));
    }

    @Test
    void registersAllResourcesAndTargetsFromTheProfile() {
        var profile = profile(id("registered_use_profile"));

        assertSame(profile, SAPAnimationRegistry.register(profile));
        assertSame(
                profile,
                SAPAnimationRegistry.findProfile(profile.id()));

        var snapshot = SAPAnimationRegistry.snapshot();
        assertTrue(snapshot.profiles().contains(profile));
        assertTrue(snapshot.rigs().contains(RIG));
        assertTrue(snapshot.controllers().contains(CONTROLLER));
        assertTrue(snapshot.clips().contains(CLIP));
        var registration = snapshot.registrations().stream()
                .filter(value -> value.controller().equals(CONTROLLER))
                .findFirst()
                .orElseThrow();
        assertEquals(
                Set.of(
                        SAPAnimationRegistry.Target.FIRST_PERSON,
                        SAPAnimationRegistry.Target.THIRD_PERSON),
                registration.targets());
    }

    @Test
    void requiresAtLeastOnePerspectiveBinding() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UseAnimationProfile(
                        id("missing_bindings"),
                        RIG,
                        CONTROLLER,
                        Set.of(CLIP),
                        STATE,
                        null,
                        null));
    }

    private static UseAnimationProfile profile(Identifier id) {
        return new UseAnimationProfile(
                id,
                RIG,
                CONTROLLER,
                Set.of(CLIP),
                STATE,
                new UseAnimationProfile.FirstPersonBinding(
                        HumanoidArm.RIGHT,
                        UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM,
                        Map.of(
                                HumanoidArm.RIGHT, RIGHT_SOCKET,
                                HumanoidArm.LEFT, LEFT_SOCKET)),
                new UseAnimationProfile.ThirdPersonBinding(
                        HumanoidArm.RIGHT,
                        UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM,
                        ModelPartRigBinder.RotationMode.REPLACE,
                        Map.of(
                                UseAnimationProfile.HumanoidBone.RIGHT_ARM,
                                RIGHT_ARM,
                                UseAnimationProfile.HumanoidBone.LEFT_ARM,
                                LEFT_ARM)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("shadowsandpetals_test", path);
    }
}
