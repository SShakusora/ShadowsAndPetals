package com.sshakusora.shadowsandpetals.client.interaction;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class AdjacentShapeTargeting {
    private static final double CLIP_EPSILON = 1.0E-5;

    private AdjacentShapeTargeting() {
    }

    static HitResult correct(
            HitResult original,
            Entity cameraEntity,
            float partialTick,
            BlockPos[] candidateOffsets,
            CandidateClipper clipper
    ) {
        Level level = cameraEntity.level();
        Vec3 from = cameraEntity.getEyePosition(partialTick);
        Vec3 viewVector = cameraEntity.getViewVector(partialTick);
        Vec3 to = original.getLocation().add(viewVector.scale(CLIP_EPSILON));
        double closestDistanceSquared = from.distanceToSqr(original.getLocation()) + CLIP_EPSILON;
        BlockHitResult closestCandidateHit = null;

        Vec3 rayDelta = to.subtract(from);
        Vec3 rayStart = from.add(rayDelta.scale(CLIP_EPSILON));
        Vec3 rayEnd = to.subtract(rayDelta.scale(CLIP_EPSILON));
        double deltaX = rayEnd.x - rayStart.x;
        double deltaY = rayEnd.y - rayStart.y;
        double deltaZ = rayEnd.z - rayStart.z;
        int currentX = Mth.floor(rayStart.x);
        int currentY = Mth.floor(rayStart.y);
        int currentZ = Mth.floor(rayStart.z);
        int endX = Mth.floor(rayEnd.x);
        int endY = Mth.floor(rayEnd.y);
        int endZ = Mth.floor(rayEnd.z);
        int stepX = Double.compare(deltaX, 0.0);
        int stepY = Double.compare(deltaY, 0.0);
        int stepZ = Double.compare(deltaZ, 0.0);
        double deltaStepX = stepX == 0 ? Double.MAX_VALUE : Math.abs(1.0 / deltaX);
        double deltaStepY = stepY == 0 ? Double.MAX_VALUE : Math.abs(1.0 / deltaY);
        double deltaStepZ = stepZ == 0 ? Double.MAX_VALUE : Math.abs(1.0 / deltaZ);
        double maxX = nextBoundary(currentX, stepX);
        double maxY = nextBoundary(currentY, stepY);
        double maxZ = nextBoundary(currentZ, stepZ);
        double nextX = stepX == 0 ? Double.MAX_VALUE : Math.abs((maxX - rayStart.x) / deltaX);
        double nextY = stepY == 0 ? Double.MAX_VALUE : Math.abs((maxY - rayStart.y) / deltaY);
        double nextZ = stepZ == 0 ? Double.MAX_VALUE : Math.abs((maxZ - rayStart.z) / deltaZ);
        LongOpenHashSet visitedCandidates = new LongOpenHashSet();

        while (true) {
            BlockPos currentPos = new BlockPos(currentX, currentY, currentZ);
            for (BlockPos offset : candidateOffsets) {
                BlockPos candidatePos = currentPos.offset(offset);
                if (!visitedCandidates.add(candidatePos.asLong())) {
                    continue;
                }

                BlockHitResult candidateHit = clipper.clip(level, candidatePos, cameraEntity, from, to);
                if (candidateHit == null) {
                    continue;
                }

                double candidateDistanceSquared = from.distanceToSqr(candidateHit.getLocation());
                if (candidateDistanceSquared <= closestDistanceSquared) {
                    closestDistanceSquared = candidateDistanceSquared;
                    closestCandidateHit = candidateHit;
                }
            }

            if (currentX == endX && currentY == endY && currentZ == endZ) {
                break;
            }
            if (nextX < nextY) {
                if (nextX < nextZ) {
                    currentX += stepX;
                    nextX += deltaStepX;
                } else {
                    currentZ += stepZ;
                    nextZ += deltaStepZ;
                }
            } else if (nextY < nextZ) {
                currentY += stepY;
                nextY += deltaStepY;
            } else {
                currentZ += stepZ;
                nextZ += deltaStepZ;
            }
        }

        return closestCandidateHit != null ? closestCandidateHit : original;
    }

    private static double nextBoundary(int blockCoordinate, int step) {
        return step > 0 ? blockCoordinate + 1.0 : blockCoordinate;
    }

    @FunctionalInterface
    interface CandidateClipper {
        @Nullable
        BlockHitResult clip(Level level, BlockPos pos, Entity cameraEntity, Vec3 from, Vec3 to);
    }
}
