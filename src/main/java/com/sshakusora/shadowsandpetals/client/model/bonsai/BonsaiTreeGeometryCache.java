package com.sshakusora.shadowsandpetals.client.model.bonsai;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiModelTransform;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.renderer.BonsaiPartCacheKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared, immutable geometry cache for chunk-rendered bonsai trees.
 *
 * <p>The cache is safe to use from section compilation workers after all
 * inputs have been captured in the immutable {@link BonsaiBlockEntity.RenderData}
 * and {@link BonsaiPartCacheKey} values. Atlas/model references are discarded
 * on resource reload.</p>
 */
@SuppressWarnings({"resource", "deprecation"})
public final class BonsaiTreeGeometryCache {
    public static final int TRUNK_TINT_INDEX = 0;
    public static final int LEAVES_TINT_INDEX = 1;

    private static @Nullable TextureAtlasSprite baseLogSprite;
    private static @Nullable TextureAtlasSprite baseLeavesSprite;
    private static final Map<BonsaiPartCacheKey, CachedParts> PART_CACHE =
            new ConcurrentHashMap<>();
    private static final Map<Identifier, Optional<ResolvedMaterial>> MATERIAL_CACHE =
            new ConcurrentHashMap<>();
    private static @Nullable BlockModelResolver blockModelResolver;

    private BonsaiTreeGeometryCache() {
    }

    public static CachedParts getParts(
            List<BlockStateModel> models,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState blockState,
            BonsaiPartCacheKey key
    ) {
        return PART_CACHE.computeIfAbsent(
                key,
                ignored -> buildParts(models, level, pos, blockState, key)
        );
    }

    private static CachedParts buildParts(
            List<BlockStateModel> models,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState blockState,
            BonsaiPartCacheKey key
    ) {
        List<BlockStateModelPart> rawParts = new ArrayList<>();
        RandomSource partRandom = RandomSource.create(42L);
        boolean hasTranslucency = false;
        for (BlockStateModel model : models) {
            model.collectParts(level, pos, blockState, partRandom, rawParts);
            hasTranslucency |= model.hasMaterialFlag(
                    level, pos, blockState, BakedQuad.FLAG_TRANSLUCENT
            );
        }

        ResolvedMaterial trunkMaterial = resolveMaterial(key.trunkBlockId());
        ResolvedMaterial leavesMaterial = key.dead()
                ? null
                : resolveMaterial(key.leavesBlockId());
        if (trunkMaterial == null) {
            return new CachedParts(List.copyOf(rawParts), hasTranslucency, -1, -1);
        }

        TextureAtlasSprite baseLog = getBaseLogSprite();
        TextureAtlasSprite baseLeaves = getBaseLeavesSprite();
        List<BlockStateModelPart> wrapped = new ArrayList<>(rawParts.size());
        for (BlockStateModelPart part : rawParts) {
            wrapped.add(new BonsaiPart(
                    part,
                    trunkMaterial,
                    leavesMaterial,
                    baseLog,
                    baseLeaves
            ));
        }
        hasTranslucency |= trunkMaterial.hasTranslucency();
        hasTranslucency |= leavesMaterial != null && leavesMaterial.hasTranslucency();
        return new CachedParts(
                List.copyOf(wrapped),
                hasTranslucency,
                tintIndex(trunkMaterial),
                tintIndex(leavesMaterial)
        );
    }

    public record CachedParts(
            List<BlockStateModelPart> parts,
            boolean hasTranslucency,
            int trunkTintIndex,
            int leavesTintIndex
    ) {
    }

    /** Returns immutable parts rotated into the block's 16-step pose. */
    public static List<BlockStateModelPart> rotateParts(
            List<BlockStateModelPart> source,
            int rotation
    ) {
        Matrix4f transform = BonsaiModelTransform.aroundBlockCenter(rotation);
        List<BlockStateModelPart> result = new ArrayList<>(source.size());
        for (BlockStateModelPart part : source) {
            result.add(new RotatedPart(part, transform));
        }
        return List.copyOf(result);
    }

    /**
     * Creates a model backed by an already extracted part list. This is used by
     * the optional, one-off breaking overlay because Minecraft's normal crack
     * path passes an empty level and cannot read block-entity ModelData.
     */
    public static BlockStateModel fixedPartsModel(List<BlockStateModelPart> parts) {
        return new FixedPartsModel(parts);
    }

    private static final class FixedPartsModel implements BlockStateModel {
        private final List<BlockStateModelPart> parts;

        private FixedPartsModel(List<BlockStateModelPart> parts) {
            this.parts = List.copyOf(parts);
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            output.addAll(parts);
        }

        @Override
        public void collectParts(
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                RandomSource random,
                List<BlockStateModelPart> output
        ) {
            output.addAll(parts);
        }

        @Override
        public Material.Baked particleMaterial() {
            if (parts.isEmpty()) {
                throw new IllegalStateException(
                        "A breaking bonsai model must contain at least one part"
                );
            }
            return parts.getFirst().particleMaterial();
        }

        @Override
        public int materialFlags() {
            int flags = 0;
            for (BlockStateModelPart part : parts) {
                flags |= part.materialFlags();
            }
            return flags;
        }
    }

    private static final class RotatedPart implements BlockStateModelPart {
        private final List<BakedQuad>[] quadsByDirection;
        private final List<BakedQuad> generalQuads;
        private final TriState ambientOcclusion;
        private final Material.Baked particleMaterial;
        private final int materialFlags;

        @SuppressWarnings("unchecked")
        private RotatedPart(BlockStateModelPart delegate, Matrix4f transform) {
            this.quadsByDirection = new List[Direction.values().length];
            int flags = delegate.materialFlags();
            for (Direction direction : Direction.values()) {
                // A non-quadrant rotation has no meaningful face bucket.
                this.quadsByDirection[direction.get3DDataValue()] = List.of();
            }
            this.generalQuads = rotateAllQuads(delegate, transform);
            flags |= flags(this.generalQuads);
            this.ambientOcclusion = delegate.ambientOcclusion();
            this.particleMaterial = delegate.particleMaterial();
            this.materialFlags = flags;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null
                    ? generalQuads
                    : quadsByDirection[direction.get3DDataValue()];
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ambientOcclusion != TriState.FALSE;
        }

        @Override
        public TriState ambientOcclusion() {
            return ambientOcclusion;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particleMaterial;
        }

        @Override
        public int materialFlags() {
            return materialFlags;
        }

        private static List<BakedQuad> rotateQuads(
                List<BakedQuad> source,
                Matrix4f transform
        ) {
            if (source.isEmpty()) {
                return source;
            }
            MutableQuad mutable = new MutableQuad();
            List<BakedQuad> result = new ArrayList<>(source.size());
            for (BakedQuad quad : source) {
                result.add(mutable.setFrom(quad)
                        .transform(transform)
                        .recomputeNormals(true)
                        .toBakedQuad());
            }
            return List.copyOf(result);
        }

        private static List<BakedQuad> rotateAllQuads(
                BlockStateModelPart source,
                Matrix4f transform
        ) {
            int size = source.getQuads(null).size();
            for (Direction direction : Direction.values()) {
                size += source.getQuads(direction).size();
            }
            if (size == 0) {
                return List.of();
            }
            List<BakedQuad> all = new ArrayList<>(size);
            for (Direction direction : Direction.values()) {
                all.addAll(rotateQuads(source.getQuads(direction), transform));
            }
            all.addAll(rotateQuads(source.getQuads(null), transform));
            return List.copyOf(all);
        }

        private static int flags(List<BakedQuad> quads) {
            int flags = 0;
            for (BakedQuad quad : quads) {
                flags |= quad.materialInfo().flags();
            }
            return flags;
        }
    }

    private static final class BonsaiPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final List<BakedQuad>[] quadsByDirection;
        private final List<BakedQuad> generalQuads;
        private final int materialFlags;

        @SuppressWarnings("unchecked")
        private BonsaiPart(
                BlockStateModelPart delegate,
                ResolvedMaterial trunkMaterial,
                @Nullable ResolvedMaterial leavesMaterial,
                TextureAtlasSprite baseLogSprite,
                TextureAtlasSprite baseLeavesSprite
        ) {
            this.delegate = delegate;
            this.quadsByDirection = new List[Direction.values().length];
            int flags = delegate.materialFlags();
            for (Direction direction : Direction.values()) {
                this.quadsByDirection[direction.get3DDataValue()] = remapQuads(
                        delegate.getQuads(direction),
                        trunkMaterial,
                        leavesMaterial,
                        baseLogSprite,
                        baseLeavesSprite
                );
                flags |= flags(this.quadsByDirection[direction.get3DDataValue()]);
            }
            this.generalQuads = remapQuads(
                    delegate.getQuads(null),
                    trunkMaterial,
                    leavesMaterial,
                    baseLogSprite,
                    baseLeavesSprite
            );
            flags |= flags(this.generalQuads);
            this.materialFlags = flags;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null
                    ? generalQuads
                    : quadsByDirection[direction.get3DDataValue()];
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.ambientOcclusion() != TriState.FALSE;
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
            return materialFlags;
        }

        private static int flags(List<BakedQuad> quads) {
            int flags = 0;
            for (BakedQuad quad : quads) {
                flags |= quad.materialInfo().flags();
            }
            return flags;
        }

        private static List<BakedQuad> remapQuads(
                List<BakedQuad> quads,
                ResolvedMaterial trunkMaterial,
                @Nullable ResolvedMaterial leavesMaterial,
                TextureAtlasSprite baseLogSprite,
                TextureAtlasSprite baseLeavesSprite
        ) {
            if (quads.isEmpty()) {
                return quads;
            }
            List<BakedQuad> result = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                result.add(remapQuad(
                        quad,
                        trunkMaterial,
                        leavesMaterial,
                        baseLogSprite,
                        baseLeavesSprite
                ));
            }
            return List.copyOf(result);
        }

        private static BakedQuad remapQuad(
                BakedQuad quad,
                ResolvedMaterial trunkMaterial,
                @Nullable ResolvedMaterial leavesMaterial,
                TextureAtlasSprite baseLogSprite,
                TextureAtlasSprite baseLeavesSprite
        ) {
            TextureAtlasSprite quadSprite = quad.materialInfo().sprite();
            TextureAtlasSprite sourceSprite;
            ResolvedMaterial targetMaterial;
            if (spritesMatch(quadSprite, baseLogSprite)) {
                targetMaterial = trunkMaterial;
                sourceSprite = baseLogSprite;
            } else if (spritesMatch(quadSprite, baseLeavesSprite)) {
                if (leavesMaterial == null) {
                    return quad;
                }
                targetMaterial = leavesMaterial;
                sourceSprite = baseLeavesSprite;
            } else {
                return quad;
            }
            int tintIndex = targetMaterial.materialInfo() != null
                    && targetMaterial.materialInfo().tintIndex() >= 0
                    ? (sourceSprite == baseLogSprite
                    ? TRUNK_TINT_INDEX
                    : LEAVES_TINT_INDEX)
                    : -1;
            return remapQuadSprite(quad, sourceSprite, targetMaterial, tintIndex);
        }

        private static BakedQuad remapQuadSprite(
                BakedQuad quad,
                TextureAtlasSprite source,
                ResolvedMaterial target,
                int tintIndex
        ) {
            long[] newUVs = new long[4];
            for (int vertex = 0; vertex < 4; vertex++) {
                long packed = quad.packedUV(vertex);
                float atlasU = UVPair.unpackU(packed);
                float atlasV = UVPair.unpackV(packed);
                float localU = getUnInterpolatedU(source, atlasU);
                float localV = getUnInterpolatedV(source, atlasV);
                newUVs[vertex] = UVPair.pack(
                        target.sprite().getU(localU),
                        target.sprite().getV(localV)
                );
            }

            BakedQuad.MaterialInfo originalInfo = quad.materialInfo();
            BakedQuad.MaterialInfo targetInfo = target.materialInfo();
            if (targetInfo == null) {
                targetInfo = originalInfo;
            }
            return new BakedQuad(
                    quad.position0(),
                    quad.position1(),
                    quad.position2(),
                    quad.position3(),
                    newUVs[0],
                    newUVs[1],
                    newUVs[2],
                    newUVs[3],
                    quad.direction(),
                    new BakedQuad.MaterialInfo(
                            target.sprite(),
                            targetInfo.layer(),
                            targetInfo.itemRenderType(),
                            tintIndex,
                            targetInfo.shade(),
                            targetInfo.lightEmission(),
                            targetInfo.ambientOcclusion()
                    ),
                    quad.bakedNormals(),
                    quad.bakedColors()
            );
        }

        private static boolean spritesMatch(
                TextureAtlasSprite first,
                TextureAtlasSprite second
        ) {
            return first.atlasLocation().equals(second.atlasLocation())
                    && first.getU0() == second.getU0()
                    && first.getU1() == second.getU1()
                    && first.getV0() == second.getV0()
                    && first.getV1() == second.getV1();
        }

        private static float getUnInterpolatedU(
                TextureAtlasSprite sprite,
                float atlasU
        ) {
            return Math.clamp(
                    (atlasU - sprite.getU0()) / (sprite.getU1() - sprite.getU0()),
                    0.0F,
                    1.0F
            );
        }

        private static float getUnInterpolatedV(
                TextureAtlasSprite sprite,
                float atlasV
        ) {
            return Math.clamp(
                    (atlasV - sprite.getV0()) / (sprite.getV1() - sprite.getV0()),
                    0.0F,
                    1.0F
            );
        }
    }

    private record ResolvedMaterial(
            TextureAtlasSprite sprite,
            BakedQuad.@Nullable MaterialInfo materialInfo,
            boolean hasTranslucency
    ) {
    }

    private static @Nullable ResolvedMaterial resolveMaterial(
            @Nullable Identifier blockId
    ) {
        if (blockId == null) {
            return null;
        }
        return MATERIAL_CACHE.computeIfAbsent(
                blockId,
                BonsaiTreeGeometryCache::resolveMaterialUncached
        ).orElse(null);
    }

    private static Optional<ResolvedMaterial> resolveMaterialUncached(
            Identifier blockId
    ) {
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        if (block == Blocks.AIR) {
            return Optional.empty();
        }
        BlockState blockState = block.defaultBlockState();
        BlockModelRenderState renderState = new BlockModelRenderState();
        getBlockModelResolver().update(
                renderState,
                blockState,
                DisplayRenderer.BLOCK_DISPLAY_CONTEXT
        );
        List<BlockStateModelPart> parts = renderState.modelParts;
        if (parts == null || parts.isEmpty()) {
            return Optional.empty();
        }
        Material.Baked particleMaterial = parts.getFirst().particleMaterial();
        TextureAtlasSprite particleSprite = particleMaterial.sprite();
        BakedQuad.MaterialInfo materialInfo = findMaterialInfo(parts, particleSprite);
        boolean hasTranslucency = parts.stream()
                .anyMatch(part -> (part.materialFlags() & BakedQuad.FLAG_TRANSLUCENT) != 0);
        return Optional.of(new ResolvedMaterial(
                particleSprite,
                materialInfo,
                hasTranslucency
        ));
    }

    private static synchronized BlockModelResolver getBlockModelResolver() {
        if (blockModelResolver == null) {
            blockModelResolver = new BlockModelResolver(
                    Minecraft.getInstance().getModelManager()
            );
        }
        return blockModelResolver;
    }

    private static BakedQuad.@Nullable MaterialInfo findMaterialInfo(
            List<BlockStateModelPart> parts,
            TextureAtlasSprite sprite
    ) {
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                for (BakedQuad quad : part.getQuads(direction)) {
                    if (BonsaiPart.spritesMatch(
                            quad.materialInfo().sprite(),
                            sprite
                    )) {
                        return quad.materialInfo();
                    }
                }
            }
            for (BakedQuad quad : part.getQuads(null)) {
                if (BonsaiPart.spritesMatch(
                        quad.materialInfo().sprite(),
                        sprite
                )) {
                    return quad.materialInfo();
                }
            }
        }
        return null;
    }

    private static synchronized TextureAtlasSprite getBaseLogSprite() {
        if (baseLogSprite == null) {
            baseLogSprite = getAtlasSprite(
                    ShadowsAndPetals.asResource("block/maple_log")
            );
        }
        return baseLogSprite;
    }

    private static synchronized TextureAtlasSprite getBaseLeavesSprite() {
        if (baseLeavesSprite == null) {
            baseLeavesSprite = getAtlasSprite(
                    ShadowsAndPetals.asResource("block/maple_leaves_0")
            );
        }
        return baseLeavesSprite;
    }

    private static TextureAtlasSprite getAtlasSprite(Identifier textureId) {
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);
        return atlas.getSprite(textureId);
    }

    private static int tintIndex(@Nullable ResolvedMaterial material) {
        return material == null || material.materialInfo() == null
                ? -1
                : material.materialInfo().tintIndex();
    }

    /** Returns the tint index used by the resolved target block model. */
    public static int getTargetTintIndex(@Nullable Identifier blockId) {
        return tintIndex(resolveMaterial(blockId));
    }

    /** Drops all references to models and atlas sprites after a resource reload. */
    public static void invalidate() {
        PART_CACHE.clear();
        MATERIAL_CACHE.clear();
        baseLogSprite = null;
        baseLeavesSprite = null;
        blockModelResolver = null;
    }
}
