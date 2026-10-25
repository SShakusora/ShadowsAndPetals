package com.sshakusora.shadowsandpetals.client.model.box;

import com.sshakusora.shadowsandpetals.block.decoration.BoxBlock;
import com.sshakusora.shadowsandpetals.blockentity.BoxBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.box.BoxContentQuadCache.ContentQuads;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Baked box variant model that appends the stored content as cached chunk geometry.
 *
 * <p>The shell is still the blockstate model and all rendering properties are delegated to
 * it. Only the <em>general</em> quad list receives content geometry so the chunk renderer
 * cannot cull it against a neighbouring block. Content is read from the immutable
 * {@link BoxBlockEntity#RENDER_DATA} snapshot supplied by the block entity, so chunk workers
 * never touch the block entity itself.</p>
 *
 * <p>This wrapper only replaces the placed block variants. The inventory model stays
 * {@code builtin/entity} and is still drawn by {@code BoxItemRenderer}.</p>
 */
public final class BoxBakedModel extends BakedModelWrapper<BakedModel> {
    private static final long RENDER_TYPE_SEED = 42L;

    /** Shell render types per state; independent of the content and constant per variant. */
    private final Map<BlockState, ChunkRenderTypeSet> shellRenderTypes = new ConcurrentHashMap<>();

    public BoxBakedModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData data,
            @Nullable RenderType renderType
    ) {
        List<BakedQuad> shellQuads = originalModel.getQuads(state, side, rand, data, renderType);
        if (state == null || !(state.getBlock() instanceof BoxBlock)
                || !data.has(BoxBlockEntity.RENDER_DATA)) {
            return shellQuads;
        }

        ContentQuads lower = contentFor(state, data, false);
        ContentQuads upper = contentFor(state, data, true);
        if (isEmpty(lower) && isEmpty(upper)) {
            return shellQuads;
        }

        // A content render type can add a layer beyond the shell's layers. Only emit the shell
        // in layers it actually belongs to, otherwise it would be tesselated twice.
        List<BakedQuad> shell = renderType == null || shellRenderTypes(state).contains(renderType)
                ? shellQuads
                : List.of();

        if (side != null) {
            return shell;
        }

        List<BakedQuad> content = mergeContent(lower, upper, renderType);
        if (shell.isEmpty()) {
            return content;
        }
        if (content.isEmpty()) {
            return shell;
        }

        List<BakedQuad> merged = new ArrayList<>(shell.size() + content.size());
        merged.addAll(shell);
        merged.addAll(content);
        return merged;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        ChunkRenderTypeSet types = originalModel.getRenderTypes(state, rand, data);
        if (state == null || !(state.getBlock() instanceof BoxBlock) || !data.has(BoxBlockEntity.RENDER_DATA)) {
            return types;
        }

        types = withContentTypes(types, contentFor(state, data, false));
        return withContentTypes(types, contentFor(state, data, true));
    }

    private ChunkRenderTypeSet shellRenderTypes(BlockState state) {
        return this.shellRenderTypes.computeIfAbsent(state, s -> originalModel.getRenderTypes(
                s,
                RandomSource.create(RENDER_TYPE_SEED),
                ModelData.EMPTY
        ));
    }

    /**
     * Returns the cached content for one half of the box, or {@code null} when that half has no
     * content in the current state. The mapping mirrors the removed block entity renderer:
     * a full box uses {@code content} below and {@code topContent} above, while a single half
     * always stores its item in {@code content} (regardless of whether it is the upper or
     * lower slab).
     */
    @Nullable
    private static ContentQuads contentFor(BlockState state, ModelData data, boolean topHalf) {
        BoxBlockEntity.RenderData renderData = data.get(BoxBlockEntity.RENDER_DATA);
        if (renderData == null) {
            return null;
        }

        SlabType type = state.getValue(BoxBlock.TYPE);
        ResourceLocation contentId;
        switch (type) {
            case DOUBLE -> contentId = topHalf ? renderData.topContent() : renderData.content();
            case TOP -> {
                if (!topHalf) {
                    return null;
                }
                contentId = renderData.content();
            }
            default -> {
                if (topHalf) {
                    return null;
                }
                contentId = renderData.content();
            }
        }

        if (contentId == null) {
            return null;
        }
        return BoxContentQuadCache.get(contentId, state.getValue(BoxBlock.FACING), topHalf);
    }

    private static boolean isEmpty(@Nullable ContentQuads content) {
        return content == null || content.isEmpty();
    }

    private static ChunkRenderTypeSet withContentTypes(ChunkRenderTypeSet types, @Nullable ContentQuads content) {
        if (isEmpty(content)) {
            return types;
        }

        boolean alreadyPresent = true;
        for (RenderType renderType : content.quads().keySet()) {
            if (!types.contains(renderType)) {
                alreadyPresent = false;
                break;
            }
        }
        if (alreadyPresent) {
            return types;
        }
        return ChunkRenderTypeSet.union(types, ChunkRenderTypeSet.of(content.quads().keySet()));
    }

    private static List<BakedQuad> mergeContent(
            @Nullable ContentQuads lower,
            @Nullable ContentQuads upper,
            @Nullable RenderType renderType
    ) {
        List<BakedQuad> lowerQuads = select(lower, renderType);
        List<BakedQuad> upperQuads = select(upper, renderType);
        if (lowerQuads.isEmpty()) {
            return upperQuads;
        }
        if (upperQuads.isEmpty()) {
            return lowerQuads;
        }

        List<BakedQuad> merged = new ArrayList<>(lowerQuads.size() + upperQuads.size());
        merged.addAll(lowerQuads);
        merged.addAll(upperQuads);
        return merged;
    }

    private static List<BakedQuad> select(@Nullable ContentQuads content, @Nullable RenderType renderType) {
        if (isEmpty(content)) {
            return List.of();
        }
        if (renderType != null) {
            return content.layer(renderType);
        }

        // A null render type means "all layers" (breaking overlay/non-standard rendering); the
        // chunk renderer never asks for a lumped list, so this only merges on that path.
        Map<RenderType, List<BakedQuad>> layers = content.quads();
        if (layers.size() == 1) {
            return layers.values().iterator().next();
        }
        List<BakedQuad> all = new ArrayList<>();
        for (List<BakedQuad> layer : layers.values()) {
            all.addAll(layer);
        }
        return all;
    }
}
