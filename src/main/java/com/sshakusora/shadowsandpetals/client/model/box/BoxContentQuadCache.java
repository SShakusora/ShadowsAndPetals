package com.sshakusora.shadowsandpetals.client.model.box;

import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bakes the stacked content quads for the placed box model.
 *
 * <p>The block entity model data only exposes resolved item ids, so the quads are built from
 * the item's own baked model. Every transform and tint is applied once per unique content
 * configuration and shared by all positions; chunk workers therefore only pay for map
 * lookups on a cache hit. The chunk renderer calls this off the render thread, so the cache
 * and every stored value are immutable after construction.</p>
 */
public final class BoxContentQuadCache {
    private static final int RANDOM_SEED = 42;

    /**
     * Bumped on resource reload. Keys from an older generation are never read, so a build that
     * was already running when the reload cleared the cache cannot leak stale sprites into a
     * later lookup.
     */
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static final Map<Key, ContentQuads> CACHE = new ConcurrentHashMap<>();

    private BoxContentQuadCache() {
    }

    /**
     * Returns the immutable quads for one half of a box, or {@link ContentQuads#EMPTY} when the
     * content id is missing, unknown, or has no bakeable geometry.
     */
    public static ContentQuads get(@Nullable ResourceLocation contentId, Direction facing, boolean topHalf) {
        if (contentId == null) {
            return ContentQuads.EMPTY;
        }

        Key key = new Key(GENERATION.get(), contentId, facing, topHalf);
        return CACHE.computeIfAbsent(key, BoxContentQuadCache::build);
    }

    /**
     * Drops every cached quad. Called when models are re-baked, since baked quads and atlas
     * sprites are replaced by the reload.
     */
    public static void invalidate() {
        GENERATION.incrementAndGet();
        CACHE.clear();
    }

    private static ContentQuads build(Key key) {
        Item item = BuiltInRegistries.ITEM.get(key.contentId());
        if (item == null || item == Items.AIR) {
            return ContentQuads.EMPTY;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ModelManager modelManager = minecraft.getModelManager();
        ItemStack stack = new ItemStack(item);

        // Mirrors ItemRenderer#getModel(stack, null, null, 0): pick the same base model,
        // including the trident/spyglass in-hand special cases, then let ItemOverrides choose
        // the stack-specific variant. The ItemModelShaper's baked-model cache is deliberately
        // not read: it is rewritten on every resource reload while chunk meshing runs on worker
        // threads. Reading baked models straight from the (reload-swapped) ModelManager and
        // resolving overrides ourselves is the thread-safety-preserving equivalent.
        BakedModel model = resolveModel(item, stack, modelManager);
        if (model == null || model == modelManager.getMissingModel() || model.isCustomRenderer()) {
            // Nothing that can be baked into a chunk mesh (e.g. another BEWLR item).
            return ContentQuads.EMPTY;
        }

        BlockItem blockItem = item instanceof BlockItem typed ? typed : null;
        BlockState state = blockItem == null ? null : blockItem.getBlock().defaultBlockState();
        // ItemRenderer#render derives this flag for the non-GUI item context used by the box
        // preview; multi-pass models may return different passes for it.
        boolean fabulous = blockItem == null
                || (!(blockItem.getBlock() instanceof HalfTransparentBlock)
                && !(blockItem.getBlock() instanceof StainedGlassPaneBlock));
        List<BakedModel> passes = model.getRenderPasses(stack, fabulous);
        if (passes == null || passes.isEmpty()) {
            return ContentQuads.EMPTY;
        }

        ItemColors itemColors = minecraft.getItemColors();
        boolean flatItemModel = !model.isGui3d();
        float depthScale = BoxContentGeometry.depthScale(model.isGui3d());
        float yOffset = key.topHalf() ? BoxContentGeometry.TOP_Y_OFFSET : BoxContentGeometry.LOWER_Y_OFFSET;

        Map<RenderType, List<BakedQuad>> quadsByType = new LinkedHashMap<>();
        RandomSource random = RandomSource.create(RANDOM_SEED);
        for (int layer = 0; layer < BoxContentGeometry.LAYER_COUNT; layer++) {
            IQuadTransformer transform = QuadTransformers.applying(new Transformation(
                    BoxContentGeometry.contentMatrix(layer, yOffset, depthScale, key.facing())
            ));

            for (BakedModel pass : passes) {
                if (pass == null || pass == modelManager.getMissingModel() || pass.isCustomRenderer()) {
                    continue;
                }

                if (blockItem == null || flatItemModel) {
                    // Flat models are generated item sprites, even when the item is also a
                    // BlockItem (modded seeds such as Farmer's Delight rice/onion). Their baked
                    // model carries no block render type of its own, so the state-based fallback
                    // in ItemBlockRenderTypes only knows the vanilla crop blocks and defaults
                    // everything else to solid. Solid has no alpha test, which bakes the sprite's
                    // transparent pixels as an opaque black quad. Cutout is what ItemRenderer
                    // uses for these sprites, and a chunk mesh can use it directly.
                    appendPass(pass, stack, itemColors, transform, random, null, null,
                            quadsByType.computeIfAbsent(RenderType.cutout(), type -> new ArrayList<>()));
                    continue;
                }

                // A pass may contribute several chunk layers (solid/cutout/cutoutMipped/
                // translucent). Bake each layer separately instead of collapsing them into one:
                // the chunk renderer asks for one layer at a time and must not receive another
                // layer's quads.
                ChunkRenderTypeSet renderTypes = pass.getRenderTypes(
                        state,
                        RandomSource.create(RANDOM_SEED),
                        ModelData.EMPTY
                );
                for (RenderType renderType : renderTypes) {
                    appendPass(pass, stack, itemColors, transform, random, state, renderType,
                            quadsByType.computeIfAbsent(renderType, type -> new ArrayList<>()));
                }
            }
        }

        if (quadsByType.isEmpty()) {
            return ContentQuads.EMPTY;
        }
        return new ContentQuads(quadsByType);
    }

    /**
     * Explicit copy of the item model selection performed by
     * {@code ItemRenderer#getModel(ItemStack, null, null, 0)}, minus the item model shaper's
     * mutable cache.
     */
    private static BakedModel resolveModel(Item item, ItemStack stack, ModelManager modelManager) {
        BakedModel base;
        if (item == Items.TRIDENT) {
            base = modelManager.getModel(ItemRenderer.TRIDENT_IN_HAND_MODEL);
        } else if (item == Items.SPYGLASS) {
            base = modelManager.getModel(ItemRenderer.SPYGLASS_IN_HAND_MODEL);
        } else {
            base = modelManager.getModel(ModelResourceLocation.inventory(
                    BuiltInRegistries.ITEM.getKey(stack.getItem())));
        }

        if (base == modelManager.getMissingModel()) {
            return base;
        }

        ItemOverrides overrides = base.getOverrides();
        BakedModel resolved = overrides == null ? base : overrides.resolve(base, stack, null, null, 0);
        return resolved == null ? modelManager.getMissingModel() : resolved;
    }

    private static void appendPass(
            BakedModel pass,
            ItemStack stack,
            ItemColors itemColors,
            IQuadTransformer transform,
            RandomSource random,
            @Nullable BlockState state,
            @Nullable RenderType renderType,
            List<BakedQuad> out
    ) {
        for (Direction direction : Direction.values()) {
            random.setSeed(RANDOM_SEED);
            appendQuads(passQuads(pass, state, direction, random, renderType),
                    stack, itemColors, transform, out);
        }

        random.setSeed(RANDOM_SEED);
        appendQuads(passQuads(pass, state, null, random, renderType),
                stack, itemColors, transform, out);
    }

    /**
     * Items are drawn by {@link ItemRenderer} through the model's full quad list, while chunks
     * ask per chunk layer. Generated items have no chunk layer information of their own, so they
     * keep using the unfiltered item list.
     */
    private static List<BakedQuad> passQuads(
            BakedModel pass,
            @Nullable BlockState state,
            @Nullable Direction direction,
            RandomSource random,
            @Nullable RenderType renderType
    ) {
        if (renderType == null || state == null) {
            return pass.getQuads(null, direction, random);
        }
        return pass.getQuads(state, direction, random, ModelData.EMPTY, renderType);
    }

    /**
     * Copies the quad before transforming it: baked models share their vertex arrays between
     * all render passes, so mutating them in place would corrupt the item model.
     */
    private static void appendQuads(
            List<BakedQuad> source,
            ItemStack stack,
            ItemColors itemColors,
            IQuadTransformer transform,
            List<BakedQuad> out
    ) {
        for (BakedQuad quad : source) {
            int[] vertices = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
            if (quad.isTinted()) {
                int tintIndex = quad.getTintIndex();
                int color = itemColors.getColor(stack, tintIndex);
                if (color != -1) {
                    multiplyColor(vertices, color);
                }
            }

            // Item colours are baked into the vertices, so the block tint must not run again.
            // AO and shade are off to keep the uniform lighting the item renderer used before
            // this geometry moved into the chunk mesh.
            Direction direction = quad.getDirection() == null ? Direction.UP : quad.getDirection();
            BakedQuad copy = new BakedQuad(vertices, -1, direction, quad.getSprite(), false, false);
            transform.processInPlace(copy);
            out.add(copy);
        }
    }

    /**
     * Multiplies the per-vertex ABGR colour by an ARGB item tint, matching what
     * {@code VertexConsumer#putBulkData} does for items.
     */
    private static void multiplyColor(int[] vertices, int argb) {
        int tintA = (argb >>> 24) & 0xFF;
        int tintR = (argb >>> 16) & 0xFF;
        int tintG = (argb >>> 8) & 0xFF;
        int tintB = argb & 0xFF;

        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * IQuadTransformer.STRIDE + IQuadTransformer.COLOR;
            int color = vertices[index];
            int a = (color >>> 24) & 0xFF;
            int b = (color >>> 16) & 0xFF;
            int g = (color >>> 8) & 0xFF;
            int r = color & 0xFF;
            vertices[index] = ((a * tintA / 255) << 24)
                    | ((b * tintB / 255) << 16)
                    | ((g * tintG / 255) << 8)
                    | (r * tintR / 255);
        }
    }

    /**
     * The transformed quads for one content configuration, split by the chunk layer they
     * belong to. Instances are immutable and shared between all boxes.
     */
    public record ContentQuads(Map<RenderType, List<BakedQuad>> quads) {
        public static final ContentQuads EMPTY = new ContentQuads(Map.of());

        public ContentQuads {
            if (quads.isEmpty()) {
                quads = Map.of();
            } else {
                Map<RenderType, List<BakedQuad>> copy = new LinkedHashMap<>(quads.size());
                for (Map.Entry<RenderType, List<BakedQuad>> entry : quads.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        copy.put(entry.getKey(), List.copyOf(entry.getValue()));
                    }
                }
                quads = Collections.unmodifiableMap(copy);
            }
        }

        public boolean isEmpty() {
            return quads.isEmpty();
        }

        public List<BakedQuad> layer(RenderType renderType) {
            List<BakedQuad> layer = quads.get(renderType);
            return layer == null ? List.of() : layer;
        }
    }

    private record Key(
            int generation,
            ResourceLocation contentId,
            Direction facing,
            boolean topHalf
    ) {
    }
}
