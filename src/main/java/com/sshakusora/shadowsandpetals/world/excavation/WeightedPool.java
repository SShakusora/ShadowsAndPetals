package com.sshakusora.shadowsandpetals.world.excavation;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class WeightedPool<T> {
    private final List<WeightedEntry<T>> entries;
    private final int totalWeight;

    private WeightedPool(List<Entry<T>> sourceEntries) {
        List<WeightedEntry<T>> entries = new ArrayList<>(sourceEntries.size());
        int cumulativeWeight = 0;
        for (Entry<T> entry : sourceEntries) {
            cumulativeWeight = Math.addExact(cumulativeWeight, entry.weight());
            entries.add(new WeightedEntry<>(entry.value(), cumulativeWeight));
        }
        this.entries = List.copyOf(entries);
        this.totalWeight = cumulativeWeight;
    }

    static <T> WeightedPool<T> of(List<Entry<T>> entries) {
        return new WeightedPool<>(entries);
    }

    static <T> WeightedPool<T> empty() {
        return new WeightedPool<>(List.of());
    }

    Optional<T> getRandom(RandomSource random) {
        if (totalWeight == 0) {
            return Optional.empty();
        }
        return getByRoll(random.nextInt(totalWeight));
    }

    Optional<T> getByRoll(int roll) {
        if (roll < 0 || roll >= totalWeight) {
            return Optional.empty();
        }
        for (WeightedEntry<T> entry : entries) {
            if (roll < entry.cumulativeWeight()) {
                return Optional.of(entry.value());
            }
        }
        return Optional.empty();
    }

    record Entry<T>(T value, int weight) {
        Entry {
            if (weight <= 0) {
                throw new IllegalArgumentException("weight must be positive");
            }
        }
    }

    private record WeightedEntry<T>(T value, int cumulativeWeight) {
    }
}
