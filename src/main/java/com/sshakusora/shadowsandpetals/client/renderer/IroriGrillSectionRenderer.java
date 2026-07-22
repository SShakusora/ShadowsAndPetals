package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

import java.util.ArrayList;
import java.util.List;

public final class IroriGrillSectionRenderer {
    private static final long MODEL_SEED = 42L;
    private static final double GRILL_Y_OFFSET = 10.0D / 16.0D;
    private static final Direction[] DIRECTIONS = Direction.values();

    private IroriGrillSectionRenderer() {
    }

    public static void addSectionGeometry(AddSectionGeometryEvent event) {
        BlockPos sectionOrigin = event.getSectionOrigin();
        int sectionY = SectionPos.blockToSectionCoord(sectionOrigin.getY());
        List<GrillSnapshot> snapshots = new ArrayList<>();

        for (BlockEntity blockEntity : event.getLevel().getChunkAt(sectionOrigin).getBlockEntities().values()) {
            if (!(blockEntity instanceof IroriBlockEntity irori)
                    || SectionPos.blockToSectionCoord(irori.getBlockPos().getY()) != sectionY) {
                continue;
            }

            IroriBlockEntity.GrillRenderInfo grillInfo = irori.getGrillRenderInfo();
            if (grillInfo == null || !canRenderInSection(irori.getBlockPos(), grillInfo)) {
                continue;
            }

            BlockStateModel model = BlockModelRegistry.getIroriGrillModel(grillInfo.model());
            if (model == null) {
                continue;
            }

            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(
                    (BlockAndTintGetter) event.getLevel(),
                    irori.getBlockPos(),
                    irori.getBlockState(),
                    RandomSource.create(MODEL_SEED),
                    parts
            );
            if (!parts.isEmpty()) {
                snapshots.add(new GrillSnapshot(
                        irori.getBlockPos().immutable(),
                        grillInfo.offsetX(),
                        grillInfo.offsetZ(),
                        grillInfo.rotated(),
                        List.copyOf(parts)
                ));
            }
        }

        if (!snapshots.isEmpty()) {
            BlockPos immutableOrigin = sectionOrigin.immutable();
            List<GrillSnapshot> immutableSnapshots = List.copyOf(snapshots);
            event.addRenderer(context -> {
                for (GrillSnapshot snapshot : immutableSnapshots) {
                    renderGrill(context, immutableOrigin, snapshot);
                }
            });
        }
    }

    public static boolean canRenderInSection(
            BlockPos masterPos,
            IroriBlockEntity.GrillRenderInfo grillInfo
    ) {
        int width;
        int depth;
        switch (grillInfo.model()) {
            case ONE_BY_ONE -> {
                width = 1;
                depth = 1;
            }
            case ONE_BY_TWO -> {
                width = grillInfo.rotated() ? 2 : 1;
                depth = grillInfo.rotated() ? 1 : 2;
            }
            case TWO_BY_TWO -> {
                width = 2;
                depth = 2;
            }
            default -> throw new IllegalStateException("Unknown grill model: " + grillInfo.model());
        }

        return isSameSection(masterPos.getX(), masterPos.getX() + width - 1)
                && isSameSection(masterPos.getY(), masterPos.getY() + 1)
                && isSameSection(masterPos.getZ(), masterPos.getZ() + depth - 1);
    }

    private static boolean isSameSection(int min, int max) {
        return SectionPos.blockToSectionCoord(min) == SectionPos.blockToSectionCoord(max);
    }

    private static void renderGrill(
            AddSectionGeometryEvent.SectionRenderingContext context,
            BlockPos sectionOrigin,
            GrillSnapshot snapshot
    ) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(
                snapshot.pos().getX() - sectionOrigin.getX() + snapshot.offsetX(),
                snapshot.pos().getY() - sectionOrigin.getY() + GRILL_Y_OFFSET,
                snapshot.pos().getZ() - sectionOrigin.getZ() + snapshot.offsetZ()
        );
        if (snapshot.rotated()) {
            poseStack.translate(0.5D, 0.0D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.translate(-0.5D, 0.0D, -0.5D);
        }

        QuadInstance instance = new QuadInstance();
        instance.setLightCoords(LevelRenderer.getLightCoords(context.getRegion(), snapshot.pos()));
        PoseStack.Pose pose = poseStack.last();

        for (BlockStateModelPart part : snapshot.parts()) {
            for (Direction direction : DIRECTIONS) {
                putQuads(context, pose, instance, part.getQuads(direction));
            }
            putQuads(context, pose, instance, part.getQuads(null));
        }
    }

    private static void putQuads(
            AddSectionGeometryEvent.SectionRenderingContext context,
            PoseStack.Pose pose,
            QuadInstance instance,
            List<BakedQuad> quads
    ) {
        for (BakedQuad quad : quads) {
            VertexConsumer buffer = context.getOrCreateChunkBuffer(quad.materialInfo().layer());
            buffer.putBakedQuad(pose, quad, instance);
        }
    }

    private record GrillSnapshot(
            BlockPos pos,
            double offsetX,
            double offsetZ,
            boolean rotated,
            List<BlockStateModelPart> parts
    ) {
    }
}
