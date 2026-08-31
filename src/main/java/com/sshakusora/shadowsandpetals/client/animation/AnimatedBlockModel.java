package com.sshakusora.shadowsandpetals.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Submits baked block-model parts using a SAP rig pose.
 *
 * <p>Vanilla block models are baked into independent quad lists and therefore
 * do not retain Blockbench's outliner hierarchy.  A binding explicitly names
 * the rig bone that owns each baked part.  The binder applies that bone's full
 * parent chain once per submitted part, which keeps the geometry compatible
 * with the existing {@code submitMultiLayerBlockModel} pipeline.</p>
 */
public final class AnimatedBlockModel {
    private final AnimationResourceRef.Rig rig;
    private final List<Binding> bindings;

    public AnimatedBlockModel(
            AnimationResourceRef.Rig rig,
            Collection<Binding> bindings
    ) {
        this.rig = Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(bindings, "bindings");
        List<Binding> copied = new ArrayList<>(bindings.size());
        for (Binding binding : bindings) {
            Objects.requireNonNull(binding, "binding");
            if (!binding.rig().equals(rig)) {
                throw new IllegalArgumentException(
                        "Binding " + binding.boneName() + " belongs to rig "
                                + binding.rig().id() + ", expected " + rig.id());
            }
            copied.add(binding);
        }
        this.bindings = List.copyOf(copied);
    }

    public AnimationResourceRef.Rig rig() {
        return rig;
    }

    public List<Binding> bindings() {
        return bindings;
    }

    /**
     * Submits every bound model part with its authored bone transform.
     *
     * <p>The caller may keep a surrounding pose for block orientation or a
     * local attachment offset.  This method owns all animation-related
     * push/pop operations.</p>
     */
    public void submit(
            RigPose pose,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlay,
            int order
    ) {
        Objects.requireNonNull(pose, "pose");
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(collector, "collector");
        if (!pose.rig().id().equals(rig.id())) {
            throw new IllegalArgumentException(
                    "Animated block model expects rig " + rig.id()
                            + ", got " + pose.rig().id());
        }

        for (Binding binding : bindings) {
            if (binding.parts().isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            PoseStackRigBinder.apply(poseStack, pose, binding.boneName(), binding.mirrorX());
            collector.submitMultiLayerBlockModel(
                    poseStack,
                    binding.parts(),
                    binding.hasTranslucency(),
                    binding.tintValues(),
                    lightCoords,
                    overlay,
                    order
            );
            poseStack.popPose();
        }
    }

    public void submit(
            RigPose pose,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords
    ) {
        submit(pose, poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }

    /**
     * A baked model group owned by one rig bone.
     */
    public record Binding(
            AnimationResourceRef.Rig rig,
            String boneName,
            List<BlockStateModelPart> parts,
            boolean hasTranslucency,
            int[] tints,
            boolean mirrorX
    ) {
        public Binding {
            Objects.requireNonNull(rig, "rig");
            Objects.requireNonNull(boneName, "boneName");
            if (boneName.isBlank()) {
                throw new IllegalArgumentException("Animated block binding bone cannot be blank");
            }
            Objects.requireNonNull(parts, "parts");
            List<BlockStateModelPart> copiedParts = new ArrayList<>(parts.size());
            for (BlockStateModelPart part : parts) {
                copiedParts.add(Objects.requireNonNull(part, "part"));
            }
            parts = List.copyOf(copiedParts);
            Objects.requireNonNull(tints, "tints");
            tints = tints.clone();
        }

        public Binding(
                AnimationResourceRef.Rig rig,
                String boneName,
                List<BlockStateModelPart> parts,
                boolean hasTranslucency,
                int[] tints
        ) {
            this(rig, boneName, parts, hasTranslucency, tints, false);
        }

        @Override
        public int[] tints() {
            return tints.clone();
        }

        private int[] tintValues() {
            return tints;
        }
    }
}
