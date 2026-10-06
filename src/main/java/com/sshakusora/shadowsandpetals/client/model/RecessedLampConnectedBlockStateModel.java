package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampBlock;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampCompositeBlock;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampConnection;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Baked-model wrapper that assembles the four connected recessed-lamp corners. */
public final class RecessedLampConnectedBlockStateModel extends BakedModelWrapper<BakedModel>
        implements IDynamicBakedModel {
    private static final int VERTEX_STRIDE = 8;
    private static final ModelProperty<ConnectionData> CONNECTION_DATA = new ModelProperty<>();
    private static final ConcurrentHashMap<QuadCacheKey, List<BakedQuad>> QUAD_CACHE =
            new ConcurrentHashMap<>();

    private final Block expectedBlock;

    public RecessedLampConnectedBlockStateModel(Block expectedBlock, BakedModel delegate) {
        super(delegate);
        this.expectedBlock = expectedBlock;
    }

    public static void clearCache() {
        QUAD_CACHE.clear();
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData originalData) {
        ModelData base = super.getModelData(level, pos, state, originalData);
        if (state.getBlock() != expectedBlock) {
            return base;
        }
        return addConnectionData(level, pos, state, base);
    }

    public static ModelData addConnectionData(BlockAndTintGetter level, BlockPos pos,
                                              BlockState state, ModelData base) {
        if (!RecessedLampConnection.isLamp(state)) {
            return base;
        }
        boolean north = RecessedLampConnection.hasConnection(level, pos, state, Direction.NORTH);
        boolean east = RecessedLampConnection.hasConnection(level, pos, state, Direction.EAST);
        boolean south = RecessedLampConnection.hasConnection(level, pos, state, Direction.SOUTH);
        boolean west = RecessedLampConnection.hasConnection(level, pos, state, Direction.WEST);
        boolean northWest = RecessedLampConnection.hasDiagonalConnection(
                level, pos, state, Direction.NORTH, Direction.WEST);
        boolean northEast = RecessedLampConnection.hasDiagonalConnection(
                level, pos, state, Direction.NORTH, Direction.EAST);
        boolean southWest = RecessedLampConnection.hasDiagonalConnection(
                level, pos, state, Direction.SOUTH, Direction.WEST);
        boolean southEast = RecessedLampConnection.hasDiagonalConnection(
                level, pos, state, Direction.SOUTH, Direction.EAST);

        ConnectionData data = new ConnectionData(
                2 | (west ? 1 : 0) | (north ? 8 : 0) | (northWest ? 4 : 0),
                1 | (east ? 2 : 0) | (north ? 4 : 0) | (northEast ? 8 : 0),
                8 | (west ? 4 : 0) | (south ? 2 : 0) | (southWest ? 1 : 0),
                4 | (east ? 8 : 0) | (south ? 1 : 0) | (southEast ? 2 : 0)
        );
        return base.derive().with(CONNECTION_DATA, data).build();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        if (state.getBlock() == expectedBlock) {
            return ChunkRenderTypeSet.of(RenderType.cutout());
        }
        return super.getRenderTypes(state, random, data);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, @Nullable Direction face, RandomSource random) {
        return BakedModelSupport.getQuads(originalModel, state, face, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, @Nullable Direction face, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        if (state.getBlock() != expectedBlock) {
            return BakedModelSupport.getQuads(originalModel, state, face, random, data, renderType);
        }
        return getConnectedLampQuads(state, face, random, data, renderType, originalModel);
    }

    public static List<BakedQuad> getConnectedLampQuads(
            BlockState state,
            @Nullable Direction face,
            RandomSource random,
            ModelData data,
            @Nullable RenderType renderType,
            BakedModel fallback
    ) {
        if (!RecessedLampConnection.isLamp(state)) {
            return BakedModelSupport.getQuads(fallback, state, face, random, data, renderType);
        }

        // The hand-authored lamp models use cutout textures. Composite models
        // can still render their stored slab in the solid pass.
        if (renderType != null && renderType != RenderType.cutout()) {
            return List.of();
        }

        ConnectionData connection = data.get(CONNECTION_DATA);
        if (connection == null) {
            return BakedModelSupport.getQuads(fallback, state, face, random, data, renderType);
        }

        boolean ceiling = RecessedLampConnection.face(state) == Direction.DOWN;
        boolean lit = state.getValue(RecessedLampBlock.LIT);
        float yTranslation = yTranslation(state);
        List<BakedQuad> result = new ArrayList<>();
        appendCorner(result, connection.northWest(), Corner.NORTH_WEST, ceiling, lit,
                state, face, random, yTranslation);
        appendCorner(result, connection.northEast(), Corner.NORTH_EAST, ceiling, lit,
                state, face, random, yTranslation);
        appendCorner(result, connection.southWest(), Corner.SOUTH_WEST, ceiling, lit,
                state, face, random, yTranslation);
        appendCorner(result, connection.southEast(), Corner.SOUTH_EAST, ceiling, lit,
                state, face, random, yTranslation);
        return result;
    }

    private static void appendCorner(List<BakedQuad> result, int modelIndex, Corner corner,
                                     boolean ceiling, boolean lit, BlockState state,
                                     @Nullable Direction face, RandomSource random, float yTranslation) {
        BlockModelRegistry.RecessedLampModelKey key =
                new BlockModelRegistry.RecessedLampModelKey(ceiling, lit, modelIndex);
        BakedModel model = BlockModelRegistry.getRecessedLampModel(key);
        if (model == null) {
            return;
        }

        QuadCacheKey cacheKey = new QuadCacheKey(model, face, corner, Float.floatToIntBits(yTranslation));
        List<BakedQuad> clipped = QUAD_CACHE.get(cacheKey);
        if (clipped == null) {
            List<BakedQuad> source = BakedModelSupport.getQuads(
                    model, state, face, random, ModelData.EMPTY, null);
            clipped = clipAndTranslate(source, corner, yTranslation);
            List<BakedQuad> previous = QUAD_CACHE.putIfAbsent(cacheKey, clipped);
            if (previous != null) {
                clipped = previous;
            }
        }
        result.addAll(clipped);
    }

    private static float yTranslation(BlockState state) {
        RecessedLampBlock.Mount mount = state.getValue(RecessedLampBlock.MOUNT);
        boolean composite = state.getBlock() instanceof RecessedLampCompositeBlock;
        return switch (mount) {
            case FLOOR, CEILING -> 0.0F;
            case FLOOR_SLAB -> composite ? 0.5F : -0.5F;
            case CEILING_SLAB -> composite ? -0.5F : 0.5F;
        };
    }

    private static List<BakedQuad> clipAndTranslate(List<BakedQuad> source, Corner corner,
                                                      float yTranslation) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<BakedQuad> result = new ArrayList<>();
        for (BakedQuad quad : source) {
            List<Vertex> polygon = new ArrayList<>(4);
            int[] vertices = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                polygon.add(Vertex.read(vertices, vertex * VERTEX_STRIDE));
            }
            polygon = clip(polygon, Axis.X, corner.minX, true);
            polygon = clip(polygon, Axis.X, corner.maxX, false);
            polygon = clip(polygon, Axis.Z, corner.minZ, true);
            polygon = clip(polygon, Axis.Z, corner.maxZ, false);
            if (polygon.size() < 3) {
                continue;
            }
            for (Vertex vertex : polygon) {
                vertex.x += corner.offsetX;
                vertex.z += corner.offsetZ;
                vertex.y += yTranslation;
            }
            if (polygon.size() == 3) {
                polygon.add(polygon.get(2));
            }
            if (polygon.size() == 4) {
                result.add(newQuad(quad, polygon, 1, 2, 3));
            } else {
                for (int index = 1; index + 1 < polygon.size(); index++) {
                    result.add(newQuad(quad, polygon, index, index + 1, index + 1));
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<Vertex> clip(List<Vertex> input, Axis axis, float boundary, boolean keepGreater) {
        if (input.isEmpty()) {
            return input;
        }
        List<Vertex> output = new ArrayList<>();
        Vertex previous = input.get(input.size() - 1);
        boolean previousInside = inside(previous, axis, boundary, keepGreater);
        for (Vertex current : input) {
            boolean currentInside = inside(current, axis, boundary, keepGreater);
            if (currentInside != previousInside) {
                float previousCoordinate = coordinate(previous, axis);
                float currentCoordinate = coordinate(current, axis);
                float denominator = currentCoordinate - previousCoordinate;
                float amount = denominator == 0.0F
                        ? 0.0F : (boundary - previousCoordinate) / denominator;
                output.add(previous.lerp(current, amount));
            }
            if (currentInside) {
                output.add(current);
            }
            previous = current;
            previousInside = currentInside;
        }
        return output;
    }

    private static boolean inside(Vertex vertex, Axis axis, float boundary, boolean keepGreater) {
        float value = coordinate(vertex, axis);
        return keepGreater ? value >= boundary - 0.00001F : value <= boundary + 0.00001F;
    }

    private static float coordinate(Vertex vertex, Axis axis) {
        return axis == Axis.X ? vertex.x : vertex.z;
    }

    private static BakedQuad newQuad(BakedQuad source, List<Vertex> polygon,
                                     int second, int third, int fourth) {
        int[] vertices = new int[VERTEX_STRIDE * 4];
        polygon.get(0).write(vertices, 0);
        polygon.get(second).write(vertices, VERTEX_STRIDE);
        polygon.get(third).write(vertices, VERTEX_STRIDE * 2);
        polygon.get(fourth).write(vertices, VERTEX_STRIDE * 3);
        boolean emissive = source.getSprite().contents().name().getPath().equals("block/recessed_lamp/on");
        if (emissive) {
            for (int vertex = 0; vertex < 4; vertex++) {
                vertices[vertex * VERTEX_STRIDE + 6] = 0x00F000F0;
            }
        }
        return new BakedQuad(vertices, source.getTintIndex(), source.getDirection(),
                source.getSprite(), !emissive && source.isShade(), !emissive && source.hasAmbientOcclusion());
    }

    private enum Axis {
        X,
        Z
    }

    private enum Corner {
        NORTH_WEST(0.5F, 1.0F, 0.5F, 1.0F, -0.5F, -0.5F),
        NORTH_EAST(0.0F, 0.5F, 0.5F, 1.0F, 0.5F, -0.5F),
        SOUTH_WEST(0.5F, 1.0F, 0.0F, 0.5F, -0.5F, 0.5F),
        SOUTH_EAST(0.0F, 0.5F, 0.0F, 0.5F, 0.5F, 0.5F);

        private final float minX;
        private final float maxX;
        private final float minZ;
        private final float maxZ;
        private final float offsetX;
        private final float offsetZ;

        Corner(float minX, float maxX, float minZ, float maxZ, float offsetX, float offsetZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
        }
    }

    private record ConnectionData(int northWest, int northEast, int southWest, int southEast) {
    }

    private record QuadCacheKey(BakedModel model, @Nullable Direction face,
                                Corner corner, int yTranslationBits) {
    }

    private static final class Vertex {
        private float x;
        private float y;
        private float z;
        private final int color;
        private final float u;
        private final float v;
        private final int light;
        private final int normal;

        private Vertex(float x, float y, float z, int color, float u, float v, int light, int normal) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.u = u;
            this.v = v;
            this.light = light;
            this.normal = normal;
        }

        private static Vertex read(int[] data, int offset) {
            return new Vertex(
                    Float.intBitsToFloat(data[offset]),
                    Float.intBitsToFloat(data[offset + 1]),
                    Float.intBitsToFloat(data[offset + 2]),
                    data[offset + 3],
                    Float.intBitsToFloat(data[offset + 4]),
                    Float.intBitsToFloat(data[offset + 5]),
                    data[offset + 6],
                    data[offset + 7]
            );
        }

        private Vertex lerp(Vertex other, float amount) {
            return new Vertex(
                    lerp(x, other.x, amount),
                    lerp(y, other.y, amount),
                    lerp(z, other.z, amount),
                    color,
                    lerp(u, other.u, amount),
                    lerp(v, other.v, amount),
                    light,
                    normal
            );
        }

        private void write(int[] data, int offset) {
            data[offset] = Float.floatToRawIntBits(x);
            data[offset + 1] = Float.floatToRawIntBits(y);
            data[offset + 2] = Float.floatToRawIntBits(z);
            data[offset + 3] = color;
            data[offset + 4] = Float.floatToRawIntBits(u);
            data[offset + 5] = Float.floatToRawIntBits(v);
            data[offset + 6] = light;
            data[offset + 7] = normal;
        }

        private static float lerp(float first, float second, float amount) {
            return first + (second - first) * amount;
        }
    }
}
