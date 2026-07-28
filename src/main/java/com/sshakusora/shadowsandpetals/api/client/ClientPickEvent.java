package com.sshakusora.shadowsandpetals.api.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.Event;

import java.util.Objects;

/**
 * Fired after {@link LocalPlayer#raycastHitResult(float, Entity)} computes a client-side hit result.
 *
 * <p>Listeners may replace {@link #getHitResult() the current result}. When Minecraft performs its
 * normal crosshair pick, the final result is used for block outlines, interaction, and other systems
 * that consume the crosshair target. Each listener should use the current result as its input so
 * changes made by earlier listeners are preserved.
 *
 * <p>This event is client-side only and is fired frequently. Listeners should avoid unnecessary
 * world scans and must not call {@link LocalPlayer#raycastHitResult(float, Entity)} recursively.
 * The event is not cancellable; leaving the result unchanged preserves vanilla behavior.
 */
public final class ClientPickEvent extends Event {
    private final LocalPlayer player;
    private final Entity cameraEntity;
    private final float partialTick;
    private final HitResult originalHitResult;
    private HitResult hitResult;

    public ClientPickEvent(
            LocalPlayer player,
            Entity cameraEntity,
            float partialTick,
            HitResult originalHitResult
    ) {
        this.player = Objects.requireNonNull(player);
        this.cameraEntity = Objects.requireNonNull(cameraEntity);
        this.partialTick = partialTick;
        this.originalHitResult = Objects.requireNonNull(originalHitResult);
        this.hitResult = originalHitResult;
    }

    public LocalPlayer getPlayer() {
        return player;
    }

    public Entity getCameraEntity() {
        return cameraEntity;
    }

    public float getPartialTick() {
        return partialTick;
    }

    /**
     * Returns the unmodified result produced by vanilla.
     */
    public HitResult getOriginalHitResult() {
        return originalHitResult;
    }

    /**
     * Returns the result after any earlier event listeners have modified it.
     */
    public HitResult getHitResult() {
        return hitResult;
    }

    /**
     * Replaces the client crosshair target.
     *
     * @param hitResult the non-null result to use
     */
    public void setHitResult(HitResult hitResult) {
        this.hitResult = Objects.requireNonNull(hitResult);
    }
}
