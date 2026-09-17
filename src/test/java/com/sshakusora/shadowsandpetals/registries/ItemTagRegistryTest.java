package com.sshakusora.shadowsandpetals.registries;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTagRegistryTest {
    @Test
    void clearLeavesTheDeclarationStoreEmpty() {
        ItemTagRegistry.clear();

        assertTrue(ItemTagRegistry.getAll().isEmpty());
    }

    @Test
    void generatedFruitTagPreservesBuilderDeclarations() throws IOException {
        assertEquals(
                List.of("shadowsandpetals:orange", "shadowsandpetals:grape"),
                readValues("data/c/tags/item/foods/fruit.json")
        );
    }

    @Test
    void generatedVegetableTagPreservesBuilderDeclarationsWithoutDuplicates() throws IOException {
        List<String> values = readValues("data/c/tags/item/foods/vegetable.json");

        assertEquals(List.of(
                "shadowsandpetals:bean_pod",
                "shadowsandpetals:cabbage",
                "shadowsandpetals:chinese_cabbage",
                "shadowsandpetals:corn",
                "shadowsandpetals:onion",
                "shadowsandpetals:spinach",
                "shadowsandpetals:tomato"
        ), values);
        assertEquals(values.size(), new LinkedHashSet<>(values).size());
    }

    @Test
    void generatedModItemsContainsAllTaggedFoods() throws IOException {
        List<String> values = readValues("data/shadowsandpetals/tags/item/mod_items.json");
        Set<String> expected = Set.of(
                "shadowsandpetals:bean_pod",
                "shadowsandpetals:cabbage",
                "shadowsandpetals:chinese_cabbage",
                "shadowsandpetals:corn",
                "shadowsandpetals:grape",
                "shadowsandpetals:onion",
                "shadowsandpetals:orange",
                "shadowsandpetals:spinach",
                "shadowsandpetals:tomato"
        );

        assertTrue(values.containsAll(expected));
    }

    private static List<String> readValues(String relativePath) throws IOException {
        Path path = projectRoot()
                .resolve("src/generated/resources")
                .resolve(relativePath);
        assertTrue(Files.isRegularFile(path), "missing generated tag: " + path);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonArray values = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .getAsJsonArray("values");
            return values.asList().stream().map(element -> element.getAsString()).toList();
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/generated/resources"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("could not locate project root from " + Path.of("").toAbsolutePath());
    }
}
