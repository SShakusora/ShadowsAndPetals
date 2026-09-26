package com.sshakusora.shadowsandpetals.client.ct;

import com.sshakusora.shadowsandpetals.client.ct.CTRegistry.CTEntry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CTRegistryTest {
    @Test
    void copycatTextureUsesTheConnectedSheetAndTileIndex() {
        CTEntry entry = entry();

        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        "shadowsandpetals",
                        "block/raw_concrete/connected_bleed/copycat/37"),
                entry.copycatTexture(0, 37));
    }

    @Test
    void copycatTextureRejectsTilesOutsideTheRegisteredSheet() {
        CTEntry entry = entry();

        assertThrows(IndexOutOfBoundsException.class, () -> entry.copycatTexture(0, 64));
    }

    private static CTEntry entry() {
        return new CTEntry(
                ResourceLocation.fromNamespaceAndPath("shadowsandpetals", "block/raw_concrete/base"),
                List.of(ResourceLocation.fromNamespaceAndPath(
                        "shadowsandpetals", "block/raw_concrete/connected_bleed")),
                CTTextureSelector.FIRST,
                CTTextureType.OMNIDIRECTIONAL,
                1);
    }
}
