package com.sshakusora.shadowsandpetals.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
 * Shared check for items whose default item model directly inherits
 * {@code minecraft:item/generated}. Boxes can only display generated sprites cleanly, so both
 * the client creative tab and recipe datagen skip items that fail this check.
 */
public final class GeneratedItemModels {
    private static final ResourceLocation GENERATED_MODEL_PARENT =
            ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated");
    private static final String PARENT_KEY = "parent";

    private GeneratedItemModels() {
    }

    /**
     * Returns {@code true} only when the item's model JSON exists in the given resource manager
     * and its direct parent is {@code minecraft:item/generated}. A missing namespace on the
     * parent defaults to {@code minecraft}. Transitive parents are not followed.
     */
    public static boolean hasGeneratedParent(ResourceManager resourceManager, Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
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
