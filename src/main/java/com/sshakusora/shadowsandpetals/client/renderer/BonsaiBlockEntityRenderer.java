package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiBlock;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiModelTransform;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders planted bonsai trees with dynamic trunk/leaf textures.
 *
 * <p>The tree models are baked with placeholder tree textures
 * ({@code maple_log}, {@code maple_leaves_0}). At render time, their quads are
 * categorized by comparing their sprites against the known base sprites, and
 * the UVs/materials are remapped to sample the resolved tree textures. The pot
 * itself is supplied by the normal chunk model renderer. The same tree quads
 * are also consumed by the chunk model when the complete tree envelope fits
 * inside one section; this BER remains the cross-section fallback.</p>
 */
@SuppressWarnings({"resource", "deprecation"}) // Atlas sprites are owned and closed by Minecraft's atlas manager.
public class BonsaiBlockEntityRenderer implements
        BlockEntityRenderer<BonsaiBlockEntity, BonsaiBlockEntityRenderer.State> {

    /**
     * The bonsai models were authored in Blockbench with a 90-degree yaw
     * error: the pot's long axis runs along model Z instead of X. The shared
     * model transform keeps the chunk-rendered pot and BER tree aligned.
     */
    public static final int TRUNK_TINT_INDEX = 0;
    public static final int LEAVES_TINT_INDEX = 1;

    // Base sprites from the bonsai model — used to identify which texture slot a quad uses
    private static @Nullable TextureAtlasSprite baseLogSprite;
    private static @Nullable TextureAtlasSprite baseLeavesSprite;

    /** Wrapped parts per render configuration; invalidated on resource reload. */
    private static final Map<BonsaiPartCacheKey, CachedParts> PART_CACHE = new ConcurrentHashMap<>();
    /** Resolved render materials per block id; invalidated on resource reload. */
    private static final Map<Identifier, Optional<ResolvedMaterial>> MATERIAL_CACHE = new ConcurrentHashMap<>();

    public BonsaiBlockEntityRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public boolean shouldRender(BonsaiBlockEntity blockEntity, Vec3 cameraPosition) {
        return blockEntity.isPlanted()
                && BonsaiRenderRouting.usesBer(blockEntity.getBlockPos())
                && BlockEntityRenderer.super.shouldRender(blockEntity, cameraPosition);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BonsaiBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTicks, cameraPosition, breakProgress);

        BlockAndTintGetter level = (BlockAndTintGetter) blockEntity.getLevel();
        if (level == null) {
            state.modelParts = List.of();
            state.hasTranslucency = false;
            state.tintLayers = BlockModelRenderState.EMPTY_TINTS;
            return;
        }

        BlockState blockState = blockEntity.getBlockState();
        state.rotation = blockState.getValue(BonsaiBlock.ROTATION);
        BlockPos pos = blockEntity.getBlockPos();

        // Safe positions are fully emitted by BonsaiPotBlockStateModel. Keep
        // the renderer registered for the fallback path, but avoid extracting
        // any tree state or submitting duplicate geometry here.
        if (!BonsaiRenderRouting.usesBer(pos)) {
            state.modelParts = List.of();
            state.hasTranslucency = false;
            state.tintLayers = BlockModelRenderState.EMPTY_TINTS;
            return;
        }

        if (!blockEntity.isPlanted()) {
            state.modelParts = List.of();
            state.hasTranslucency = false;
            state.tintLayers = BlockModelRenderState.EMPTY_TINTS;
            return;
        }

        BlockStateModel treeModel = blockEntity.isDead()
                ? BlockModelRegistry.BONSAI_DEAD_SHAPES.get(blockEntity.getShape())
                : BlockModelRegistry.BONSAI_SHAPES.get(blockEntity.getShape());
        if (treeModel == null) {
            state.modelParts = List.of();
            state.hasTranslucency = false;
            state.tintLayers = BlockModelRenderState.EMPTY_TINTS;
            return;
        }

        List<BlockStateModel> models = List.of(treeModel);

        BonsaiPartCacheKey key = BonsaiPartCacheKey.forState(
                blockEntity.getShape(),
                blockEntity.isPlanted(),
                blockEntity.isDead(),
                blockEntity.getTrunkBlockId(),
                blockEntity.getLeavesBlockId());
        CachedParts cached = getCachedParts(models, level, pos, blockState, key);
        state.modelParts = cached.parts();
        state.hasTranslucency = cached.hasTranslucency();
        state.tintLayers = resolveTintLayers(
                cached,
                blockEntity.getTrunkBlockId(),
                blockEntity.getLeavesBlockId(),
                level,
                pos
        );
    }

    public static CachedParts getCachedParts(
            List<BlockStateModel> models,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState blockState,
            BonsaiPartCacheKey key
    ) {
        return PART_CACHE.computeIfAbsent(key, k -> buildParts(models, level, pos, blockState, k));
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
        ResolvedMaterial leavesMaterial = key.dead() ? null : resolveMaterial(key.leavesBlockId());
        if (trunkMaterial == null) {
            // Nothing to remap onto (unresolved sprite) —
            // keep the raw parts, exactly like the pre-cache behaviour.
            return new CachedParts(List.copyOf(rawParts), hasTranslucency, -1, -1);
        }
        TextureAtlasSprite baseLog = getBaseLogSprite();
        TextureAtlasSprite baseLeaves = getBaseLeavesSprite();
        List<BlockStateModelPart> wrapped = new ArrayList<>(rawParts.size());
        for (BlockStateModelPart part : rawParts) {
            wrapped.add(new BonsaiPart(part, trunkMaterial, leavesMaterial, baseLog, baseLeaves));
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

    /** Returns immutable tree parts rotated into the block's 16-step pose. */
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
                // A non-quadrant transform no longer has a meaningful face
                // bucket. Keep directional lists empty so the chunk renderer
                // cannot cull a rotated tree against an adjacent solid block.
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
        @Deprecated
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

        private static List<BakedQuad> rotateQuads(List<BakedQuad> source, Matrix4f transform) {
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

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (state.modelParts.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                BonsaiModelTransform.rotationDegrees(state.rotation)));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        submitNodeCollector.submitMultiLayerBlockModel(
                poseStack,
                state.modelParts,
                state.hasTranslucency,
                state.tintLayers,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BonsaiBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        if (!BonsaiRenderRouting.usesBer(pos) || !blockEntity.isPlanted()) {
            // Empty pots are rendered by the chunk model; keep block bounds for
            // renderer state even though this renderer submits no geometry.
            return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
        }
        AABB bounds = BonsaiRenderRouting.treeBounds(
                blockEntity.getShape(),
                blockEntity.isDead(),
                blockEntity.getBlockState().getValue(BonsaiBlock.ROTATION)
        );
        // The cached bounds describe the tree mesh. Include the pot's block
        // volume as well, since some dead/twin meshes are narrower than the
        // pot and must not be culled at the sides.
        return new AABB(
                pos.getX() + Math.min(bounds.minX, 0.0D),
                pos.getY() + Math.min(bounds.minY, 0.0D),
                pos.getZ() + Math.min(bounds.minZ, 0.0D),
                pos.getX() + Math.max(bounds.maxX, 1.0D),
                pos.getY() + Math.max(bounds.maxY, 1.0D),
                pos.getZ() + Math.max(bounds.maxZ, 1.0D)
        );
    }

    /**
     * Wraps a {@link BlockStateModelPart} with quads remapped from the base
     * log/leaves sprites to the resolved tree's sprites. Remapping happens
     * once in the constructor; {@link #getQuads} returns the baked lists.
     */
    private static final class BonsaiPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final List<BakedQuad>[] quadsByDirection;
        private final List<BakedQuad> generalQuads;
        private final int materialFlags;

        @SuppressWarnings("unchecked")
        BonsaiPart(
                BlockStateModelPart delegate,
                ResolvedMaterial trunkMaterial,
                @Nullable ResolvedMaterial leavesMaterial,
                TextureAtlasSprite baseLogSprite,
                TextureAtlasSprite baseLeavesSprite
        ) {
            this.delegate = delegate;
            this.quadsByDirection = new List[6];
            int materialFlags = delegate.materialFlags();
            for (Direction direction : Direction.values()) {
                this.quadsByDirection[direction.get3DDataValue()] =
                        remapQuads(delegate.getQuads(direction), trunkMaterial, leavesMaterial,
                                baseLogSprite, baseLeavesSprite);
                materialFlags |= flags(this.quadsByDirection[direction.get3DDataValue()]);
            }
            this.generalQuads = remapQuads(delegate.getQuads(null), trunkMaterial, leavesMaterial,
                    baseLogSprite, baseLeavesSprite);
            materialFlags |= flags(this.generalQuads);
            this.materialFlags = materialFlags;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? generalQuads : quadsByDirection[direction.get3DDataValue()];
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

        private List<BakedQuad> remapQuads(
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
                result.add(remapQuad(quad, trunkMaterial, leavesMaterial, baseLogSprite, baseLeavesSprite));
            }
            return result;
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
                    // Dead tree — leaves quads keep the base sprite (model has none)
                    return quad;
                }
                targetMaterial = leavesMaterial;
                sourceSprite = baseLeavesSprite;
            } else {
                // Pot or other texture — leave as-is
                return quad;
            }
            int tintIndex = targetMaterial.materialInfo() != null
                    && targetMaterial.materialInfo().tintIndex() >= 0
                    ? (sourceSprite == baseLogSprite ? TRUNK_TINT_INDEX : LEAVES_TINT_INDEX)
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
            for (int v = 0; v < 4; v++) {
                long packed = quad.packedUV(v);
                float atlasU = UVPair.unpackU(packed);
                float atlasV = UVPair.unpackV(packed);

                float localU = getUnInterpolatedU(source, atlasU);
                float localV = getUnInterpolatedV(source, atlasV);

                float targetU = target.sprite().getU(localU);
                float targetV = target.sprite().getV(localV);

                newUVs[v] = UVPair.pack(targetU, targetV);
            }

            BakedQuad.MaterialInfo mat = quad.materialInfo();
            BakedQuad.MaterialInfo targetInfo = target.materialInfo();
            if (targetInfo == null) {
                targetInfo = mat;
            }
            return new BakedQuad(
                    quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                    newUVs[0], newUVs[1], newUVs[2], newUVs[3],
                    quad.direction(),
                    new BakedQuad.MaterialInfo(
                            target.sprite(), targetInfo.layer(), targetInfo.itemRenderType(),
                            tintIndex, targetInfo.shade(), targetInfo.lightEmission(),
                            targetInfo.ambientOcclusion()
                    ),
                    quad.bakedNormals(),
                    quad.bakedColors()
            );
        }

        private static boolean spritesMatch(TextureAtlasSprite a, TextureAtlasSprite b) {
            return a.atlasLocation().equals(b.atlasLocation())
                    && a.getU0() == b.getU0()
                    && a.getU1() == b.getU1()
                    && a.getV0() == b.getV0()
                    && a.getV1() == b.getV1();
        }

        private static float getUnInterpolatedU(TextureAtlasSprite sprite, float atlasU) {
            return Math.clamp((atlasU - sprite.getU0()) / (sprite.getU1() - sprite.getU0()), 0.0F, 1.0F);
        }

        private static float getUnInterpolatedV(TextureAtlasSprite sprite, float atlasV) {
            return Math.clamp((atlasV - sprite.getV0()) / (sprite.getV1() - sprite.getV0()), 0.0F, 1.0F);
        }
    }

    private record ResolvedMaterial(
            TextureAtlasSprite sprite,
            BakedQuad.@Nullable MaterialInfo materialInfo,
            boolean hasTranslucency
    ) {
    }

    private static @Nullable BlockModelResolver blockModelResolver;

    private static @Nullable ResolvedMaterial resolveMaterial(@Nullable Identifier blockId) {
        if (blockId == null) {
            return null;
        }
        return MATERIAL_CACHE.computeIfAbsent(blockId, BonsaiBlockEntityRenderer::resolveMaterialUncached).orElse(null);
    }

    private static Optional<ResolvedMaterial> resolveMaterialUncached(Identifier blockId) {
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        if (block == Blocks.AIR) {
            return Optional.empty();
        }
        BlockState blockState = block.defaultBlockState();
        BlockModelRenderState renderState = new BlockModelRenderState();
        getBlockModelResolver().update(renderState, blockState, DisplayRenderer.BLOCK_DISPLAY_CONTEXT);
        List<BlockStateModelPart> parts = renderState.modelParts;
        if (parts == null || parts.isEmpty()) {
            return Optional.empty();
        }
        Material.Baked particleMaterial = parts.getFirst().particleMaterial();
        TextureAtlasSprite particleSprite = particleMaterial.sprite();
        BakedQuad.MaterialInfo materialInfo = findMaterialInfo(parts, particleSprite);
        boolean hasTranslucency = parts.stream()
                .anyMatch(part -> (part.materialFlags() & BakedQuad.FLAG_TRANSLUCENT) != 0);
        return Optional.of(new ResolvedMaterial(particleSprite, materialInfo, hasTranslucency));
    }

    private static synchronized BlockModelResolver getBlockModelResolver() {
        if (blockModelResolver == null) {
            blockModelResolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
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
                    if (BonsaiPart.spritesMatch(quad.materialInfo().sprite(), sprite)) {
                        return quad.materialInfo();
                    }
                }
            }
            for (BakedQuad quad : part.getQuads(null)) {
                if (BonsaiPart.spritesMatch(quad.materialInfo().sprite(), sprite)) {
                    return quad.materialInfo();
                }
            }
        }
        return null;
    }

    private static synchronized TextureAtlasSprite getBaseLogSprite() {
        if (baseLogSprite == null) {
            baseLogSprite = getAtlasSprite(ShadowsAndPetals.asResource("block/maple_log"));
        }
        return baseLogSprite;
    }

    private static synchronized TextureAtlasSprite getBaseLeavesSprite() {
        if (baseLeavesSprite == null) {
            baseLeavesSprite = getAtlasSprite(ShadowsAndPetals.asResource("block/maple_leaves_0"));
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

    /** Returns the original tint index of a resolved trunk/leaf block model. */
    public static int getTargetTintIndex(@Nullable Identifier blockId) {
        return tintIndex(resolveMaterial(blockId));
    }

    private static int[] resolveTintLayers(
            CachedParts cached,
            @Nullable Identifier trunkBlockId,
            @Nullable Identifier leavesBlockId,
            BlockAndTintGetter level,
            BlockPos pos
    ) {
        boolean trunkTinted = cached.trunkTintIndex() >= 0;
        boolean leavesTinted = cached.leavesTintIndex() >= 0;
        if (!trunkTinted && !leavesTinted) {
            return BlockModelRenderState.EMPTY_TINTS;
        }

        int[] tints = new int[leavesTinted ? LEAVES_TINT_INDEX + 1 : TRUNK_TINT_INDEX + 1];
        java.util.Arrays.fill(tints, 0xFFFFFFFF);
        if (trunkTinted) {
            tints[TRUNK_TINT_INDEX] = resolveBlockTint(
                    trunkBlockId, level, pos, cached.trunkTintIndex());
        }
        if (leavesTinted) {
            tints[LEAVES_TINT_INDEX] = resolveBlockTint(
                    leavesBlockId, level, pos, cached.leavesTintIndex());
        }
        return tints;
    }

    private static int resolveBlockTint(
            @Nullable Identifier blockId,
            BlockAndTintGetter level,
            BlockPos pos,
            int tintIndex
    ) {
        // The material cache intentionally stores only baked metadata. Resolve
        // the actual block state here because foliage colors depend on biome and
        // position, while the quad cache remains safely shareable.
        if (blockId == null) {
            return 0xFFFFFFFF;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        if (block == Blocks.AIR) {
            return 0xFFFFFFFF;
        }
        BlockTintSource tintSource = Minecraft.getInstance().getBlockColors()
                .getTintSource(block.defaultBlockState(), tintIndex);
        int tint = tintSource == null
                ? -1
                : tintSource.colorInWorld(block.defaultBlockState(), level, pos);
        return tint == -1 ? 0xFFFFFFFF : tint;
    }

    /**
     * Drops all cached parts and sprites. Called when models are re-baked
     * (resource reload), since baked quads and atlas sprites are replaced.
     */
    public static void invalidateCaches() {
        PART_CACHE.clear();
        MATERIAL_CACHE.clear();
        baseLogSprite = null;
        baseLeavesSprite = null;
        blockModelResolver = null;
    }

    public static class State extends BlockEntityRenderState {
        public int rotation = 0;
        public List<BlockStateModelPart> modelParts = List.of();
        public boolean hasTranslucency = false;
        public int[] tintLayers = BlockModelRenderState.EMPTY_TINTS;
    }
}
