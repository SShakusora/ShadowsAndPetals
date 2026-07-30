package com.sshakusora.shadowsandpetals.world.clam;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sshakusora.shadowsandpetals.registries.AttachmentRegistry;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public final class ClamHarvestData {
    private static final Codec<CooldownEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("pos").forGetter(CooldownEntry::pos),
            Codec.LONG.fieldOf("available_at").forGetter(CooldownEntry::availableAt),
            Codec.LONG.optionalFieldOf("duration", 0L).forGetter(CooldownEntry::duration)
    ).apply(instance, CooldownEntry::new));

    public static final MapCodec<ClamHarvestData> CODEC = ENTRY_CODEC.listOf().fieldOf("cooldowns").xmap(
            ClamHarvestData::new,
            ClamHarvestData::entries
    );

    private final Long2LongOpenHashMap cooldowns = new Long2LongOpenHashMap();
    private final Long2LongOpenHashMap cooldownDurations = new Long2LongOpenHashMap();

    public ClamHarvestData() {
    }

    private ClamHarvestData(List<CooldownEntry> entries) {
        for (CooldownEntry entry : entries) {
            cooldowns.put(entry.pos(), entry.availableAt());
            if (entry.duration() > 0L) {
                cooldownDurations.put(entry.pos(), entry.duration());
            }
        }
    }

    public static long getRemainingCooldownTicks(ServerLevel level, BlockPos pos) {
        Cooldown cooldown = getCooldown(level, pos);
        return cooldown.endTick() == 0L ? 0L : cooldown.endTick() - level.getGameTime();
    }

    public static Cooldown getCooldown(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ClamHarvestData data = chunk.getExistingDataOrNull(AttachmentRegistry.CLAM_HARVEST.get());
        if (data == null) {
            return Cooldown.NONE;
        }

        long packedPos = pos.asLong();
        if (!data.cooldowns.containsKey(packedPos)) {
            return Cooldown.NONE;
        }

        long cooldownEndTick = data.cooldowns.get(packedPos);
        if (cooldownEndTick > level.getGameTime()) {
            long durationTicks = data.cooldownDurations.get(packedPos);
            if (durationTicks <= 0L) {
                durationTicks = cooldownEndTick - level.getGameTime();
                data.cooldownDurations.put(packedPos, durationTicks);
                chunk.markUnsaved();
            }
            return new Cooldown(cooldownEndTick, durationTicks);
        }

        data.cooldowns.remove(packedPos);
        data.cooldownDurations.remove(packedPos);
        if (data.cooldowns.isEmpty()) {
            chunk.removeData(AttachmentRegistry.CLAM_HARVEST.get());
        } else {
            chunk.markUnsaved();
        }
        return Cooldown.NONE;
    }

    public static void startCooldown(ServerLevel level, BlockPos pos, long durationTicks) {
        LevelChunk chunk = level.getChunkAt(pos);
        ClamHarvestData data = chunk.getData(AttachmentRegistry.CLAM_HARVEST.get());
        long packedPos = pos.asLong();
        data.cooldowns.put(packedPos, level.getGameTime() + durationTicks);
        data.cooldownDurations.put(packedPos, durationTicks);
        chunk.markUnsaved();
    }

    public boolean isEmpty() {
        return cooldowns.isEmpty();
    }

    private List<CooldownEntry> entries() {
        List<CooldownEntry> entries = new ArrayList<>(cooldowns.size());
        cooldowns.long2LongEntrySet().forEach(entry ->
                entries.add(new CooldownEntry(
                        entry.getLongKey(),
                        entry.getLongValue(),
                        cooldownDurations.get(entry.getLongKey())
                )));
        return entries;
    }

    public record Cooldown(long endTick, long durationTicks) {
        private static final Cooldown NONE = new Cooldown(0L, 0L);
    }

    private record CooldownEntry(long pos, long availableAt, long duration) {
    }
}
