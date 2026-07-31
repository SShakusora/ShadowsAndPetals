package com.sshakusora.shadowsandpetals.world.excavation;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.excavation.SandExcavationDataMaps;
import com.sshakusora.shadowsandpetals.api.excavation.SandExcavationDropCategory;
import com.sshakusora.shadowsandpetals.api.excavation.SandExcavationDropData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class SandExcavationLootPool {
    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private SandExcavationLootPool() {
    }

    public static SandExcavationResult roll(ServerLevel level) {
        SandExcavationResult.Category category = SandExcavationChanceRules.rollCategory(level);
        if (category == SandExcavationResult.Category.EMPTY) {
            return SandExcavationResult.empty();
        }

        WeightedPool<Drop> pool = category == SandExcavationResult.Category.SEAFOOD
                ? snapshot.seafood()
                : snapshot.trash();
        return pool.getRandom(level.getRandom())
                .map(drop -> new SandExcavationResult(
                        category,
                        drop.data().createStack(drop.item(), level.getRandom())
                ))
                .orElseGet(SandExcavationResult::empty);
    }

    @SubscribeEvent
    public static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
        event.ifRegistry(Registries.ITEM, SandExcavationLootPool::rebuild);
    }

    private static void rebuild(Registry<Item> itemRegistry) {
        List<WeightedPool.Entry<Drop>> seafood = new ArrayList<>();
        List<WeightedPool.Entry<Drop>> trash = new ArrayList<>();
        itemRegistry.getDataMap(SandExcavationDataMaps.DROPS).forEach((key, data) -> {
            Item item = itemRegistry.getValueOrThrow(key);
            WeightedPool.Entry<Drop> entry = new WeightedPool.Entry<>(new Drop(item, data), data.weight());
            if (data.category() == SandExcavationDropCategory.SEAFOOD) {
                seafood.add(entry);
            } else {
                trash.add(entry);
            }
        });
        snapshot = new Snapshot(WeightedPool.of(seafood), WeightedPool.of(trash));
    }

    private record Drop(Item item, SandExcavationDropData data) {
    }

    private record Snapshot(WeightedPool<Drop> seafood, WeightedPool<Drop> trash) {
        private static final Snapshot EMPTY = new Snapshot(WeightedPool.empty(), WeightedPool.empty());
    }
}
