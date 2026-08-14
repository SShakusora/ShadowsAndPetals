package com.sshakusora.shadowsandpetals.advancement.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Triggered when a player completes a 3x3 raw concrete surface. */
public final class RawConcrete3x3FormedTrigger
        extends SimpleCriterionTrigger<RawConcrete3x3FormedTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, ignored -> true);
    }

    public Criterion<TriggerInstance> rawConcrete3x3Formed() {
        return createCriterion(TriggerInstance.any());
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                        .forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));

        public static TriggerInstance any() {
            return new TriggerInstance(Optional.empty());
        }
    }
}
