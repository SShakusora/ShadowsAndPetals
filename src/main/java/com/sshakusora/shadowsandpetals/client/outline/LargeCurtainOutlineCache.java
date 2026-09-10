package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LargeCurtainBlock;
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
 * Resource-reloadable selection outlines for the four quadrants of a large
 * curtain. The static white master models contain the visible folded fabric
 * and rail; colored variants only override textures and share their geometry.
 */
public final class LargeCurtainOutlineCache
        extends SimplePreparableReloadListener<LargeCurtainOutlineCache.Prepared> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier RELOAD_ID = ShadowsAndPetals.asResource("large_curtain_outlines");
    private static final LargeCurtainOutlineCache INSTANCE = new LargeCurtainOutlineCache();

    private volatile Map<Pose, Map<Direction, OutlineGeometry>> outlines = Map.of();

    private LargeCurtainOutlineCache() {
    }

    /**
     * Registers one provider for every dye variant and installs the model
     * outline reload listener.
     */
    public static void register(AddClientReloadListenersEvent event) {
        for (DyeColor color : DyeColor.values()) {
            BlockOutlineRegistry.register(
                    BlockRegistry.LARGE_CURTAINS.get(color).get(),
                    LargeCurtainOutlineCache::getOutline
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
            if (base != null) {
                prepared.put(pose, buildDirections(base));
            }
        }
        return new Prepared(Map.copyOf(prepared));
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        outlines = prepared.outlines();
        LOGGER.debug("Loaded static model outlines for {} large curtain poses", outlines.size());
    }

    @Nullable
    private static OutlineGeometry load(ResourceManager manager, Pose pose) {
        Identifier modelId = outlineModelId(pose);
        Resource resource = manager.getResource(modelId).orElseThrow(() ->
                new IllegalArgumentException("Missing large curtain outline model " + modelId));
        try (Reader reader = resource.openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            OutlineGeometry geometry = RockeryOutlineGeometry.fromModel(model);
            if (geometry == null || geometry.lines().isEmpty()) {
                if (pose.emptyModel) {
                    return null;
                }
                throw new IllegalArgumentException(
                        "Large curtain outline model has no visible geometry " + modelId
                );
            }
            return geometry;
        } catch (IOException | RuntimeException exception) {
            if (pose.emptyModel && exception instanceof IllegalArgumentException
                    && exception.getMessage() != null
                    && exception.getMessage().contains("has no visible geometry")) {
                return null;
            }
            throw new IllegalArgumentException(
                    "Failed to load large curtain outline model " + modelId, exception
            );
        }
    }

    static Identifier outlineModelId(Pose pose) {
        return ShadowsAndPetals.asResource(
                "models/block/large_curtain/static/" + pose.modelPath + ".json"
        );
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
        UPPER_RIGHT_OUTER_CLOSED(
                DoubleBlockHalf.UPPER, LargeCurtainBlock.Column.OUTER, CurtainBlock.Side.RIGHT, false,
                "right/closed/white/upper_outer", false
        ),
        UPPER_RIGHT_INNER_CLOSED(
                DoubleBlockHalf.UPPER, LargeCurtainBlock.Column.INNER, CurtainBlock.Side.RIGHT, false,
                "right/closed/white/upper_inner", false
        ),
        LOWER_RIGHT_OUTER_CLOSED(
                DoubleBlockHalf.LOWER, LargeCurtainBlock.Column.OUTER, CurtainBlock.Side.RIGHT, false,
                "right/closed/white/lower_outer", false
        ),
        LOWER_RIGHT_INNER_CLOSED(
                DoubleBlockHalf.LOWER, LargeCurtainBlock.Column.INNER, CurtainBlock.Side.RIGHT, false,
                "right/closed/white/lower_inner", false
        ),
        UPPER_LEFT_OUTER_CLOSED(
                DoubleBlockHalf.UPPER, LargeCurtainBlock.Column.OUTER, CurtainBlock.Side.LEFT, false,
                "left/closed/white/upper_outer", false
        ),
        UPPER_LEFT_INNER_CLOSED(
                DoubleBlockHalf.UPPER, LargeCurtainBlock.Column.INNER, CurtainBlock.Side.LEFT, false,
                "left/closed/white/upper_inner", false
        ),
        LOWER_LEFT_OUTER_CLOSED(
                DoubleBlockHalf.LOWER, LargeCurtainBlock.Column.OUTER, CurtainBlock.Side.LEFT, false,
                "left/closed/white/lower_outer", false
        ),
        LOWER_LEFT_INNER_CLOSED(
                DoubleBlockHalf.LOWER, LargeCurtainBlock.Column.INNER, CurtainBlock.Side.LEFT, false,
                "left/closed/white/lower_inner", false
        ),
        UPPER_RIGHT_OUTER_OPEN(
                DoubleBlockHalf.UPPER, LargeCurtainBlock.Column.OUTER, CurtainBlock.Side.RIGHT, true,
                "right/open/white/upper_outer", false
        ),
        UPPER_RIGHT_INNER_OPEN(
                DoubleBlockHalf.UPPER, LargeCurtainBlock.Column.INNER, CurtainBlock.Side.RIGHT, true,
                "right/open/white/upper_inner", false
        ),
        LOWER_RIGHT_OUTER_OPEN(
                DoubleBlockHalf.LOWER, LargeCurtainBlock.Column.OUTER, CurtainBlock.Side.RIGHT, true,
                "right/open/white/lower_outer", true
        ),
        LOWER_RIGHT_INNER_OPEN(
                DoubleBlockHalf.LOWER, LargeCurtainBlock.Column.INNER, CurtainBlock.Side.RIGHT, true,
                "right/open/white/lower_inner", false
        ),
        UPPER_LEFT_OUTER_OPEN(
                DoubleBlockHalf.UPPER, LargeCurtainBlock.Column.OUTER, CurtainBlock.Side.LEFT, true,
                "left/open/white/upper_outer", false
        ),
        UPPER_LEFT_INNER_OPEN(
                DoubleBlockHalf.UPPER, LargeCurtainBlock.Column.INNER, CurtainBlock.Side.LEFT, true,
                "left/open/white/upper_inner", false
        ),
        LOWER_LEFT_OUTER_OPEN(
                DoubleBlockHalf.LOWER, LargeCurtainBlock.Column.OUTER, CurtainBlock.Side.LEFT, true,
                "left/open/white/lower_outer", true
        ),
        LOWER_LEFT_INNER_OPEN(
                DoubleBlockHalf.LOWER, LargeCurtainBlock.Column.INNER, CurtainBlock.Side.LEFT, true,
                "left/open/white/lower_inner", false
        );

        private final DoubleBlockHalf half;
        private final LargeCurtainBlock.Column column;
        private final CurtainBlock.Side side;
        private final boolean open;
        private final String modelPath;
        private final boolean emptyModel;

        Pose(
                DoubleBlockHalf half,
                LargeCurtainBlock.Column column,
                CurtainBlock.Side side,
                boolean open,
                String modelPath,
                boolean emptyModel
        ) {
            this.half = half;
            this.column = column;
            this.side = side;
            this.open = open;
            this.modelPath = modelPath;
            this.emptyModel = emptyModel;
        }

        private static Pose from(BlockState state) {
            DoubleBlockHalf half = state.getValue(CurtainBlock.HALF);
            LargeCurtainBlock.Column column = state.getValue(LargeCurtainBlock.COLUMN);
            CurtainBlock.Side side = state.getValue(CurtainBlock.SIDE);
            boolean open = state.getValue(CurtainBlock.OPEN);
            for (Pose pose : values()) {
                if (pose.half == half
                        && pose.column == column
                        && pose.side == side
                        && pose.open == open) {
                    return pose;
                }
            }
            throw new IllegalStateException(
                    "No large curtain outline pose for " + half + ", " + column + ", " + side + ", " + open
            );
        }
    }

    public record Prepared(Map<Pose, Map<Direction, OutlineGeometry>> outlines) {
    }
}
