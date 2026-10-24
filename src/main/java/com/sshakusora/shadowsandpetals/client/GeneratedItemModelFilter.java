package com.sshakusora.shadowsandpetals.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sshakusora.shadowsandpetals.util.BoxContents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Client-only creative box filter that keeps items whose default item model directly inherits
 * {@code minecraft:item/generated}.
 *
 * <p>The model JSON is read from the current {@link ResourceManager} on every call so resource
 * pack changes take effect immediately without reload bookkeeping.</p>
 */
public final class GeneratedItemModelFilter {
    private static final ResourceLocation GENERATED_MODEL_PARENT =
            ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated");
    private static final String PARENT_KEY = "parent";

    private GeneratedItemModelFilter() {
    }

    /**
     * Installs the filter into {@link BoxContents}. Only called from client setup.
     */
    public static void install() {
        BoxContents.setContentFilter(GeneratedItemModelFilter::hasGeneratedParent);
    }

    /**
     * Returns {@code true} only when the item's model JSON exists and its direct parent is
     * {@code minecraft:item/generated}. A missing namespace on the parent defaults to
     * {@code minecraft}. Transitive parents are not followed.
     */
    private static boolean hasGeneratedParent(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resourceManager = minecraft == null ? null : minecraft.getResourceManager();
        if (resourceManager == null) {
            return false;
        }

        ResourceLocation modelFile = ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "models/item/" + itemId.getPath() + ".json");

        Optional<Resource> model = resourceManager.getResource(modelFile);
        if (model.isEmpty()) {
            return false;
        }

        try (InputStream stream = model.get().open()) {
            JsonElement root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                return false;
            }

            JsonElement parent = root.getAsJsonObject().get(PARENT_KEY);
            if (parent == null || !parent.isJsonPrimitive() || !parent.getAsJsonPrimitive().isString()) {
                return false;
            }

            ResourceLocation parentId = ResourceLocation.tryParse(parent.getAsString());
            return GENERATED_MODEL_PARENT.equals(parentId);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }
}
