package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;

/**
 * Resource-reloadable selection outlines for vanity models.
 *
 * <p>The block's collision shapes intentionally remain a compact gameplay
 * approximation. The selection outline is instead extracted from the two
 * Blockbench model JSON files, so small trim pieces and rotated elements stay
 * aligned with the rendered model.</p>
 */
public final class VanityOutlineCache extends SimplePreparableReloadListener<VanityOutlineCache.Prepared> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier RELOAD_ID = ShadowsAndPetals.asResource("vanity_outlines");
    private static final Identifier LOWER_MODEL =
            ShadowsAndPetals.asResource("models/block/vanity/vanity_lower.json");
    private static final Identifier UPPER_MODEL =
            ShadowsAndPetals.asResource("models/block/vanity/vanity_upper.json");
    private static final VanityOutlineCache INSTANCE = new VanityOutlineCache();

    private volatile Map<DoubleBlockHalf, Map<Direction, OutlineGeometry>> outlines = Map.of();

    private VanityOutlineCache() {
    }

    /**
     * Registers the shared model outlines for every wood vanity and installs
     * the resource reload listener that prepares their geometry.
     */
    public static void register(AddClientReloadListenersEvent event) {
        for (var vanity : BlockRegistry.VANITIES) {
            BlockOutlineRegistry.register(vanity.get(), VanityOutlineCache::getOutline);
        }
        event.addListener(RELOAD_ID, INSTANCE);
    }

    @Nullable
    private static OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        return selectOutline(state, INSTANCE.outlines);
    }

    @Nullable
    static OutlineGeometry selectOutline(
            BlockState state,
            Map<DoubleBlockHalf, Map<Direction, OutlineGeometry>> outlines
    ) {
        Map<Direction, OutlineGeometry> byDirection = outlines.get(state.getValue(VanityBlock.HALF));
        return byDirection == null ? null : byDirection.get(state.getValue(VanityBlock.FACING));
    }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        OutlineGeometry lower = load(manager, LOWER_MODEL);
        OutlineGeometry upper = load(manager, UPPER_MODEL);
        return new Prepared(buildDirections(lower, upper));
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        outlines = prepared.outlines();
        LOGGER.debug("Loaded model outlines for {} vanity halves", outlines.size());
    }

    private static OutlineGeometry load(ResourceManager manager, Identifier model) {
        Resource resource = manager.getResource(model).orElseThrow(() ->
                new IllegalArgumentException("Missing vanity outline model " + model));
        try (Reader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(json);
            if (geometry == null || geometry.lines().isEmpty()) {
                throw new IllegalArgumentException("Vanity outline model has no visible geometry " + model);
            }
            return geometry;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Failed to load vanity outline model " + model, exception);
        }
    }

    static Map<DoubleBlockHalf, Map<Direction, OutlineGeometry>> buildDirections(
            OutlineGeometry lower,
            OutlineGeometry upper
    ) {
        EnumMap<DoubleBlockHalf, Map<Direction, OutlineGeometry>> result = new EnumMap<>(DoubleBlockHalf.class);
        result.put(DoubleBlockHalf.LOWER, buildHorizontalDirections(lower));
        result.put(DoubleBlockHalf.UPPER, buildHorizontalDirections(upper));
        return Map.copyOf(result);
    }

    private static Map<Direction, OutlineGeometry> buildHorizontalDirections(OutlineGeometry base) {
        EnumMap<Direction, OutlineGeometry> result = new EnumMap<>(Direction.class);
        result.put(Direction.NORTH, base);
        result.put(Direction.EAST, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.NORTH)));
        result.put(Direction.SOUTH, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.EAST)));
        result.put(Direction.WEST, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.SOUTH)));
        return Map.copyOf(result);
    }

    public record Prepared(Map<DoubleBlockHalf, Map<Direction, OutlineGeometry>> outlines) {
    }
}
