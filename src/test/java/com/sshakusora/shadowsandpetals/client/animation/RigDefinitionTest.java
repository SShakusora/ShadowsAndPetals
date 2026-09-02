package com.sshakusora.shadowsandpetals.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RigDefinitionTest {
    private static final Identifier RIG_ID = Identifier.fromNamespaceAndPath(
            "test", "animation/rig");

    @Test
    void childBindingAppliesTheCompleteParentChain() {
        RigDefinition rig = RigDefinition.create(
                RIG_ID,
                List.of(
                        new RigDefinition.BoneSpec(
                                "root", null, new Vector3f(),
                                new BoneTransform(new Vector3f(16.0F, 0.0F, 0.0F),
                                        new Vector3f(), new Vector3f(1.0F))),
                        new RigDefinition.BoneSpec(
                                "child", "root", new Vector3f(8.0F, 16.0F, 8.0F),
                                new BoneTransform())));

        assertArrayEquals(new int[]{0, 1}, rig.chainTo(1));

        PoseStack poseStack = new PoseStack();
        PoseStackRigBinder.apply(poseStack, rig.restPose(), "child");
        Vector3f translation = poseStack.last().pose().getTranslation(new Vector3f());
        assertEquals(1.0F, translation.x, 1.0e-5F);
        assertEquals(0.0F, translation.y, 1.0e-5F);
        assertEquals(0.0F, translation.z, 1.0e-5F);
    }

    @Test
    void bindingRejectsABoneThatIsNotInTheRig() {
        RigDefinition rig = RigDefinition.create(
                RIG_ID,
                List.of(new RigDefinition.BoneSpec(
                        "root", null, new Vector3f(), new BoneTransform())));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PoseStackRigBinder.apply(new PoseStack(), rig.restPose(), "missing"));

        assertEquals("Rig test:animation/rig has no bone missing", exception.getMessage());
    }
}
