package com.sshakusora.shadowsandpetals.data.model;

import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class SuggestedItemModels implements ItemModelOutput {
    private final Map<Item, Suggestion> suggestions = new HashMap<>();
    private final Map<Item, Item> copies = new HashMap<>();

    @Override
    public void accept(Item item, ItemModel.Unbaked model, ClientItem.Properties properties) {
        suggestions.put(item, new Suggestion(model, properties));
    }

    @Override
    public void copy(Item donor, Item acceptor) {
        copies.put(acceptor, donor);
    }

    Suggestion get(Item item) {
        return resolve(item, new HashSet<>());
    }

    private Suggestion resolve(Item item, Set<Item> visited) {
        Suggestion direct = suggestions.get(item);
        if (direct != null) {
            return direct;
        }
        Item donor = copies.get(item);
        if (donor == null) {
            return null;
        }
        if (!visited.add(item)) {
            throw new IllegalStateException("Circular item model copy involving " + item);
        }
        return resolve(donor, visited);
    }

    record Suggestion(ItemModel.Unbaked model, ClientItem.Properties properties) {
    }
}
