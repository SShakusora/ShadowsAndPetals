package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Type-safe references declared by hand next to their registry entry. */
public final class AnimationResourceRef {
    private AnimationResourceRef() {
    }

    public record Rig(Identifier id) {
        public Rig {
            Objects.requireNonNull(id, "id");
        }
    }

    public record Controller(Identifier id) {
        public Controller {
            Objects.requireNonNull(id, "id");
        }
    }

    public record Clip(Identifier id) {
        public Clip {
            Objects.requireNonNull(id, "id");
        }
    }

    public record State(Controller controller, String name) {
        public State {
            Objects.requireNonNull(controller, "controller");
            requireName(name, "state");
        }
    }

    public record Bone(Rig rig, String name) {
        public Bone {
            Objects.requireNonNull(rig, "rig");
            requireName(name, "bone");
        }
    }

    public record Socket(Rig rig, String name) {
        public Socket {
            Objects.requireNonNull(rig, "rig");
            requireName(name, "socket");
        }
    }

    private static void requireName(String name, String type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Animation " + type + " name cannot be blank");
        }
    }
}
