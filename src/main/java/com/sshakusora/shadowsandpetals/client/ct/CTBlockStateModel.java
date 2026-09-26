package com.sshakusora.shadowsandpetals.client.ct;

import com.sshakusora.shadowsandpetals.client.ct.CTRegistry.CTEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Runtime connected-texture wrapper for baked block-state models. */
public final class CTBlockStateModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel {
    private static final ModelProperty<CTData> CT_DATA = new ModelProperty<>();
    private static final int BLOCK_VERTEX_STRIDE = 8;

    private final Block expectedBlock;
    private final CTEntry entry;
    private volatile @Nullable Sprites sprites;

    public CTBlockStateModel(Block expectedBlock, BakedModel delegate, CTEntry entry) {
        super(delegate);
        this.expectedBlock = expectedBlock;
        this.entry = entry;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData originalData) {
        ModelData base = super.getModelData(level, pos, state, originalData);
        if (state == null || state.getBlock() != expectedBlock) {
            return base;
        }

        CTContextBridge bridge = CTContextBridges.find(level, pos, state);
        boolean copycat = bridge != null;
        EnumMap<Direction, Integer> tileIndices = new EnumMap<>(Direction.class);
        EnumMap<Direction, Integer> textureIndices = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            BlockPos neighborPos = pos.relative(face);
            if (!shouldBuildTextureData(level, pos, state, face, neighborPos, bridge)) {
                continue;
            }
            CTContext context = bridge == null
                    ? buildContext(level, pos, state, face)
                    : bridge.buildContext(level, pos, state, face);
            tileIndices.put(face, entry.type().getTextureIndex(context));
            textureIndices.put(face, entry.selectTextureIndex(state, pos, face));
        }
        return base.derive().with(CT_DATA, new CTData(tileIndices, textureIndices, copycat)).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, @Nullable Direction face, RandomSource random) {
        return originalModel.getQuads(state, face, random);
    }

    @Override
    public List<BakedQuad> getQuads(
            BlockState state,
            @Nullable Direction face,
            RandomSource random,
            ModelData data,
            @Nullable RenderType renderType
    ) {
        List<BakedQuad> quads = originalModel instanceof IDynamicBakedModel dynamic
                ? dynamic.getQuads(state, face, random, data, renderType)
                : originalModel.getQuads(state, face, random);
        if (face == null || state == null || state.getBlock() != expectedBlock) {
            return quads;
        }

        CTData ctData = data.get(CT_DATA);
        if (ctData == null) {
            return quads;
        }
        Integer tileIndex = ctData.tileIndices.get(face);
        Integer textureIndex = ctData.textureIndices.get(face);
        if (tileIndex == null || textureIndex == null || textureIndex < 0
                || textureIndex >= entry.connectedTextures().size()) {
            return quads;
        }

        Sprites resolvedSprites = ensureSprites();
        if (ctData.copycat) {
            if (tileIndex < 0 || tileIndex >= resolvedSprites.copycat.get(textureIndex).size()) {
                return quads;
            }
            TextureAtlasSprite tileSprite = resolvedSprites.copycat.get(textureIndex).get(tileIndex);
            List<BakedQuad> result = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                result.add(quad.getSprite() == resolvedSprites.base
                        ? remapQuadToSprite(quad, tileSprite, resolvedSprites.base)
                        : quad);
            }
            return result;
        }

        ConnectedSprite connected = resolvedSprites.connected.get(textureIndex);
        int column = Math.floorMod(tileIndex, entry.type().getSheetSize());
        int row = Math.floorDiv(tileIndex, entry.type().getSheetSize());

        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            result.add(quad.getSprite() == resolvedSprites.base
                    ? remapQuad(quad, column, row, connected, resolvedSprites.base)
                    : quad);
        }
        return result;
    }

    private Sprites ensureSprites() {
        Sprites resolved = sprites;
        if (resolved != null) {
            return resolved;
        }
        synchronized (this) {
            resolved = sprites;
            if (resolved == null) {
                TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
                TextureAtlasSprite base = atlas.getSprite(entry.baseTexture());
                List<ConnectedSprite> connected = entry.connectedTextures().stream()
                        .map(atlas::getSprite)
                        .map(sprite -> new ConnectedSprite(sprite, entry.type().getSheetSize(), entry.padding()))
                        .toList();
                List<List<TextureAtlasSprite>> copycat = new ArrayList<>(entry.connectedTextures().size());
                int tileCount = entry.type().getSheetSize() * entry.type().getSheetSize();
                for (int connectedTextureIndex = 0;
                     connectedTextureIndex < entry.connectedTextures().size();
                     connectedTextureIndex++) {
                    List<TextureAtlasSprite> tiles = new ArrayList<>(tileCount);
                    for (int tileIndex = 0; tileIndex < tileCount; tileIndex++) {
                        tiles.add(atlas.getSprite(entry.copycatTexture(connectedTextureIndex, tileIndex)));
                    }
                    copycat.add(List.copyOf(tiles));
                }
                resolved = new Sprites(base, connected, List.copyOf(copycat));
                sprites = resolved;
            }
            return resolved;
        }
    }

    private CTContext buildContext(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        Direction.Axis axis = face.getAxis();
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        Direction up = axis.isHorizontal() ? Direction.UP : Direction.NORTH;
        Direction right = axis == Direction.Axis.X ? Direction.SOUTH : Direction.WEST;
        right = positive ? right.getOpposite() : right;
        if (face == Direction.DOWN) {
            up = up.getOpposite();
            right = right.getOpposite();
        }

        CTContext context = new CTContext();
        context.up = connectsTo(level, pos, state, face, up);
        context.down = connectsTo(level, pos, state, face, up.getOpposite());
        context.left = connectsTo(level, pos, state, face, right.getOpposite());
        context.right = connectsTo(level, pos, state, face, right);
        context.topLeft = context.up && context.left
                && connectsToDiagonal(level, pos, state, face, up, right.getOpposite());
        context.topRight = context.up && context.right
                && connectsToDiagonal(level, pos, state, face, up, right);
        context.bottomLeft = context.down && context.left
                && connectsToDiagonal(level, pos, state, face, up.getOpposite(), right.getOpposite());
        context.bottomRight = context.down && context.right
                && connectsToDiagonal(level, pos, state, face, up.getOpposite(), right);
        return context;
    }

    private boolean connectsTo(BlockAndTintGetter level, BlockPos pos, BlockState state,
                               Direction face, Direction offset) {
        return connectsTo(level, pos, state, pos.relative(offset), face);
    }

    private boolean connectsToDiagonal(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                       Direction face, Direction first, Direction second) {
        return connectsTo(level, pos, state, pos.relative(first).relative(second), face);
    }

    private boolean connectsTo(BlockAndTintGetter level, BlockPos pos, BlockState state,
                               BlockPos otherPos, Direction face) {
        BlockState reference = level.getBlockState(pos);
        BlockState other = level.getBlockState(otherPos);
        BlockState appearance = other.getAppearance(level, otherPos, face, reference, pos);
        if (appearance.getBlock() != state.getBlock()) {
            return false;
        }
        BlockPos blockingPos = otherPos.relative(face);
        BlockState blockingState = level.getBlockState(blockingPos);
        if (!Block.isFaceFull(blockingState.getShape(level, blockingPos), face.getOpposite())) {
            return true;
        }
        if (face.getAxis().choose(pos.getX(), pos.getY(), pos.getZ())
                != face.getAxis().choose(otherPos.getX(), otherPos.getY(), otherPos.getZ())) {
            return true;
        }
        BlockState blockingAppearance = blockingState.getAppearance(level, blockingPos, face, reference, otherPos);
        return blockingAppearance.getBlock() != state.getBlock();
    }

    private static boolean shouldBuildTextureData(BlockAndTintGetter level, BlockPos pos,
                                                   BlockState state, Direction face, BlockPos neighborPos,
                                                   @Nullable CTContextBridge bridge) {
        if (Block.shouldRenderFace(state, level, pos, face, neighborPos)) {
            return true;
        }
        if (bridge != null) {
            return true;
        }
        return false;
    }

    private static BakedQuad remapQuad(BakedQuad quad, int column, int row,
                                       ConnectedSprite connected, TextureAtlasSprite base) {
        int[] vertices = quad.getVertices().clone();
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * BLOCK_VERTEX_STRIDE;
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);
            vertices[offset + 4] = Float.floatToRawIntBits(connected.u(column, localU(base, u)));
            vertices[offset + 5] = Float.floatToRawIntBits(connected.v(row, localV(base, v)));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(),
                connected.sprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    private static BakedQuad remapQuadToSprite(BakedQuad quad, TextureAtlasSprite target,
                                               TextureAtlasSprite base) {
        int[] vertices = quad.getVertices().clone();
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * BLOCK_VERTEX_STRIDE;
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);
            vertices[offset + 4] = Float.floatToRawIntBits(target.getU(localU(base, u)));
            vertices[offset + 5] = Float.floatToRawIntBits(target.getV(localV(base, v)));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(),
                target, quad.isShade(), quad.hasAmbientOcclusion());
    }

    private static float localU(TextureAtlasSprite sprite, float atlasU) {
        return clamp01((atlasU - sprite.getU0()) / (sprite.getU1() - sprite.getU0()));
    }

    private static float localV(TextureAtlasSprite sprite, float atlasV) {
        return clamp01((atlasV - sprite.getV0()) / (sprite.getV1() - sprite.getV0()));
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private record CTData(Map<Direction, Integer> tileIndices, Map<Direction, Integer> textureIndices,
                          boolean copycat) {
    }

    private record Sprites(TextureAtlasSprite base, List<ConnectedSprite> connected,
                           List<List<TextureAtlasSprite>> copycat) {
    }

    private static final class ConnectedSprite {
        private final TextureAtlasSprite sprite;
        private final float tileWidth;
        private final float tileHeight;
        private final float paddingX;
        private final float paddingY;
        private final float contentWidth;
        private final float contentHeight;

        private ConnectedSprite(TextureAtlasSprite sprite, int sheetSize, int padding) {
            this.sprite = sprite;
            this.tileWidth = sprite.contents().width() / (float) sheetSize;
            this.tileHeight = sprite.contents().height() / (float) sheetSize;
            this.paddingX = Math.min(padding, tileWidth / 2.0F);
            this.paddingY = Math.min(padding, tileHeight / 2.0F);
            this.contentWidth = tileWidth - 2.0F * paddingX;
            this.contentHeight = tileHeight - 2.0F * paddingY;
        }

        private float u(int column, float local) {
            float pixel = column * tileWidth + paddingX + local * contentWidth;
            return sprite.getU(pixel / sprite.contents().width());
        }

        private float v(int row, float local) {
            float pixel = row * tileHeight + paddingY + local * contentHeight;
            return sprite.getV(pixel / sprite.contents().height());
        }
    }
}
