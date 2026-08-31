package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimatedBlockModelTest {
    private static final AnimationResourceRef.Rig RIG = new AnimationResourceRef.Rig(
            Identifier.fromNamespaceAndPath("test", "rig"));
    private static final AnimationResourceRef.Rig OTHER_RIG = new AnimationResourceRef.Rig(
            Identifier.fromNamespaceAndPath("test", "other_rig"));

    @Test
    void bindingCopiesMutableTintArray() {
        int[] tints = {0xFFFFFFFF};
        AnimatedBlockModel.Binding binding = new AnimatedBlockModel.Binding(
                RIG, "body", List.of(), false, tints);

        tints[0] = 0;

        assertArrayEquals(new int[]{0xFFFFFFFF}, binding.tints());
    }

    @Test
    void modelRejectsBindingFromAnotherRig() {
        AnimatedBlockModel.Binding binding = new AnimatedBlockModel.Binding(
                OTHER_RIG, "body", List.of(), false, new int[0]);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AnimatedBlockModel(RIG, List.of(binding)));

        assertEquals("Binding body belongs to rig test:other_rig, expected test:rig", exception.getMessage());
    }
}
