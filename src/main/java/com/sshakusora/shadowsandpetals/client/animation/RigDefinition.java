package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class RigDefinition {
    private final Identifier id;
    private final List<Bone> bones;
    private final Map<String, Integer> indices;
    private final int[][] chains;

    private RigDefinition(Identifier id, List<Bone> bones, Map<String, Integer> indices) {
        this.id = id;
        this.bones = List.copyOf(bones);
        this.indices = Map.copyOf(indices);
        this.chains = buildChains(bones);
    }

    public static RigDefinition create(Identifier id, List<BoneSpec> specs) {
        if (specs.isEmpty()) {
            throw new IllegalArgumentException("Rig " + id + " has no bones");
        }

        Map<String, Integer> indices = new HashMap<>();
        for (int index = 0; index < specs.size(); index++) {
            String name = specs.get(index).name();
            if (name.isBlank()) {
                throw new IllegalArgumentException("Rig " + id + " contains a blank bone name");
            }
            if (indices.putIfAbsent(name, index) != null) {
                throw new IllegalArgumentException("Rig " + id + " contains duplicate bone " + name);
            }
        }

        List<Bone> bones = new ArrayList<>(specs.size());
        for (BoneSpec spec : specs) {
            validateVector(id, spec.name(), "pivot", spec.pivot());
            validateVector(id, spec.name(), "translation", spec.restTransform().translation());
            validateVector(id, spec.name(), "rotation", spec.restTransform().rotation());
            validateVector(id, spec.name(), "scale", spec.restTransform().scale());
            if (spec.restTransform().scale().x == 0.0F
                    || spec.restTransform().scale().y == 0.0F
                    || spec.restTransform().scale().z == 0.0F) {
                throw new IllegalArgumentException(
                        "Rig " + id + " bone " + spec.name() + " has a zero rest scale");
            }
            int parentIndex = -1;
            if (spec.parent() != null) {
                Integer resolvedParent = indices.get(spec.parent());
                if (resolvedParent == null) {
                    throw new IllegalArgumentException(
                            "Rig " + id + " bone " + spec.name() + " references missing parent " + spec.parent());
                }
                parentIndex = resolvedParent;
            }
            bones.add(new Bone(spec.name(), parentIndex, spec.pivot(), spec.restTransform()));
        }

        validateAcyclic(id, bones);
        return new RigDefinition(id, bones, indices);
    }

    private static void validateVector(
            Identifier id,
            String boneName,
            String field,
            Vector3fc vector
    ) {
        if (!Float.isFinite(vector.x())
                || !Float.isFinite(vector.y())
                || !Float.isFinite(vector.z())) {
            throw new IllegalArgumentException(
                    "Rig " + id + " bone " + boneName + " has a non-finite " + field);
        }
    }

    public Identifier id() {
        return id;
    }

    public List<Bone> bones() {
        return bones;
    }

    public int boneCount() {
        return bones.size();
    }

    public int indexOf(String name) {
        return indices.getOrDefault(name, -1);
    }

    public Bone bone(int index) {
        return bones.get(index);
    }

    public int[] chainTo(int index) {
        return chains[index];
    }

    public RigPose restPose() {
        return RigPose.rest(this);
    }

    private static void validateAcyclic(Identifier id, List<Bone> bones) {
        for (int index = 0; index < bones.size(); index++) {
            Set<Integer> visited = new HashSet<>();
            int current = index;
            while (current >= 0) {
                if (!visited.add(current)) {
                    throw new IllegalArgumentException(
                            "Rig " + id + " contains a parent cycle at bone " + bones.get(index).name());
                }
                current = bones.get(current).parentIndex();
            }
        }
    }

    private static int[][] buildChains(List<Bone> bones) {
        int[][] result = new int[bones.size()][];
        for (int index = 0; index < bones.size(); index++) {
            List<Integer> reverse = new ArrayList<>();
            int current = index;
            while (current >= 0) {
                reverse.add(current);
                current = bones.get(current).parentIndex();
            }
            int[] chain = new int[reverse.size()];
            for (int chainIndex = 0; chainIndex < reverse.size(); chainIndex++) {
                chain[chainIndex] = reverse.get(reverse.size() - chainIndex - 1);
            }
            result[index] = chain;
        }
        return result;
    }

    public record Bone(String name, int parentIndex, Vector3f pivot, BoneTransform restTransform) {
        public Bone(String name, int parentIndex, Vector3fc pivot, BoneTransform restTransform) {
            this(name, parentIndex, new Vector3f(pivot), restTransform.copy());
        }
    }

    public record BoneSpec(
            String name,
            @Nullable String parent,
            Vector3f pivot,
            BoneTransform restTransform
    ) {
        public BoneSpec(String name, @Nullable String parent, Vector3fc pivot, BoneTransform restTransform) {
            this(name, parent, new Vector3f(pivot), restTransform.copy());
        }
    }
}
