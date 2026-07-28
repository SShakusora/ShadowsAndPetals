package com.sshakusora.shadowsandpetals.mixin;

import com.sshakusora.shadowsandpetals.api.client.ClientPickEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "raycastHitResult", at = @At("RETURN"), cancellable = true)
    private void shadowsandpetals$fireClientPickEvent(
            float partialTick,
            Entity cameraEntity,
            CallbackInfoReturnable<HitResult> callback
    ) {
        HitResult original = callback.getReturnValue();
        ClientPickEvent event = new ClientPickEvent(
                (LocalPlayer) (Object) this,
                cameraEntity,
                partialTick,
                original
        );
        NeoForge.EVENT_BUS.post(event);

        if (event.getHitResult() != original) {
            callback.setReturnValue(event.getHitResult());
        }
    }
}
