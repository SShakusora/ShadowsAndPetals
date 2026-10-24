package com.sshakusora.shadowsandpetals.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Shared content discovery and serialization for the tagged food boxes.
 */
public final class BoxContents {
    public static final String CONTENT_TAG = "Content";
    public static final String TOP_CONTENT_TAG = "TopContent";
    public static final String PRIMARY_IS_TOP_TAG = "PrimaryIsTop";
    public static final String DEFAULT_CONTENT_ID = "minecraft:porkchop";

    public static final TagKey<Item> RAW_FISH = commonTag("foods/raw_fish");
    public static final TagKey<Item> RAW_MEAT = commonTag("foods/raw_meat");
    public static final TagKey<Item> VEGETABLE = commonTag("foods/vegetable");
    public static final TagKey<Item> BERRY = commonTag("foods/berry");
    public static final TagKey<Item> CROPS = commonTag("crops");

    private static final List<TagKey<Item>> CONTENT_TAGS = List.of(RAW_FISH, RAW_MEAT, VEGETABLE, BERRY, CROPS);

    /**
     * Optional filter applied to tag candidates before they become creative box stacks. It stays
     * accept-all by default so dedicated-server code never depends on client-only model data.
     */
    private static volatile Predicate<Item> contentFilter = item -> true;

    private BoxContents() {}

    /**
     * Installs the candidate filter, replacing any previously installed one. Client setup installs
     * a model-based filter; the default accepts every tagged item.
     */
    public static void setContentFilter(Predicate<Item> filter) {
        contentFilter = Objects.requireNonNull(filter, "filter");
    }

    /**
     * Resolves the content held by a box stack. A stack without stored data keeps the legacy
     * raw porkchop behaviour, while an unreadable or missing item resolves to an empty stack.
     */
    public static ItemStack contentStack(ItemStack boxStack) {
        String rawContent = contentId(boxStack);
        return rawContent == null ? defaultContent() : resolveContent(rawContent);
    }

    /**
     * Resolves the upper content of a double box stack. Missing or default data yields the
     * legacy porkchop default, matching {@link #contentStack(ItemStack)}.
     */
    public static ItemStack topContentStack(ItemStack boxStack) {
        String rawContent = topContentId(boxStack);
        return rawContent == null ? defaultContent() : resolveContent(rawContent);
    }

    /**
     * A box stack with a stored top content represents a full (double) box. The top value is
     * always written for double boxes so the item round-trips even when both halves are default.
     */
    public static boolean isDoubleItem(ItemStack boxStack) {
        return topContentId(boxStack) != null;
    }

    /**
     * A single box stack that was picked up from a top half keeps that half's placement for
     * rendering and later merge routing.
     */
    public static boolean isTopItem(ItemStack boxStack) {
        CompoundTag tag = blockEntityTag(boxStack);
        return tag != null
                && !tag.contains(TOP_CONTENT_TAG, Tag.TAG_STRING)
                && tag.getBoolean(PRIMARY_IS_TOP_TAG);
    }

    @Nullable
    public static String contentId(ItemStack boxStack) {
        CompoundTag tag = blockEntityTag(boxStack);
        return tag != null && tag.contains(CONTENT_TAG, Tag.TAG_STRING)
                ? tag.getString(CONTENT_TAG)
                : null;
    }

    @Nullable
    public static String topContentId(ItemStack boxStack) {
        CompoundTag tag = blockEntityTag(boxStack);
        return tag != null && tag.contains(TOP_CONTENT_TAG, Tag.TAG_STRING)
                ? tag.getString(TOP_CONTENT_TAG)
                : null;
    }

    @Nullable
    private static CompoundTag blockEntityTag(ItemStack boxStack) {
        CustomData data = boxStack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTag();
    }

    /**
     * Resolves a stored content id. A {@code null} value means no stored data (porkchop);
     * malformed or missing items resolve to an empty stack so rendering can safely skip them.
     */
    public static ItemStack resolveContent(@Nullable String rawContent) {
        if (rawContent == null) {
            return defaultContent();
        }

        ResourceLocation contentId = ResourceLocation.tryParse(rawContent);
        if (contentId == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(contentId).orElse(Items.AIR);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static ItemStack defaultContent() {
        return new ItemStack(Items.PORKCHOP);
    }

    /**
     * Builds the creative-tab variants for every unique item in the five content tags.
     */
    public static List<ItemStack> creativeTabStacks(Item boxItem, BlockEntityType<?> boxType) {
        if (boxItem == Items.AIR) {
            return List.of();
        }

        Predicate<Item> filter = contentFilter;
        Map<ResourceLocation, Item> sortedItems = new TreeMap<>();
        for (TagKey<Item> tag : CONTENT_TAGS) {
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                Item item = holder.value();
                if (item == Items.AIR || item == boxItem) {
                    continue;
                }

                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null || sortedItems.containsKey(id) || !filter.test(item)) {
                    continue;
                }

                sortedItems.put(id, item);
            }
        }

        List<ItemStack> stacks = new ArrayList<>(sortedItems.size());
        for (Map.Entry<ResourceLocation, Item> entry : sortedItems.entrySet()) {
            stacks.add(createBoxStack(boxItem, entry.getValue(), entry.getKey(), boxType));
        }
        return stacks;
    }

    private static ItemStack createBoxStack(Item boxItem, Item content, ResourceLocation contentId, BlockEntityType<?> boxType) {
        ItemStack stack = new ItemStack(boxItem);
        if (content != Items.PORKCHOP) {
            CompoundTag tag = new CompoundTag();
            tag.putString(CONTENT_TAG, contentId.toString());
            BlockItem.setBlockEntityData(stack, boxType, tag);
        }
        return stack;
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
