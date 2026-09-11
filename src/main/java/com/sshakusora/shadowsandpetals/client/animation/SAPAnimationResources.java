package com.sshakusora.shadowsandpetals.client.animation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

public final class SAPAnimationResources implements PreparableReloadListener {
    public static final SAPAnimationResources INSTANCE = new SAPAnimationResources();
    private volatile Map<ResourceLocation, RigDefinition> rigs = Map.of();
    private volatile Map<ResourceLocation, AnimationControllerDefinition> controllers = Map.of();

    private SAPAnimationResources() {}

    public RigDefinition rig(ResourceLocation id) {
        RigDefinition value = rigs.get(id);
        if (value == null) throw new IllegalStateException("Animation rig is not loaded: " + id);
        return value;
    }
    public @Nullable RigDefinition findRig(ResourceLocation id) { return rigs.get(id); }
    public AnimationControllerDefinition controller(ResourceLocation id) {
        AnimationControllerDefinition value = controllers.get(id);
        if (value == null) throw new IllegalStateException("Animation controller is not loaded: " + id);
        return value;
    }
    public @Nullable AnimationControllerDefinition findController(ResourceLocation id) { return controllers.get(id); }
    public Set<ResourceLocation> rigIds() { return rigs.keySet(); }
    public Set<ResourceLocation> controllerIds() { return controllers.keySet(); }
    public Set<ResourceLocation> clipIds() { return Set.of(); }
    public float stateDurationSeconds(AnimationResourceRef.State state) { return 0.0F; }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
            ProfilerFiller preparationProfiler, ProfilerFiller applicationProfiler,
            Executor preparationExecutor, Executor applicationExecutor) {
        rigs = Map.of();
        controllers = Map.of();
        return CompletableFuture.completedFuture(null);
    }
}
