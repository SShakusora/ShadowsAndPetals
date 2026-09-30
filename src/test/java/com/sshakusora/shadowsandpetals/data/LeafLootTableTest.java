package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LeafLootTableTest {
    private static final float[] OAK_STYLE_FRUIT_CHANCES = {
            0.005F,
            0.0055555557F,
            0.00625F,
            0.008333334F,
            0.025F
    };

    @Test
    void generatedFruitDropsMatchOakLeavesBehavior() throws IOException {
        assertFruitDrop("sakura_leaves", "shadowsandpetals:cherry");
        assertFruitDrop("autumn_oak_leaves", "shadowsandpetals:chestnut");
    }

    private static void assertFruitDrop(String leavesId, String fruitId) throws IOException {
        JsonObject loot = loadJson("data/shadowsandpetals/loot_table/blocks/" + leavesId + ".json");
        JsonObject fruitPool = findPoolForItem(loot.getAsJsonArray("pools"), fruitId);
        assertNotNull(fruitPool, leavesId);
        assertEquals(1.0F, fruitPool.get("rolls").getAsFloat());
        assertNoShearsOrSilkTouch(fruitPool.getAsJsonArray("conditions"));

        JsonObject fruitEntry = fruitPool.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(fruitId, fruitEntry.get("name").getAsString());
        assertHasCondition(fruitEntry.getAsJsonArray("conditions"), "minecraft:survives_explosion");

        JsonObject bonusCondition = findCondition(
                fruitEntry.getAsJsonArray("conditions"),
                "minecraft:table_bonus"
        );
        assertNotNull(bonusCondition, leavesId);
        assertEquals("minecraft:fortune", bonusCondition.get("enchantment").getAsString());
        JsonArray chances = bonusCondition.getAsJsonArray("chances");
        assertEquals(OAK_STYLE_FRUIT_CHANCES.length, chances.size());
        for (int i = 0; i < OAK_STYLE_FRUIT_CHANCES.length; i++) {
            assertEquals(OAK_STYLE_FRUIT_CHANCES[i], chances.get(i).getAsFloat(), 0.0F);
        }
    }

    private static JsonObject findPoolForItem(JsonArray pools, String itemId) {
        for (JsonElement poolElement : pools) {
            JsonObject pool = poolElement.getAsJsonObject();
            JsonArray entries = pool.getAsJsonArray("entries");
            if (entries != null && entries.size() == 1) {
                JsonElement name = entries.get(0).getAsJsonObject().get("name");
                if (name != null && itemId.equals(name.getAsString())) {
                    return pool;
                }
            }
        }
        return null;
    }

    private static void assertNoShearsOrSilkTouch(JsonArray conditions) {
        JsonObject inverted = findCondition(conditions, "minecraft:inverted");
        assertNotNull(inverted);
        JsonObject anyOf = inverted.getAsJsonObject("term");
        assertEquals("minecraft:any_of", anyOf.get("condition").getAsString());

        boolean hasShears = false;
        boolean hasSilkTouch = false;
        for (JsonElement termElement : anyOf.getAsJsonArray("terms")) {
            JsonObject predicate = termElement.getAsJsonObject().getAsJsonObject("predicate");
            if (predicate.has("items")) {
                hasShears = "minecraft:shears".equals(predicate.get("items").getAsString());
            }
            if (predicate.has("predicates")) {
                JsonArray enchantments = predicate.getAsJsonObject("predicates")
                        .getAsJsonArray("minecraft:enchantments");
                if (enchantments != null && enchantments.size() == 1) {
                    hasSilkTouch = "minecraft:silk_touch".equals(
                            enchantments.get(0).getAsJsonObject().get("enchantments").getAsString()
                    );
                }
            }
        }
        assertTrue(hasShears);
        assertTrue(hasSilkTouch);
    }

    private static void assertHasCondition(JsonArray conditions, String conditionType) {
        assertNotNull(findCondition(conditions, conditionType));
    }

    private static JsonObject findCondition(JsonArray conditions, String conditionType) {
        for (JsonElement conditionElement : conditions) {
            JsonObject condition = conditionElement.getAsJsonObject();
            if (conditionType.equals(condition.get("condition").getAsString())) {
                return condition;
            }
        }
        return null;
    }

    private static JsonObject loadJson(String resourceName) throws IOException {
        try (InputStream stream = LeafLootTableTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(stream, resourceName);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
