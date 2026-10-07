package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampBlock;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampCompositeBlock;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampConnection;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Four intersection quadrants, clipped and translated into the owning lamp block. */
public final class RecessedLampConnectedBlockStateModel extends DelegateBlockStateModel implements DynamicBlockStateModel {
    private static final ConcurrentHashMap<PartKey, BlockStateModelPart> PARTS = new ConcurrentHashMap<>();
    private final Block expectedBlock;

    public RecessedLampConnectedBlockStateModel(Block expectedBlock, BlockStateModel delegate) {
        super(delegate);
        this.expectedBlock = expectedBlock;
    }

    public static void clearCache() {
        PARTS.clear();
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (state == null || state.getBlock() != expectedBlock) {
            return this;
        }
        return new GeometryKey(state, connections(level, pos, state));
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                             List<BlockStateModelPart> parts) {
        if (state == null || state.getBlock() != expectedBlock) {
            delegate.collectParts(random, parts);
            return;
        }
        RecessedLampConnection.Corners corners = connections(level, pos, state);
        boolean ceiling = RecessedLampConnection.face(state) == Direction.DOWN;
        boolean lit = state.getValue(RecessedLampBlock.LIT);
        float translation = yTranslation(state);
        append(parts, corners.northWest(), Corner.NORTH_WEST, ceiling, lit, translation, random);
        append(parts, corners.northEast(), Corner.NORTH_EAST, ceiling, lit, translation, random);
        append(parts, corners.southWest(), Corner.SOUTH_WEST, ceiling, lit, translation, random);
        append(parts, corners.southEast(), Corner.SOUTH_EAST, ceiling, lit, translation, random);
    }

    private static RecessedLampConnection.Corners connections(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return RecessedLampConnection.corners(
                RecessedLampConnection.hasConnection(level, pos, state, Direction.NORTH),
                RecessedLampConnection.hasConnection(level, pos, state, Direction.EAST),
                RecessedLampConnection.hasConnection(level, pos, state, Direction.SOUTH),
                RecessedLampConnection.hasConnection(level, pos, state, Direction.WEST),
                RecessedLampConnection.hasDiagonalConnection(level, pos, state, Direction.NORTH, Direction.WEST),
                RecessedLampConnection.hasDiagonalConnection(level, pos, state, Direction.NORTH, Direction.EAST),
                RecessedLampConnection.hasDiagonalConnection(level, pos, state, Direction.SOUTH, Direction.WEST),
                RecessedLampConnection.hasDiagonalConnection(level, pos, state, Direction.SOUTH, Direction.EAST));
    }

    static float yTranslation(BlockState state) {
        boolean composite = state.getBlock() instanceof RecessedLampCompositeBlock;
        return switch (state.getValue(RecessedLampBlock.MOUNT)) {
            case FLOOR, CEILING -> 0.0F;
            case FLOOR_SLAB -> composite ? 0.5F : -0.5F;
            case CEILING_SLAB -> composite ? -0.5F : 0.5F;
        };
    }

    private static void append(List<BlockStateModelPart> output, int index, Corner corner,
                               boolean ceiling, boolean lit, float translation, RandomSource random) {
        BlockStateModel model = BlockModelRegistry.getRecessedLampModel(
                new BlockModelRegistry.RecessedLampModelKey(ceiling, lit, index));
        if (model == null) {
            return;
        }
        List<BlockStateModelPart> source = new ArrayList<>();
        model.collectParts(random, source);
        for (BlockStateModelPart part : source) {
            output.add(PARTS.computeIfAbsent(new PartKey(part, corner, translation),
                    key -> new ClippedPart(key.part(), key.corner(), key.translation())));
        }
    }

    private record GeometryKey(BlockState state, RecessedLampConnection.Corners corners) {
    }

    private record PartKey(BlockStateModelPart part, Corner corner, float translation) {
    }

    enum Corner {
        NORTH_WEST(0.5F, 1.0F, 0.5F, 1.0F, -0.5F, -0.5F),
        NORTH_EAST(0.0F, 0.5F, 0.5F, 1.0F, 0.5F, -0.5F),
        SOUTH_WEST(0.5F, 1.0F, 0.0F, 0.5F, -0.5F, 0.5F),
        SOUTH_EAST(0.0F, 0.5F, 0.0F, 0.5F, 0.5F, 0.5F);

        final float minX, maxX, minZ, maxZ, offsetX, offsetZ;

        Corner(float minX, float maxX, float minZ, float maxZ, float offsetX, float offsetZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
        }
    }

    private static final class ClippedPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final List<BakedQuad> quads;
        private final int flags;

        ClippedPart(BlockStateModelPart delegate, Corner corner, float translation) {
            this.delegate = delegate;
            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad quad : delegate.getQuads(null)) {
                result.addAll(clipAndTranslate(quad, corner, translation));
            }
            // The source JSON has no cullfaces. Keep all output unculled, including
            // any faces a resource pack may add at an intersection's old boundary.
            for (Direction face : Direction.values()) {
                for (BakedQuad quad : delegate.getQuads(face)) {
                    result.addAll(clipAndTranslate(quad, corner, translation));
                }
            }
            quads = List.copyOf(result);
            flags = quads.stream().mapToInt(q -> q.materialInfo().flags()).reduce(0, (a, b) -> a | b);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction face) {
            return face == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public TriState ambientOcclusion() {
            return delegate.ambientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return flags;
        }
    }

    static List<BakedQuad> clipAndTranslate(BakedQuad quad, Corner corner, float translation) {
        List<Vertex> polygon = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            Vector3fc position = quad.position(i);
            polygon.add(new Vertex(position.x(), position.y(), position.z(),
                    UVPair.unpackU(quad.packedUV(i)), UVPair.unpackV(quad.packedUV(i))));
        }
        polygon = clip(polygon, true, corner.minX, true);
        polygon = clip(polygon, true, corner.maxX, false);
        polygon = clip(polygon, false, corner.minZ, true);
        polygon = clip(polygon, false, corner.maxZ, false);
        if (polygon.size() < 3) {
            return List.of();
        }
        List<Vertex> shifted = polygon.stream().map(v -> new Vertex(v.x + corner.offsetX,
                v.y + translation, v.z + corner.offsetZ, v.u, v.v)).toList();
        List<BakedQuad> result = new ArrayList<>();
        if (shifted.size() <= 4) {
            result.add(quad(quad, shifted.get(0), shifted.get(1), shifted.get(2), shifted.get(shifted.size() - 1)));
        } else {
            for (int i = 1; i + 1 < shifted.size(); i++) {
                result.add(quad(quad, shifted.get(0), shifted.get(i), shifted.get(i + 1), shifted.get(i + 1)));
            }
        }
        return List.copyOf(result);
    }

    private static List<Vertex> clip(List<Vertex> input, boolean xAxis, float boundary, boolean greater) {
        if (input.isEmpty()) {
            return input;
        }
        List<Vertex> output = new ArrayList<>();
        Vertex previous = input.getLast();
        boolean previousInside = inside(previous, xAxis, boundary, greater);
        for (Vertex current : input) {
            boolean currentInside = inside(current, xAxis, boundary, greater);
            if (currentInside != previousInside) {
                float from = coordinate(previous, xAxis);
                float amount = (boundary - from) / (coordinate(current, xAxis) - from);
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

    private static float coordinate(Vertex vertex, boolean xAxis) {
        return xAxis ? vertex.x : vertex.z;
    }

    private static boolean inside(Vertex vertex, boolean xAxis, float boundary, boolean greater) {
        float value = coordinate(vertex, xAxis);
        return greater ? value >= boundary : value <= boundary;
    }

    private static BakedQuad quad(BakedQuad source, Vertex a, Vertex b, Vertex c, Vertex d) {
        BakedQuad.MaterialInfo material = source.materialInfo();
        boolean emissive = material.sprite().contents().name().equals(ShadowsAndPetals.asResource("block/recessed_lamp/on"));
        // Match the original lamp's cutout pass while leaving the composite's
        // stored slab in its own render layer.
        if (emissive || material.layer() != ChunkSectionLayer.CUTOUT) {
            material = new BakedQuad.MaterialInfo(material.sprite(), ChunkSectionLayer.CUTOUT, material.itemRenderType(),
                    material.tintIndex(), !emissive && material.shade(), emissive ? 15 : material.lightEmission(),
                    !emissive && material.ambientOcclusion());
        }
        return new BakedQuad(a.position(), b.position(), c.position(), d.position(),
                a.uv(), b.uv(), c.uv(), d.uv(), source.direction(), material,
                source.bakedNormals(), source.bakedColors());
    }

    private record Vertex(float x, float y, float z, float u, float v) {
        Vertex lerp(Vertex other, float amount) {
            return new Vertex(x + (other.x - x) * amount, y + (other.y - y) * amount,
                    z + (other.z - z) * amount, u + (other.u - u) * amount, v + (other.v - v) * amount);
        }

        Vector3fc position() {
            return new Vector3f(x, y, z);
        }

        long uv() {
            return UVPair.pack(u, v);
        }
    }
}
