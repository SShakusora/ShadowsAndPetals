package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.CurtainBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
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
 * Resource-reloadable selection outlines for the static curtain block models.
 *
 * <p>The curtain's collision shape is deliberately a thin, axis-aligned slice,
 * while its block-state model contains the folded panels and decorative rail.
 * This cache reads the eight white model masters (half, side and open state)
 * once per client resource reload and derives the four horizontal facings from
 * each master. Colored curtain models only override textures, so they share the
 * same geometry.</p>
 */
public final class CurtainOutlineCache
        extends SimplePreparableReloadListener<CurtainOutlineCache.Prepared> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier RELOAD_ID = ShadowsAndPetals.asResource("curtain_outlines");
    private static final CurtainOutlineCache INSTANCE = new CurtainOutlineCache();

    private volatile Map<Pose, Map<Direction, OutlineGeometry>> outlines = Map.of();

    private CurtainOutlineCache() {
    }

    /**
     * Registers one provider for every dye variant and installs the model
     * outline reload listener.
     */
    public static void register(AddClientReloadListenersEvent event) {
        for (DyeColor color : DyeColor.values()) {
            BlockOutlineRegistry.register(
                    BlockRegistry.CURTAINS.get(color).get(),
                    CurtainOutlineCache::getOutline
            );
        }
        event.addListener(RELOAD_ID, INSTANCE);
    }

    @Nullable
    private static OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        Pose pose = Pose.from(state);
        Map<Direction, OutlineGeometry> byDirection = INSTANCE.outlines.get(pose);
        return byDirection == null ? null : byDirection.get(state.getValue(CurtainBlock.FACING));
    }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        EnumMap<Pose, Map<Direction, OutlineGeometry>> prepared = new EnumMap<>(Pose.class);
        for (Pose pose : Pose.values()) {
            OutlineGeometry base = load(manager, pose);
            prepared.put(pose, buildDirections(base));
        }
        return new Prepared(Map.copyOf(prepared));
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        outlines = prepared.outlines();
        LOGGER.debug("Loaded static model outlines for {} curtain poses", outlines.size());
    }

    private static OutlineGeometry load(ResourceManager manager, Pose pose) {
        Identifier modelId = ShadowsAndPetals.asResource("models/block/curtain/" + pose.modelName + ".json");
        Resource resource = manager.getResource(modelId).orElseThrow(() ->
                new IllegalArgumentException("Missing curtain outline model " + modelId));
        try (Reader reader = resource.openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(model);
            if (geometry == null || geometry.lines().isEmpty()) {
                throw new IllegalArgumentException("Curtain outline model has no visible geometry " + modelId);
            }
            return geometry;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Failed to load curtain outline model " + modelId, exception);
        }
    }

    static Map<Direction, OutlineGeometry> buildDirections(OutlineGeometry base) {
        EnumMap<Direction, OutlineGeometry> result = new EnumMap<>(Direction.class);
        result.put(Direction.NORTH, base);
        result.put(Direction.EAST, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.NORTH)));
        result.put(Direction.SOUTH, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.EAST)));
        result.put(Direction.WEST, RockeryOutlineGeometry.rotateClockwise(result.get(Direction.SOUTH)));
        return Map.copyOf(result);
    }

    enum Pose {
        UPPER_RIGHT_CLOSED(DoubleBlockHalf.UPPER, CurtainBlock.Side.RIGHT, false, "curtain_upper_right"),
        UPPER_RIGHT_OPEN(DoubleBlockHalf.UPPER, CurtainBlock.Side.RIGHT, true, "curtain_upper_right_open"),
        UPPER_LEFT_CLOSED(DoubleBlockHalf.UPPER, CurtainBlock.Side.LEFT, false, "curtain_upper_left"),
        UPPER_LEFT_OPEN(DoubleBlockHalf.UPPER, CurtainBlock.Side.LEFT, true, "curtain_upper_left_open"),
        LOWER_RIGHT_CLOSED(DoubleBlockHalf.LOWER, CurtainBlock.Side.RIGHT, false, "curtain_lower_right"),
        LOWER_RIGHT_OPEN(DoubleBlockHalf.LOWER, CurtainBlock.Side.RIGHT, true, "curtain_lower_right_open"),
        LOWER_LEFT_CLOSED(DoubleBlockHalf.LOWER, CurtainBlock.Side.LEFT, false, "curtain_lower_left"),
        LOWER_LEFT_OPEN(DoubleBlockHalf.LOWER, CurtainBlock.Side.LEFT, true, "curtain_lower_left_open");

        private final DoubleBlockHalf half;
        private final CurtainBlock.Side side;
        private final boolean open;
        private final String modelName;

        Pose(DoubleBlockHalf half, CurtainBlock.Side side, boolean open, String modelName) {
            this.half = half;
            this.side = side;
            this.open = open;
            this.modelName = modelName;
        }

        private static Pose from(BlockState state) {
            DoubleBlockHalf half = state.getValue(CurtainBlock.HALF);
            CurtainBlock.Side side = state.getValue(CurtainBlock.SIDE);
            boolean open = state.getValue(CurtainBlock.OPEN);
            for (Pose pose : values()) {
                if (pose.half == half && pose.side == side && pose.open == open) {
                    return pose;
                }
            }
            throw new IllegalStateException("No curtain outline pose for " + half + ", " + side + ", " + open);
        }
    }

    public record Prepared(Map<Pose, Map<Direction, OutlineGeometry>> outlines) {
    }
}
