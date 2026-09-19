package com.sshakusora.shadowsandpetals.compat.chinjufu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChinjufuDataConvertersTest {
    @Test
    void tansuToVanityKeepsOnlySlotsSupportedByVanity() {
        CompoundTag oldTag = new CompoundTag();
        ListTag oldItems = new ListTag();
        oldItems.add(item(0, "minecraft:stick"));
        oldItems.add(item(8, "minecraft:apple"));
        oldItems.add(item(9, "minecraft:stone"));
        oldItems.add(item(-1, "minecraft:dirt"));
        oldTag.put("Items", oldItems);

        CompoundTag migrated = ChinjufuDataConverters.tansuToVanity(oldTag, null, null);
        ListTag migratedItems = migrated.getList("Items", Tag.TAG_COMPOUND);

        assertEquals(2, migratedItems.size());
        assertEquals("minecraft:stick", migratedItems.getCompound(0).getString("id"));
        assertEquals("minecraft:apple", migratedItems.getCompound(1).getString("id"));
        assertEquals(4, oldTag.getList("Items", Tag.TAG_COMPOUND).size());
    }

    private static CompoundTag item(int slot, String id) {
        CompoundTag item = new CompoundTag();
        item.putInt("Slot", slot);
        item.putString("id", id);
        item.putByte("Count", (byte) 1);
        return item;
    }
}
