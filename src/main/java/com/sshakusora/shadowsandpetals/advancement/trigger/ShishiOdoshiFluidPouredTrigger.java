package com.sshakusora.shadowsandpetals.advancement.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Triggered when a shishi-odoshi starts pouring a non-empty fluid. */
public final class ShishiOdoshiFluidPouredTrigger
        extends SimpleCriterionTrigger<ShishiOdoshiFluidPouredTrigger.TriggerInstance> {
    private static final double NEARBY_PLAYER_RANGE = 32.0D;

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void triggerNearby(ServerLevel level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double rangeSquared = NEARBY_PLAYER_RANGE * NEARBY_PLAYER_RANGE;

        for (ServerPlayer player : level.getPlayers(candidate ->
                candidate.distanceToSqr(centerX, centerY, centerZ) <= rangeSquared)) {
            trigger(player, ignored -> true);
        }
    }

    public Criterion<TriggerInstance> fluidPoured() {
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
