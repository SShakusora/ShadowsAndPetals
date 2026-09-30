package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.AbstractCurtainBlock;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.CurtainSide;
import com.sshakusora.shadowsandpetals.block.decoration.curtain.LongCurtainBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;

/** Reloadable outlines extracted from the two-half curtain model JSON files. */
public final class LongCurtainOutlineCache extends SimplePreparableReloadListener<LongCurtainOutlineCache.Prepared> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation RELOAD_ID = ShadowsAndPetals.asResource("long_curtain_outlines");
    private static final LongCurtainOutlineCache INSTANCE = new LongCurtainOutlineCache();
    private volatile Map<Pose, Map<Direction, OutlineGeometry>> outlines = Map.of();

    private LongCurtainOutlineCache() {
    }

    public static void register(RegisterClientReloadListenersEvent event) {
        for (DyeColor color : DyeColor.values()) {
            BlockOutlineRegistry.register(BlockRegistry.LONG_CURTAINS.get(color).get(), LongCurtainOutlineCache::getOutline);
        }
        event.registerReloadListener(INSTANCE);
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
        ResourceLocation modelId = ShadowsAndPetals.asResource("models/block/long_curtain/" + pose.modelPath + ".json");
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
        UPPER_RIGHT_CLOSED(DoubleBlockHalf.UPPER, CurtainSide.RIGHT, false, "static/right/closed/white/upper"),
        UPPER_RIGHT_OPEN(DoubleBlockHalf.UPPER, CurtainSide.RIGHT, true, "static/right/open/white/upper"),
        UPPER_LEFT_CLOSED(DoubleBlockHalf.UPPER, CurtainSide.LEFT, false, "static/left/closed/white/upper"),
        UPPER_LEFT_OPEN(DoubleBlockHalf.UPPER, CurtainSide.LEFT, true, "static/left/open/white/upper"),
        LOWER_RIGHT_CLOSED(DoubleBlockHalf.LOWER, CurtainSide.RIGHT, false, "static/right/closed/white/lower"),
        LOWER_RIGHT_OPEN(DoubleBlockHalf.LOWER, CurtainSide.RIGHT, true, "static/right/open/white/lower"),
        LOWER_LEFT_CLOSED(DoubleBlockHalf.LOWER, CurtainSide.LEFT, false, "static/left/closed/white/lower"),
        LOWER_LEFT_OPEN(DoubleBlockHalf.LOWER, CurtainSide.LEFT, true, "static/left/open/white/lower");

        private final DoubleBlockHalf half;
        private final CurtainSide side;
        private final boolean open;
        private final String modelPath;

        Pose(DoubleBlockHalf half, CurtainSide side, boolean open, String modelPath) {
            this.half = half;
            this.side = side;
            this.open = open;
            this.modelPath = modelPath;
        }

        private static Pose from(BlockState state) {
            DoubleBlockHalf half = state.getValue(LongCurtainBlock.HALF);
            CurtainSide side = state.getValue(AbstractCurtainBlock.SIDE);
            boolean open = state.getValue(AbstractCurtainBlock.OPEN);
            for (Pose pose : values()) {
                if (pose.half == half && pose.side == side && pose.open == open) {
                    return pose;
                }
            }
            throw new IllegalStateException("No curtain outline pose for " + state);
        }
    }

    public record Prepared(Map<Pose, Map<Direction, OutlineGeometry>> outlines) {
    }
}
