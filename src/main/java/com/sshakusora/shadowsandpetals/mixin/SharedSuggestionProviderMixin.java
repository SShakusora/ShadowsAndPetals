package com.sshakusora.shadowsandpetals.mixin;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sshakusora.shadowsandpetals.legacy.LegacyCompatIds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(SharedSuggestionProvider.class)
public interface SharedSuggestionProviderMixin {
    @Inject(method = "suggestResource(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"), cancellable = true)
    private static void shadowsandpetals$filterLegacyCompatIds(
            Iterable<ResourceLocation> resources,
            SuggestionsBuilder builder,
            CallbackInfoReturnable<CompletableFuture<?>> cir
    ) {
        List<ResourceLocation> filtered = new ArrayList<>();
        boolean changed = false;

        for (ResourceLocation resource : resources) {
            if (LegacyCompatIds.shouldHideFromSuggestions(resource)) {
                changed = true;
                continue;
            }
            filtered.add(resource);
        }

        if (changed) {
            cir.setReturnValue(SharedSuggestionProvider.suggestResource(filtered, builder));
        }
    }
}
