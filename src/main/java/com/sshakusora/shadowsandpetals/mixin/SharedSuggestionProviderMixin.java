package com.sshakusora.shadowsandpetals.mixin;

import com.sshakusora.shadowsandpetals.legacy.LegacyCompatIds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(SharedSuggestionProvider.class)
public interface SharedSuggestionProviderMixin {
    @ModifyVariable(method = "suggestResource(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"), argsOnly = true)
    private static Iterable<ResourceLocation> shadowsandpetals$filterLegacyCompatIds(Iterable<ResourceLocation> resources) {
        List<ResourceLocation> filtered = new ArrayList<>();

        for (ResourceLocation resource : resources) {
            if (!LegacyCompatIds.shouldHideFromSuggestions(resource)) {
                filtered.add(resource);
            }
        }

        return filtered;
    }
}
