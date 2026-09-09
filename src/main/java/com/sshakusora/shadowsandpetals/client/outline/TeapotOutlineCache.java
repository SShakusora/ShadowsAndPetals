package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.CopperTeapotBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPart;
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
    private static final String GRILL_MODEL_PREFIX = "models/block/grill/double/";
    private static final double MODEL_UNITS_PER_BLOCK = 16.0D;
    private static final TeapotOutlineCache INSTANCE = new TeapotOutlineCache();

    private volatile Map<Boolean, Map<Direction, OutlineGeometry>> outlines = Map.of();
    private volatile Map<IroriGrillPart, Map<Direction, OutlineGeometry>> compositeOutlines = Map.of();

    private TeapotOutlineCache() {
    }

    /**
     * Registers the teapot provider and installs the resource reload listener
     * that prepares its model geometry.
     */
    public static void register(AddClientReloadListenersEvent event) {
        BlockOutlineRegistry.register(BlockRegistry.COPPER_TEAPOT.get(), TeapotOutlineCache::getOutline);
        BlockOutlineRegistry.register(
                BlockRegistry.IRORI_GRILL_COPPER_TEAPOT.get(),
                TeapotOutlineCache::getCompositeOutline
        );
        event.addListener(RELOAD_ID, INSTANCE);
    }

    @Nullable
    private static OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        return selectOutline(state, INSTANCE.outlines);
    }

    @Nullable
    private static OutlineGeometry getCompositeOutline(BlockState state, BlockOutlineContext context) {
        Map<Direction, OutlineGeometry> byDirection =
                INSTANCE.compositeOutlines.get(state.getValue(IroriGrillBlock.GRILL_PART));
        return byDirection == null ? null : byDirection.get(state.getValue(CopperTeapotBlock.FACING));
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
        Map<Boolean, Map<Direction, OutlineGeometry>> teapotOutlines = buildDirections(load(manager, MAIN_MODEL));
        return new Prepared(
                teapotOutlines,
                buildCompositeOutlines(teapotOutlines.get(true), loadGrillOutlines(manager))
        );
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        outlines = prepared.outlines();
        compositeOutlines = prepared.compositeOutlines();
        LOGGER.debug("Loaded model outlines for the copper teapot and its Irori grill composite");
    }

    private static OutlineGeometry load(ResourceManager manager, Identifier modelId) {
        Resource resource = manager.getResource(modelId).orElseThrow(() ->
                new IllegalArgumentException("Missing outline model " + modelId));
        try (Reader reader = resource.openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(model);
            if (geometry == null || geometry.lines().isEmpty()) {
                throw new IllegalArgumentException("Outline model has no visible geometry " + modelId);
            }
            return geometry;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Failed to load outline model " + modelId, exception);
        }
    }

    private static Map<IroriGrillPart, OutlineGeometry> loadGrillOutlines(ResourceManager manager) {
        EnumMap<IroriGrillPart, OutlineGeometry> result = new EnumMap<>(IroriGrillPart.class);
        for (IroriGrillPart part : IroriGrillPart.values()) {
            Identifier modelId = ShadowsAndPetals.asResource(
                    GRILL_MODEL_PREFIX + part.modelName() + "_upper.json"
            );
            result.put(part, orientGrillOutline(part, load(manager, modelId)));
        }
        return Map.copyOf(result);
    }

    static OutlineGeometry orientGrillOutline(IroriGrillPart part, OutlineGeometry geometry) {
        return part == IroriGrillPart.STRIP_WEST || part == IroriGrillPart.STRIP_EAST
                ? RockeryOutlineGeometry.rotateClockwise(geometry)
                : geometry;
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

    static Map<IroriGrillPart, Map<Direction, OutlineGeometry>> buildCompositeOutlines(
            Map<Direction, OutlineGeometry> teapotOutlines,
            Map<IroriGrillPart, OutlineGeometry> grillOutlines
    ) {
        EnumMap<IroriGrillPart, Map<Direction, OutlineGeometry>> result =
                new EnumMap<>(IroriGrillPart.class);
        for (IroriGrillPart part : IroriGrillPart.values()) {
            OutlineGeometry grill = grillOutlines.get(part);
            if (grill == null) {
                continue;
            }

            EnumMap<Direction, OutlineGeometry> byDirection = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                OutlineGeometry teapot = teapotOutlines.get(direction);
                if (teapot != null) {
                    byDirection.put(direction, combine(teapot, grill));
                }
            }
            result.put(part, Map.copyOf(byDirection));
        }
        return Map.copyOf(result);
    }

    static OutlineGeometry combine(OutlineGeometry first, OutlineGeometry second) {
        List<OutlineGeometry.Line> lines = new ArrayList<>(first.lines().size() + second.lines().size());
        lines.addAll(first.lines());
        lines.addAll(second.lines());
        return OutlineGeometry.of(lines);
    }

    public record Prepared(
            Map<Boolean, Map<Direction, OutlineGeometry>> outlines,
            Map<IroriGrillPart, Map<Direction, OutlineGeometry>> compositeOutlines
    ) {
    }
}
