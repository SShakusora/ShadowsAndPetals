package com.sshakusora.shadowsandpetals.client.animation.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.animation.AnimationResourceRef;
import com.sshakusora.shadowsandpetals.client.animation.BlockAnimationDefinition;
import com.sshakusora.shadowsandpetals.client.animation.SAPAnimationRegistry;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Fluent registration builder for resource-driven block-entity animations. */
public final class RegBlockAnimationBuilder {
    private final Identifier animationId;
    private AnimationResourceRef.Rig rig;
    private AnimationResourceRef.Controller controller;
    private final Set<AnimationResourceRef.Clip> clips = new LinkedHashSet<>();
    private String defaultState;
    private boolean registered;

    public RegBlockAnimationBuilder(Identifier animationId) {
        this.animationId = Objects.requireNonNull(animationId, "animationId");
    }

    public RegBlockAnimationBuilder rig(String path) {
        return rig(ShadowsAndPetals.asResource(path));
    }

    public RegBlockAnimationBuilder rig(Identifier id) {
        this.rig = new AnimationResourceRef.Rig(Objects.requireNonNull(id, "id"));
        return this;
    }

    public RegBlockAnimationBuilder controller(String path) {
        return controller(ShadowsAndPetals.asResource(path));
    }

    public RegBlockAnimationBuilder controller(Identifier id) {
        this.controller = new AnimationResourceRef.Controller(Objects.requireNonNull(id, "id"));
        return this;
    }

    public RegBlockAnimationBuilder clip(String path) {
        return clip(ShadowsAndPetals.asResource(path));
    }

    public RegBlockAnimationBuilder clip(Identifier id) {
        clips.add(new AnimationResourceRef.Clip(Objects.requireNonNull(id, "id")));
        return this;
    }

    public RegBlockAnimationBuilder defaultState(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Block animation default state cannot be blank");
        }
        defaultState = name;
        return this;
    }

    public BlockAnimationDefinition register() {
        if (registered) {
            throw new IllegalStateException("Block animation builder has already registered " + animationId);
        }
        Identifier conventionalId = Identifier.fromNamespaceAndPath(
                animationId.getNamespace(), "animation/" + animationId.getPath());
        AnimationResourceRef.Rig resolvedRig = rig != null
                ? rig
                : new AnimationResourceRef.Rig(conventionalId);
        AnimationResourceRef.Controller resolvedController = controller != null
                ? controller
                : new AnimationResourceRef.Controller(conventionalId);
        Set<AnimationResourceRef.Clip> resolvedClips = clips.isEmpty()
                ? Set.of(new AnimationResourceRef.Clip(conventionalId))
                : Set.copyOf(clips);
        String resolvedState = defaultState != null ? defaultState : conventionalId.getPath();
        BlockAnimationDefinition definition = new BlockAnimationDefinition(
                animationId,
                resolvedRig,
                resolvedController,
                resolvedClips,
                new AnimationResourceRef.State(resolvedController, resolvedState)
        );
        BlockAnimationDefinition result = SAPAnimationRegistry.register(definition);
        registered = true;
        return result;
    }
}
