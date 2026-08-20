package com.sshakusora.shadowsandpetals.data;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModConnectedTextureProviderTest {
    @Test
    void cropFirstTileCopiesTheTopLeftSheetTile() {
        BufferedImage source = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFF112233);
        source.setRGB(15, 15, 0xFF445566);
        source.setRGB(16, 0, 0xFF778899);

        BufferedImage result = ModConnectedTextureProvider.cropFirstTile(
                source,
                Identifier.fromNamespaceAndPath("shadowsandpetals", "block/raw_concrete/connected"),
                2);

        assertEquals(16, result.getWidth());
        assertEquals(16, result.getHeight());
        assertEquals(0xFF112233, result.getRGB(0, 0));
        assertEquals(0xFF445566, result.getRGB(15, 15));
    }
}
