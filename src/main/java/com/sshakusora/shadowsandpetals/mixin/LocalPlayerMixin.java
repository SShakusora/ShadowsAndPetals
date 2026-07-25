package com.sshakusora.shadowsandpetals.mixin;

import com.sshakusora.shadowsandpetals.client.interaction.RecessedLampTargeting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "raycastHitResult", at = @At("RETURN"), cancellable = true)
    private void shadowsandpetals$correctRecessedLampTarget(
            float a,
            Entity cameraEntity,
            CallbackInfoReturnable<HitResult> callback
    ) {
        HitResult original = callback.getReturnValue();
        HitResult corrected = RecessedLampTargeting.correct(original, cameraEntity, a);
        if (corrected != original) {
            callback.setReturnValue(corrected);
        }
    }
}
