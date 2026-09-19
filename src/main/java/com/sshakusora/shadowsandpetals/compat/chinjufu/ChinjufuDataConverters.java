package com.sshakusora.shadowsandpetals.compat.chinjufu;

import com.sshakusora.shadowsandpetals.blockentity.VanityBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

/** Pure data conversions for ChinjufuMod block-entity payloads. */
public final class ChinjufuDataConverters {
    private ChinjufuDataConverters() {}

    /**
     * Converts the old 45-slot Tansu inventory to the nine slots supported by a vanity.
     *
     * <p>The input is copied so a failed or deferred migration never mutates the legacy payload
     * that is still owned by the temporary compatibility block entity.</p>
     */
    public static CompoundTag tansuToVanity(CompoundTag oldTag, BlockState state, BlockPos pos) {
        CompoundTag migrated = oldTag.copy();
        if (!migrated.contains("Items", Tag.TAG_LIST)) {
            return migrated;
        }

        ListTag oldItems = migrated.getList("Items", Tag.TAG_COMPOUND);
        ListTag newItems = new ListTag();
        for (int index = 0; index < oldItems.size(); index++) {
            CompoundTag item = oldItems.getCompound(index);
            int slot = item.getInt("Slot");
            if (slot >= 0 && slot < VanityBlockEntity.CONTAINER_SIZE) {
                newItems.add(item.copy());
            }
        }
        migrated.put("Items", newItems);
        return migrated;
    }
}
