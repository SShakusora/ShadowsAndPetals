package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.AbstractCurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainSide;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;

/** Reloadable outlines for the single-cell curtain. */
public final class CurtainOutlineCache
        extends SimplePreparableReloadListener<CurtainOutlineCache.Prepared> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CurtainOutlineCache INSTANCE = new CurtainOutlineCache();
    private volatile Map<Pose, Map<Direction, OutlineGeometry>> outlines = Map.of();

    private CurtainOutlineCache() {
    }

    public static void register(AddClientReloadListenersEvent event) {
        for (DyeColor color : DyeColor.values()) {
            BlockOutlineRegistry.register(
                    BlockRegistry.CURTAINS.get(color).get(), CurtainOutlineCache::getOutline
            );
        }
        event.addListener(ShadowsAndPetals.asResource("curtain_outlines"), INSTANCE);
    }

    private static @Nullable OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        Map<Direction, OutlineGeometry> byDirection = INSTANCE.outlines.get(Pose.from(state));
        return byDirection == null ? null : byDirection.get(state.getValue(AbstractCurtainBlock.FACING));
    }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        EnumMap<Pose, Map<Direction, OutlineGeometry>> prepared = new EnumMap<>(Pose.class);
        for (Pose pose : Pose.values()) {
            prepared.put(pose, buildDirections(load(manager, pose)));
        }
        return new Prepared(Map.copyOf(prepared));
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        outlines = prepared.outlines();
        LOGGER.debug("Loaded static model outlines for {} curtain poses", outlines.size());
    }

    private static OutlineGeometry load(ResourceManager manager, Pose pose) {
        Identifier modelId = ShadowsAndPetals.asResource(
                "models/block/curtain/" + pose.modelPath + ".json");
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
        RIGHT_CLOSED(CurtainSide.RIGHT, false, "static/right/closed/white/upper"),
        RIGHT_OPEN(CurtainSide.RIGHT, true, "static/right/open/white/upper"),
        LEFT_CLOSED(CurtainSide.LEFT, false, "static/left/closed/white/upper"),
        LEFT_OPEN(CurtainSide.LEFT, true, "static/left/open/white/upper");

        private final CurtainSide side;
        private final boolean open;
        private final String modelPath;

        Pose(CurtainSide side, boolean open, String modelPath) {
            this.side = side;
            this.open = open;
            this.modelPath = modelPath;
        }

        private static Pose from(BlockState state) {
            CurtainSide side = state.getValue(AbstractCurtainBlock.SIDE);
            boolean open = state.getValue(AbstractCurtainBlock.OPEN);
            for (Pose pose : values()) {
                if (pose.side == side && pose.open == open) {
                    return pose;
                }
            }
            throw new IllegalStateException("No curtain outline pose for " + state);
        }
    }

    public record Prepared(Map<Pose, Map<Direction, OutlineGeometry>> outlines) {
    }
}
