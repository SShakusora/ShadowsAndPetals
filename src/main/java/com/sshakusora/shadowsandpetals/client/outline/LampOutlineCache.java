package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Resource-reloadable outlines for the lamp block models.
 *
 * <p>The model geometry is parsed once per resource reload. Providers queried
 * by the outline event only perform a block/direction lookup, so model JSON is
 * never parsed during rendering.</p>
 */
public final class LampOutlineCache extends SimplePreparableReloadListener<LampOutlineCache.Prepared> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier RELOAD_ID = ShadowsAndPetals.asResource("lamp_outlines");
    private static final LampOutlineCache INSTANCE = new LampOutlineCache();

    private volatile Map<Block, Definition> definitions = Map.of();
    private volatile Map<Block, Map<Direction, OutlineGeometry>> outlines = Map.of();

    private LampOutlineCache() {
    }

    /**
     * Installs the four block providers and registers the resource listener.
     * This is called from the client reload-listener registration event.
     */
    public static void register(AddClientReloadListenersEvent event) {
        Map<Block, Definition> registered = new IdentityHashMap<>();
        register(registered, BlockRegistry.BEDROOM_LAMP.get(), "bedroom_lamp", Orientation.FIXED);
        register(registered, BlockRegistry.WALL_LAMP.get(), "wall_lamp", Orientation.HORIZONTAL);
        register(registered, BlockRegistry.EMERGENCY_LAMP.get(), "emergency_lamp", Orientation.DIRECTIONAL);
        register(registered, BlockRegistry.DESK_LAMP.get(), "desk_lamp", Orientation.HORIZONTAL);

        INSTANCE.definitions = immutableIdentityMap(registered);
        registered.keySet().forEach(block -> BlockOutlineRegistry.register(block, LampOutlineCache::getOutline));
        event.addListener(RELOAD_ID, INSTANCE);
    }

    @Nullable
    private static OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        Definition definition = INSTANCE.definitions.get(state.getBlock());
        return selectOutline(state, definition, INSTANCE.outlines);
    }

    @Nullable
    static OutlineGeometry selectOutline(
            BlockState state,
            @Nullable Definition definition,
            Map<Block, Map<Direction, OutlineGeometry>> outlines
    ) {
        if (definition == null || definition.block() != state.getBlock()) {
            return null;
        }

        Map<Direction, OutlineGeometry> byDirection = outlines.get(state.getBlock());
        if (byDirection == null) {
            return null;
        }
        return selectDirection(byDirection, definition.direction(state));
    }

    @Nullable
    static OutlineGeometry selectDirection(
            @Nullable Map<Direction, OutlineGeometry> byDirection,
            Direction direction
    ) {
        return byDirection == null ? null : byDirection.get(direction);
    }

    private static void register(
            Map<Block, Definition> registered,
            Block block,
            String modelName,
            Orientation orientation
    ) {
        registered.put(
                block,
                new Definition(
                        block,
                        ShadowsAndPetals.asResource("models/block/" + modelName + "/off.json"),
                        orientation
                )
        );
    }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Block, Map<Direction, OutlineGeometry>> prepared = new IdentityHashMap<>();
        for (Definition definition : definitions.values()) {
            OutlineGeometry base = load(manager, definition);
            prepared.put(definition.block(), buildDirections(base, definition.orientation()));
        }
        return new Prepared(immutableIdentityMap(prepared));
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        outlines = prepared.outlines();
        LOGGER.debug("Loaded model outlines for {} lamp blocks", outlines.size());
    }

    private static OutlineGeometry load(ResourceManager manager, Definition definition) {
        Resource resource = manager.getResource(definition.model()).orElseThrow(() ->
                new IllegalArgumentException("Missing lamp outline model " + definition.model()));
        try (Reader reader = resource.openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(model);
            if (geometry == null || geometry.lines().isEmpty()) {
                throw new IllegalArgumentException("Lamp outline model has no visible geometry " + definition.model());
            }
            return geometry;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Failed to load lamp outline model " + definition.model(), exception);
        }
    }

    private static Map<Direction, OutlineGeometry> buildDirections(
            OutlineGeometry base,
            Orientation orientation
    ) {
        EnumMap<Direction, OutlineGeometry> result = new EnumMap<>(Direction.class);
        switch (orientation) {
            case FIXED -> result.put(Direction.UP, base);
            case HORIZONTAL -> {
                result.put(Direction.NORTH, base);
                result.put(Direction.EAST, transform(base, LampOutlineCache::rotateClockwise));
                result.put(Direction.SOUTH, transform(result.get(Direction.EAST), LampOutlineCache::rotateClockwise));
                result.put(Direction.WEST, transform(result.get(Direction.SOUTH), LampOutlineCache::rotateClockwise));
            }
            case DIRECTIONAL -> {
                result.put(Direction.UP, base);
                result.put(Direction.DOWN, transform(base, point -> transformPoint(point, Direction.DOWN)));
                result.put(Direction.NORTH, transform(base, point -> transformPoint(point, Direction.NORTH)));
                result.put(Direction.EAST, transform(base, point -> transformPoint(point, Direction.EAST)));
                result.put(Direction.SOUTH, transform(base, point -> transformPoint(point, Direction.SOUTH)));
                result.put(Direction.WEST, transform(base, point -> transformPoint(point, Direction.WEST)));
            }
        }
        return Map.copyOf(result);
    }

    static OutlineGeometry transform(OutlineGeometry geometry, UnaryOperator<Vec3> pointTransform) {
        return OutlineGeometry.of(geometry.lines().stream()
                .map(line -> new OutlineGeometry.Line(
                        pointTransform.apply(line.from()),
                        pointTransform.apply(line.to())
                ))
                .toList());
    }

    static Vec3 rotateClockwise(Vec3 point) {
        return new Vec3(16.0D - point.z, point.y, point.x);
    }

    static Vec3 transformPoint(
            Vec3 point,
            Direction direction
    ) {
        return switch (direction) {
            case UP -> point;
            case DOWN -> new Vec3(point.x, 16.0D - point.y, 16.0D - point.z);
            case NORTH -> new Vec3(point.x, point.z, 16.0D - point.y);
            case EAST -> new Vec3(point.y, point.z, point.x);
            case SOUTH -> new Vec3(16.0D - point.x, point.z, point.y);
            case WEST -> new Vec3(16.0D - point.y, point.z, 16.0D - point.x);
        };
    }

    private static Direction directionFor(BlockState state, Orientation orientation) {
        return switch (orientation) {
            case FIXED -> Direction.UP;
            case HORIZONTAL -> state.getValue(HorizontalDirectionalBlock.FACING);
            case DIRECTIONAL -> state.getValue(DirectionalBlock.FACING);
        };
    }

    private static <V> Map<Block, V> immutableIdentityMap(Map<Block, V> source) {
        Map<Block, V> copy = new IdentityHashMap<>(source);
        return Collections.unmodifiableMap(copy);
    }

    enum Orientation {
        FIXED,
        HORIZONTAL,
        DIRECTIONAL
    }

    record Definition(Block block, Identifier model, Orientation orientation) {
        private Direction direction(BlockState state) {
            return directionFor(state, orientation);
        }
    }

    public record Prepared(Map<Block, Map<Direction, OutlineGeometry>> outlines) {
    }
}
