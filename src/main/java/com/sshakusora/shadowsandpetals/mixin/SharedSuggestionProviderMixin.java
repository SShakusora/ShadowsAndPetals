package com.sshakusora.shadowsandpetals.mixin;

import com.sshakusora.shadowsandpetals.legacy.LegacyCompatIds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(SharedSuggestionProvider.class)
public interface SharedSuggestionProviderMixin {
    @ModifyVariable(method = "suggestResource(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"), argsOnly = true)
    private static Iterable<Identifier> shadowsandpetals$filterLegacyCompatIds(Iterable<Identifier> resources) {
        List<Identifier> filtered = new ArrayList<>();

        for (Identifier resource : resources) {
            if (!LegacyCompatIds.shouldHideFromSuggestions(resource)) {
                filtered.add(resource);
            }
        }

        return filtered;
    }
}
