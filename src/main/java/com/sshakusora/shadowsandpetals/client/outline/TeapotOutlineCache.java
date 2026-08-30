package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.CopperTeapotBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;

/**
 * Resource-reloadable selection outlines for the copper teapot model.
 *
 * <p>The block's collision shape is intentionally a gameplay-friendly
 * approximation. The selection outline is extracted from the Blockbench model
 * instead, preserving the rotated spout and handle geometry. The block entity
 * renderer owns the animated lid, so this cache follows the static main model
 * used by the block-state variants.</p>
 */
public final class TeapotOutlineCache extends SimplePreparableReloadListener<TeapotOutlineCache.Prepared> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier RELOAD_ID = ShadowsAndPetals.asResource("teapot_outlines");
    private static final Identifier MAIN_MODEL =
            ShadowsAndPetals.asResource("models/block/teapot/copper/main.json");
    private static final double MODEL_UNITS_PER_BLOCK = 16.0D;
    private static final TeapotOutlineCache INSTANCE = new TeapotOutlineCache();

    private volatile Map<Boolean, Map<Direction, OutlineGeometry>> outlines = Map.of();

    private TeapotOutlineCache() {
    }

    /**
     * Registers the teapot provider and installs the resource reload listener
     * that prepares its model geometry.
     */
    public static void register(AddClientReloadListenersEvent event) {
        BlockOutlineRegistry.register(BlockRegistry.COPPER_TEAPOT.get(), TeapotOutlineCache::getOutline);
        event.addListener(RELOAD_ID, INSTANCE);
    }

    @Nullable
    private static OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        return selectOutline(state, INSTANCE.outlines);
    }

    @Nullable
    static OutlineGeometry selectOutline(
            BlockState state,
            Map<Boolean, Map<Direction, OutlineGeometry>> outlines
    ) {
        Map<Direction, OutlineGeometry> byDirection = outlines.get(state.getValue(CopperTeapotBlock.ON_IRORI));
        return byDirection == null ? null : byDirection.get(state.getValue(CopperTeapotBlock.FACING));
    }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        return new Prepared(buildDirections(load(manager)));
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        outlines = prepared.outlines();
        LOGGER.debug("Loaded model outlines for the copper teapot");
    }

    private static OutlineGeometry load(ResourceManager manager) {
        Resource resource = manager.getResource(MAIN_MODEL).orElseThrow(() ->
                new IllegalArgumentException("Missing teapot outline model " + MAIN_MODEL));
        try (Reader reader = resource.openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(model);
            if (geometry == null || geometry.lines().isEmpty()) {
                throw new IllegalArgumentException("Teapot outline model has no visible geometry " + MAIN_MODEL);
            }
            return geometry;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Failed to load teapot outline model " + MAIN_MODEL, exception);
        }
    }

    static Map<Boolean, Map<Direction, OutlineGeometry>> buildDirections(OutlineGeometry base) {
        OutlineGeometry onIrori = RockeryOutlineGeometry.translate(
                base,
                0.0D,
                CopperTeapotBlock.IRORI_RENDER_OFFSET * MODEL_UNITS_PER_BLOCK,
                0.0D
        );
        return Map.of(
                false, buildHorizontalDirections(base),
                true, buildHorizontalDirections(onIrori)
        );
    }

    private static Map<Direction, OutlineGeometry> buildHorizontalDirections(OutlineGeometry base) {
        EnumMap<Direction, OutlineGeometry> result = new EnumMap<>(Direction.class);
        result.put(Direction.NORTH, base);
        result.put(Direction.EAST, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.NORTH)));
        result.put(Direction.SOUTH, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.EAST)));
        result.put(Direction.WEST, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.SOUTH)));
        return Map.copyOf(result);
    }

    public record Prepared(Map<Boolean, Map<Direction, OutlineGeometry>> outlines) {
    }
}
