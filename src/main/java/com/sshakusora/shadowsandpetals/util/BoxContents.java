package com.sshakusora.shadowsandpetals.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Shared content discovery and serialization for the tagged food boxes.
 */
public final class BoxContents {
    public static final String CONTENT_TAG = "Content";
    public static final String TOP_CONTENT_TAG = "TopContent";
    public static final String PRIMARY_IS_TOP_TAG = "PrimaryIsTop";
    /** Marker written for an empty top half so a double box round-trips as double. */
    public static final String EMPTY_CONTENT_ID = "minecraft:air";

    public static final TagKey<Item> RAW_FISH = commonTag("foods/raw_fish");
    public static final TagKey<Item> RAW_MEAT = commonTag("foods/raw_meat");
    public static final TagKey<Item> VEGETABLE = commonTag("foods/vegetable");
    public static final TagKey<Item> FRUIT = commonTag("foods/fruit");
    public static final TagKey<Item> BERRY = commonTag("foods/berry");
    public static final TagKey<Item> CROPS = commonTag("crops");

    private static final List<TagKey<Item>> CONTENT_TAGS = List.of(RAW_FISH, RAW_MEAT, VEGETABLE, FRUIT, BERRY, CROPS);

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
     * Resolves the content held by a box stack. A stack without stored data is an empty box;
     * an unreadable or missing item also resolves to an empty stack.
     */
    public static ItemStack contentStack(ItemStack boxStack) {
        return resolveContent(contentId(boxStack));
    }

    /**
     * Resolves the upper content of a double box stack. Missing data means that half is empty.
     */
    public static ItemStack topContentStack(ItemStack boxStack) {
        return resolveContent(topContentId(boxStack));
    }

    /**
     * A box stack with a stored top content represents a full (double) box. The top value is
     * always written for double boxes so the item round-trips even when a half is empty.
     */
    public static boolean isDoubleItem(ItemStack boxStack) {
        return topContentId(boxStack) != null;
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
     * Resolves a stored content id. A {@code null} value means the box is empty; malformed or
     * missing items also resolve to an empty stack so rendering can safely skip them.
     */
    public static ItemStack resolveContent(@Nullable String rawContent) {
        if (rawContent == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation contentId = ResourceLocation.tryParse(rawContent);
        if (contentId == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(contentId).orElse(Items.AIR);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * Builds the creative-tab entries: the bare (empty) box first, then one box per unique item
     * in the content tags.
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

        List<ItemStack> stacks = new ArrayList<>(sortedItems.size() + 1);
        // The bare box is the empty box; keep it first in the tab.
        stacks.add(new ItemStack(boxItem));
        for (Map.Entry<ResourceLocation, Item> entry : sortedItems.entrySet()) {
            stacks.add(createBoxStack(boxItem, entry.getKey(), boxType));
        }
        return stacks;
    }

    /**
     * Collects the unique items reachable from the content tags by reading real tag files
     * from the datagen resource manager. The registry lookup passed to data providers does not
     * carry tags, so enumerating tag contents has to go through {@link TagLoader}. The filter is
     * the same model check the client applies to the boxes tab.
     */
    public static List<Item> taggedContentItems(ResourceManager resourceManager, Predicate<Item> filter) {
        Map<ResourceLocation, Collection<Item>> tags = new TagLoader<Item>(
                id -> BuiltInRegistries.ITEM.getOptional(id),
                "tags/item"
        ).loadAndBuild(resourceManager);

        Set<Item> items = new LinkedHashSet<>();
        for (TagKey<Item> tag : CONTENT_TAGS) {
            for (Item item : tags.getOrDefault(tag.location(), List.of())) {
                if (item != Items.AIR && filter.test(item)) {
                    items.add(item);
                }
            }
        }
        return List.copyOf(items);
    }

    /**
     * Builds a box stack holding the given content item, matching the components written by the
     * creative-tab box stacks.
     */
    public static ItemStack createContentBox(Item boxItem, Item content, BlockEntityType<?> boxType) {
        ResourceLocation contentId = BuiltInRegistries.ITEM.getKey(content);
        if (contentId == null) {
            throw new IllegalArgumentException("Unregistered content item " + content);
        }
        return createBoxStack(boxItem, contentId, boxType);
    }

    private static ItemStack createBoxStack(Item boxItem, ResourceLocation contentId, BlockEntityType<?> boxType) {
        ItemStack stack = new ItemStack(boxItem);
        CompoundTag tag = new CompoundTag();
        tag.putString(CONTENT_TAG, contentId.toString());
        BlockItem.setBlockEntityData(stack, boxType, tag);
        return stack;
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
