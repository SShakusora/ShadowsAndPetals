package com.sshakusora.shadowsandpetals.client.ct;

import com.sshakusora.shadowsandpetals.client.ct.CTRegistry.CTEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Connected-texture block state model. Wraps a standard cube_all model and
 * remaps quad UVs at render time so they sample the correct tile from a
 * {@code _connected.png} sprite-sheet atlas.
 */
public final class CTBlockStateModel extends DelegateBlockStateModel implements DynamicBlockStateModel {

    private final Identifier baseTextureId;
    private final Identifier connectedTextureId;
    private final CTTextureType type;

    // Lazy-resolved sprites and pre-computed UV deltas
    private volatile Sprites sprites;

    public CTBlockStateModel(BlockStateModel delegate, CTEntry entry) {
        super(delegate);
        this.baseTextureId = entry.baseTexture();
        this.connectedTextureId = entry.connectedTexture();
        this.type = entry.type();
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        Object delegateKey = delegate.createGeometryKey(level, pos, state, random);
        Map<Direction, Integer> indices = computeCTIndices(level, pos, state);
        return new GeometryKey(
                delegateKey,
                indices.getOrDefault(Direction.DOWN, -1),
                indices.getOrDefault(Direction.UP, -1),
                indices.getOrDefault(Direction.NORTH, -1),
                indices.getOrDefault(Direction.SOUTH, -1),
                indices.getOrDefault(Direction.WEST, -1),
                indices.getOrDefault(Direction.EAST, -1));
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                             RandomSource random, List<BlockStateModelPart> parts) {
        Sprites sp = ensureSprites();
        Map<Direction, Integer> ctIndices = computeCTIndices(level, pos, state);

        List<BlockStateModelPart> delegateParts = new ArrayList<>();
        delegate.collectParts(level, pos, state, random, delegateParts);

        for (BlockStateModelPart part : delegateParts) {
            parts.add(new CTPart(part, ctIndices, sp, type.getSheetSize()));
        }
    }

    private Sprites ensureSprites() {
        Sprites sp = sprites;
        if (sp != null) return sp;
        synchronized (this) {
            sp = sprites;
            if (sp != null) return sp;
            TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance()
                    .getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
            TextureAtlasSprite base = atlas.getSprite(baseTextureId);
            TextureAtlasSprite connected = atlas.getSprite(connectedTextureId);
            sp = new Sprites(base, connected);
            sprites = sp;
            return sp;
        }
    }

    private Map<Direction, Integer> computeCTIndices(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        Map<Direction, Integer> indices = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            BlockPos neighborPos = pos.relative(face);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!Block.shouldRenderFace(level, pos, state, neighborState, face))
                continue;
            CTContext ctx = buildContext(level, pos, state, face);
            indices.put(face, type.getTextureIndex(ctx));
        }
        return indices;
    }

    private CTContext buildContext(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        Axis axis = face.getAxis();
        boolean positive = face.getAxisDirection() == AxisDirection.POSITIVE;
        Direction up = axis.isHorizontal() ? Direction.UP : Direction.NORTH;
        Direction right = axis == Axis.X ? Direction.SOUTH : Direction.WEST;

        right = positive ? right.getOpposite() : right;
        if (face == Direction.DOWN) {
            up = up.getOpposite();
            right = right.getOpposite();
        }

        CTContext ctx = new CTContext();
        ctx.up    = connectsTo(level, pos, state, face, up);
        ctx.down  = connectsTo(level, pos, state, face, up.getOpposite());
        ctx.left  = connectsTo(level, pos, state, face, right.getOpposite());
        ctx.right = connectsTo(level, pos, state, face, right);

        ctx.topLeft     = ctx.up   && ctx.left  && connectsToDiag(level, pos, state, face, up, right.getOpposite());
        ctx.topRight    = ctx.up   && ctx.right && connectsToDiag(level, pos, state, face, up, right);
        ctx.bottomLeft  = ctx.down && ctx.left  && connectsToDiag(level, pos, state, face, up.getOpposite(), right.getOpposite());
        ctx.bottomRight = ctx.down && ctx.right && connectsToDiag(level, pos, state, face, up.getOpposite(), right);
        return ctx;
    }

    private boolean connectsTo(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                Direction face, Direction offset) {
        BlockPos np = pos.relative(offset);
        BlockState other = getCTBlockState(level, state, face, pos, np);
        return connectsTo(level, pos, state, np, other, face);
    }

    private boolean connectsToDiag(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                    Direction face, Direction off1, Direction off2) {
        BlockPos np = pos.relative(off1).relative(off2);
        BlockState other = getCTBlockState(level, state, face, pos, np);
        return connectsTo(level, pos, state, np, other, face);
    }

    private boolean connectsTo(BlockAndTintGetter level, BlockPos pos, BlockState state,
                               BlockPos otherPos, BlockState other, Direction face) {
        return state.getBlock() == other.getBlock()
                && !isBlocked(level, pos, state, otherPos, face);
    }

    private boolean isBlocked(BlockAndTintGetter level, BlockPos from, BlockState state, BlockPos to, Direction face) {
        BlockPos blockingPos = to.relative(face);
        BlockState blockingState = level.getBlockState(blockingPos);
        if (!Block.isFaceFull(blockingState.getShape(level, blockingPos), face.getOpposite()))
            return false;
        if (face.getAxis().choose(from.getX(), from.getY(), from.getZ())
                != face.getAxis().choose(to.getX(), to.getY(), to.getZ()))
            return false;

        BlockState currentState = level.getBlockState(from);
        BlockState blockingAppearance = getCTBlockState(
                level, currentState, face.getOpposite(), from.relative(face), blockingPos);
        return connectsTo(level, from, state, blockingPos, blockingAppearance, face);
    }

    private BlockState getCTBlockState(BlockAndTintGetter level, BlockState reference, Direction face,
                                       BlockPos fromPos, BlockPos toPos) {
        BlockState blockState = level.getBlockState(toPos);
        return blockState.getAppearance(level, toPos, face, reference, fromPos);
    }

    private static final class CTPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final Map<Direction, Integer> ctIndices;
        private final Sprites sp;
        private final int sheetSize;

        CTPart(BlockStateModelPart delegate, Map<Direction, Integer> ctIndices,
               Sprites sp, int sheetSize) {
            this.delegate = delegate;
            this.ctIndices = ctIndices;
            this.sp = sp;
            this.sheetSize = sheetSize;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            List<BakedQuad> quads = delegate.getQuads(direction);
            if (direction == null || ctIndices.isEmpty())
                return quads;

            Integer index = ctIndices.get(direction);
            if (index == null)
                return quads;

            int col = index % sheetSize;
            int row = index / sheetSize;

            List<BakedQuad> result = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                if (matchesBaseSprite(quad.materialInfo().sprite())) {
                    result.add(remapQuad(quad, col, row));
                } else {
                    result.add(quad);
                }
            }
            return result;
        }

        /** Robust comparison: same atlas + same UV span = same sprite. */
        private boolean matchesBaseSprite(TextureAtlasSprite sprite) {
            return sprite.atlasLocation().equals(sp.base.atlasLocation())
                    && sprite.getU0() == sp.base.getU0()
                    && sprite.getU1() == sp.base.getU1()
                    && sprite.getV0() == sp.base.getV0()
                    && sprite.getV1() == sp.base.getV1();
        }

        private BakedQuad remapQuad(BakedQuad quad, int col, int row) {
            long[] newUVs = new long[4];
            for (int v = 0; v < 4; v++) {
                long packed = quad.packedUV(v);
                float atlasU = UVPair.unpackU(packed);
                float atlasV = UVPair.unpackV(packed);

                float targetU = getTargetU(atlasU, col);
                float targetV = getTargetV(atlasV, row);

                newUVs[v] = UVPair.pack(targetU, targetV);
            }

            BakedQuad.MaterialInfo mat = quad.materialInfo();
            return new BakedQuad(
                    quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                    newUVs[0], newUVs[1], newUVs[2], newUVs[3],
                    quad.direction(),
                    new BakedQuad.MaterialInfo(sp.connected, mat.layer(), mat.itemRenderType(),
                            mat.tintIndex(), mat.shade(), mat.lightEmission(), mat.ambientOcclusion()));
        }

        private float getTargetU(float atlasU, int tileX) {
            float localU = getUnInterpolatedU(sp.base, atlasU);
            return sp.connected.getU((tileX + localU) / sheetSize);
        }

        private float getTargetV(float atlasV, int tileY) {
            float localV = getUnInterpolatedV(sp.base, atlasV);
            return sp.connected.getV((tileY + localV) / sheetSize);
        }

        private static float getUnInterpolatedU(TextureAtlasSprite sprite, float atlasU) {
            return clamp01((atlasU - sprite.getU0()) / (sprite.getU1() - sprite.getU0()));
        }

        private static float getUnInterpolatedV(TextureAtlasSprite sprite, float atlasV) {
            return clamp01((atlasV - sprite.getV0()) / (sprite.getV1() - sprite.getV0()));
        }

        private static float clamp01(float value) {
            return Math.max(0.0F, Math.min(1.0F, value));
        }

        @Override public boolean useAmbientOcclusion() { return delegate.useAmbientOcclusion(); }
        @Override public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() { return delegate.particleMaterial(); }
        @Override public int materialFlags() { return delegate.materialFlags(); }
    }

    private static final class Sprites {
        final TextureAtlasSprite base, connected;

        Sprites(TextureAtlasSprite base, TextureAtlasSprite connected) {
            this.base = base;
            this.connected = connected;
        }
    }

    private record GeometryKey(Object delegateKey, int down, int up, int north, int south, int west, int east) {}
}
