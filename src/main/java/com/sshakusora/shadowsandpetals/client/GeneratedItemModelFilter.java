package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.util.BoxContents;
import com.sshakusora.shadowsandpetals.util.GeneratedItemModels;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

/**
 * Client-only creative box filter that keeps items whose default item model directly inherits
 * {@code minecraft:item/generated}.
 *
 * <p>The model JSON is read from the current {@link ResourceManager} on every call so resource
 * pack changes take effect immediately without reload bookkeeping. Recipe datagen reuses the
 * same check from {@link GeneratedItemModels}.</p>
 */
public final class GeneratedItemModelFilter {
    private GeneratedItemModelFilter() {
    }

    /**
     * Installs the filter into {@link BoxContents}. Only called from client setup.
     */
    public static void install() {
        BoxContents.setContentFilter(GeneratedItemModelFilter::hasGeneratedParent);
    }

    private static boolean hasGeneratedParent(Item item) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resourceManager = minecraft == null ? null : minecraft.getResourceManager();
        return resourceManager != null && GeneratedItemModels.hasGeneratedParent(resourceManager, item);
    }
}
