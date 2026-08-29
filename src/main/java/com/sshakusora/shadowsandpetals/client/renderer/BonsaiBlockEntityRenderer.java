package com.sshakusora.shadowsandpetals.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.BonsaiBlock;
import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
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
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders bonsai pots with dynamic trunk/leaf textures.
 *
 * <p>The bonsai models are baked with placeholder textures (maple_log,
 * maple_leaves_0, bonsai_pot). At render time, quads are categorized by
 * comparing their sprite against the known base sprites, and the UVs are
 * remapped to sample the resolved tree's actual trunk/leaf textures.</p>
 */
public class BonsaiBlockEntityRenderer implements
        BlockEntityRenderer<BonsaiBlockEntity, BonsaiBlockEntityRenderer.State> {

    private static final RandomSource PART_RANDOM = RandomSource.create(42L);

    /**
     * The bonsai models were authored in Blockbench with a 90-degree yaw
     * error: the pot's long axis runs along model Z instead of X. This
     * compensation rotates every model (empty pot, living and dead trees —
     * they all funnel through {@code submit}) into the intended orientation.
     * Remove once the models are re-exported fixed.
     */
    private static final float MODEL_AUTHORING_ROTATION_DEGREES = 90.0F;

    // Base sprites from the bonsai model — used to identify which texture slot a quad uses
    private @Nullable TextureAtlasSprite baseLogSprite;
    private @Nullable TextureAtlasSprite baseLeavesSprite;
    private @Nullable TextureAtlasSprite basePotSprite;

    public BonsaiBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
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

        state.planted = blockEntity.isPlanted();

        // Resolve trunk/leaf sprites from the stored block IDs
        state.trunkSprite = resolveSprite(blockEntity.getTrunkBlockId());
        state.leavesSprite = blockEntity.isDead()
                ? null
                : resolveSprite(blockEntity.getLeavesBlockId());

        // Collect model parts
        BlockAndTintGetter level = (BlockAndTintGetter) blockEntity.getLevel();
        if (level == null) {
            state.modelParts = List.of();
            state.hasTranslucency = false;
            return;
        }

        BlockState blockState = blockEntity.getBlockState();
        state.rotation = blockState.getValue(BonsaiBlock.ROTATION);
        BlockPos pos = blockEntity.getBlockPos();

        BlockStateModel model;
        if (blockEntity.isPlanted()) {
            model = blockEntity.isDead()
                    ? BlockModelRegistry.BONSAI_DEAD_SHAPES.get(blockEntity.getShape())
                    : BlockModelRegistry.BONSAI_SHAPES.get(blockEntity.getShape());
        } else {
            model = BlockModelRegistry.BONSAI_EMPTY_POT.get();
        }

        if (model == null) {
            state.modelParts = List.of();
            state.hasTranslucency = false;
            return;
        }

        List<BlockStateModelPart> rawParts = new ArrayList<>();
        PART_RANDOM.setSeed(42L);
        model.collectParts(level, pos, blockState, PART_RANDOM, rawParts);
        state.hasTranslucency = model.hasMaterialFlag(
                level, pos, blockState, BakedQuad.FLAG_TRANSLUCENT
        );

        // Wrap parts with texture-remapping if planted
        if (state.planted && state.trunkSprite != null) {
            TextureAtlasSprite baseLog = getBaseLogSprite();
            TextureAtlasSprite baseLeaves = getBaseLeavesSprite();
            List<BlockStateModelPart> wrapped = new ArrayList<>(rawParts.size());
            for (BlockStateModelPart part : rawParts) {
                wrapped.add(new BonsaiPart(part, state.trunkSprite, state.leavesSprite,
                        baseLog, baseLeaves));
            }
            state.modelParts = List.copyOf(wrapped);
        } else {
            state.modelParts = List.copyOf(rawParts);
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
        poseStack.mulPose(Axis.YP.rotationDegrees(-RotationSegment.convertToDegrees(state.rotation)));
        poseStack.mulPose(Axis.YP.rotationDegrees(MODEL_AUTHORING_ROTATION_DEGREES));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        submitNodeCollector.submitMultiLayerBlockModel(
                poseStack,
                state.modelParts,
                state.hasTranslucency,
                BlockModelRenderState.EMPTY_TINTS,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }
    @Override
    public AABB getRenderBoundingBox(BonsaiBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        // The rendered tree extends well outside the block (up to ~2 blocks up
        // and ~1 block sideways); expand so it is not frustum-culled.
        return new AABB(
                pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 2.0D, pos.getZ() + 2.0D
        );
    }

    /**
     * Wraps a {@link BlockStateModelPart} and remaps quads from the base
     * log/leaves sprites to the resolved tree's sprites at render time.
     */
    private static final class BonsaiPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final TextureAtlasSprite trunkSprite;
        private final @Nullable TextureAtlasSprite leavesSprite;
        private final TextureAtlasSprite baseLogSprite;
        private final TextureAtlasSprite baseLeavesSprite;

        BonsaiPart(
                BlockStateModelPart delegate,
                TextureAtlasSprite trunkSprite,
                @Nullable TextureAtlasSprite leavesSprite,
                TextureAtlasSprite baseLogSprite,
                TextureAtlasSprite baseLeavesSprite
        ) {
            this.delegate = delegate;
            this.trunkSprite = trunkSprite;
            this.leavesSprite = leavesSprite;
            this.baseLogSprite = baseLogSprite;
            this.baseLeavesSprite = baseLeavesSprite;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            List<BakedQuad> quads = delegate.getQuads(direction);
            if (quads.isEmpty()) {
                return quads;
            }
            List<BakedQuad> result = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                result.add(remapQuad(quad));
            }
            return result;
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
            return delegate.materialFlags();
        }

        private BakedQuad remapQuad(BakedQuad quad) {
            TextureAtlasSprite quadSprite = quad.materialInfo().sprite();

            TextureAtlasSprite targetSprite;
            TextureAtlasSprite sourceSprite;

            if (spritesMatch(quadSprite, baseLogSprite)) {
                targetSprite = trunkSprite;
                sourceSprite = baseLogSprite;
            } else if (spritesMatch(quadSprite, baseLeavesSprite)) {
                if (leavesSprite == null) {
                    // Dead tree — skip leaves quads entirely
                    return quad;
                }
                targetSprite = leavesSprite;
                sourceSprite = baseLeavesSprite;
            } else {
                // Pot or other texture — leave as-is
                return quad;
            }

            return remapQuadSprite(quad, sourceSprite, targetSprite);
        }

        private static BakedQuad remapQuadSprite(
                BakedQuad quad,
                TextureAtlasSprite source,
                TextureAtlasSprite target
        ) {
            long[] newUVs = new long[4];
            for (int v = 0; v < 4; v++) {
                long packed = quad.packedUV(v);
                float atlasU = UVPair.unpackU(packed);
                float atlasV = UVPair.unpackV(packed);

                float localU = getUnInterpolatedU(source, atlasU);
                float localV = getUnInterpolatedV(source, atlasV);

                float targetU = target.getU(localU);
                float targetV = target.getV(localV);

                newUVs[v] = UVPair.pack(targetU, targetV);
            }

            BakedQuad.MaterialInfo mat = quad.materialInfo();
            return new BakedQuad(
                    quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                    newUVs[0], newUVs[1], newUVs[2], newUVs[3],
                    quad.direction(),
                    new BakedQuad.MaterialInfo(
                            target, mat.layer(), mat.itemRenderType(),
                            mat.tintIndex(), mat.shade(), mat.lightEmission(), mat.ambientOcclusion()
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

    private final BlockModelResolver blockModelResolver =
            new BlockModelResolver(Minecraft.getInstance().getModelManager());
    private final BlockModelRenderState blockModelRenderState = new BlockModelRenderState();

    private @Nullable TextureAtlasSprite resolveSprite(@Nullable Identifier blockId) {
        if (blockId == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        if (block == Blocks.AIR) {
            return null;
        }
        BlockState blockState = block.defaultBlockState();
        blockModelResolver.update(blockModelRenderState, blockState, DisplayRenderer.BLOCK_DISPLAY_CONTEXT);
        List<BlockStateModelPart> parts = blockModelRenderState.modelParts;
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        Material.Baked particleMaterial = parts.get(0).particleMaterial();
        if (particleMaterial == null) {
            return null;
        }
        return particleMaterial.sprite();
    }

    private TextureAtlasSprite getBaseLogSprite() {
        if (baseLogSprite == null) {
            baseLogSprite = getAtlasSprite(ShadowsAndPetals.asResource("block/maple_log"));
        }
        return baseLogSprite;
    }

    private TextureAtlasSprite getBaseLeavesSprite() {
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

    public static class State extends BlockEntityRenderState {
        public boolean planted = false;
        public int rotation = 0;
        public @Nullable TextureAtlasSprite trunkSprite;
        public @Nullable TextureAtlasSprite leavesSprite;
        public List<BlockStateModelPart> modelParts = List.of();
        public boolean hasTranslucency = false;
    }
}