package com.sshakusora.shadowsandpetals.client.animation;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Resource references for an animation controller used by a block entity.
 *
 * <p>The controller and clips remain ordinary NeoForge entity-animation
 * resources.  This type only records how a BER intends to use them; the
 * geometry binding is supplied by {@link AnimatedBlockModel} after the
 * standalone block models have been baked.</p>
 */
public record BlockAnimationDefinition(
        Identifier id,
        AnimationResourceRef.Rig rig,
        AnimationResourceRef.Controller controller,
        Set<AnimationResourceRef.Clip> clips,
        AnimationResourceRef.State defaultState
) {
    public BlockAnimationDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(clips, "clips");
        Objects.requireNonNull(defaultState, "defaultState");
        if (!defaultState.controller().equals(controller)) {
            throw new IllegalArgumentException(
                    "Block animation default state belongs to controller "
                            + defaultState.controller().id()
                            + ", expected " + controller.id());
        }
        clips = Set.copyOf(new LinkedHashSet<>(clips));
    }

    public void requireClip(AnimationResourceRef.Clip clip) {
        if (!clips.contains(Objects.requireNonNull(clip, "clip"))) {
            throw new IllegalArgumentException(
                    "Block animation " + id + " does not register clip " + clip.id());
        }
    }
}
