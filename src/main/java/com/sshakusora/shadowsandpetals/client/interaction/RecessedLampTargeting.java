package com.sshakusora.shadowsandpetals.client.interaction;

import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public final class RecessedLampTargeting {
    private static final double CLIP_EPSILON = 1.0E-5;

    private RecessedLampTargeting() {
    }

    public static HitResult correct(HitResult original, Entity cameraEntity, float partialTick) {
        Level level = cameraEntity.level();
        Vec3 from = cameraEntity.getEyePosition(partialTick);
        Vec3 viewVector = cameraEntity.getViewVector(partialTick);
        Vec3 to = original.getLocation().add(viewVector.scale(CLIP_EPSILON));
        double closestDistanceSquared = from.distanceToSqr(original.getLocation()) + CLIP_EPSILON;
        BlockHitResult closestLampHit = null;

        Vec3 rayStart = from.add(to.subtract(from).scale(CLIP_EPSILON));
        Vec3 rayEnd = to.subtract(to.subtract(from).scale(CLIP_EPSILON));
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

        while (true) {
            BlockPos currentPos = new BlockPos(currentX, currentY, currentZ);
            BlockHitResult lampHit = clipLampAt(level, currentPos, cameraEntity, from, to);
            if (lampHit != null) {
                double lampDistanceSquared = from.distanceToSqr(lampHit.getLocation());
                if (lampDistanceSquared <= closestDistanceSquared) {
                    closestDistanceSquared = lampDistanceSquared;
                    closestLampHit = lampHit;
                }
            }
            lampHit = clipLampAt(level, currentPos.above(), cameraEntity, from, to);
            if (lampHit != null) {
                double lampDistanceSquared = from.distanceToSqr(lampHit.getLocation());
                if (lampDistanceSquared <= closestDistanceSquared) {
                    closestDistanceSquared = lampDistanceSquared;
                    closestLampHit = lampHit;
                }
            }
            lampHit = clipLampAt(level, currentPos.below(), cameraEntity, from, to);
            if (lampHit != null) {
                double lampDistanceSquared = from.distanceToSqr(lampHit.getLocation());
                if (lampDistanceSquared <= closestDistanceSquared) {
                    closestDistanceSquared = lampDistanceSquared;
                    closestLampHit = lampHit;
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

        return closestLampHit != null ? closestLampHit : original;
    }

    private static double nextBoundary(int blockCoordinate, int step) {
        return step > 0 ? blockCoordinate + 1.0 : blockCoordinate;
    }

    private static @Nullable BlockHitResult clipLampAt(
            Level level,
            BlockPos lampPos,
            Entity cameraEntity,
            Vec3 from,
            Vec3 to
    ) {
        BlockState lampState = level.getBlockState(lampPos);
        if (!(lampState.getBlock() instanceof RecessedLampBlock)) {
            return null;
        }

        RecessedLampBlock.Mount mount = lampState.getValue(RecessedLampBlock.MOUNT);
        if (mount != RecessedLampBlock.Mount.FLOOR_SLAB
                && mount != RecessedLampBlock.Mount.CEILING_SLAB) {
            return null;
        }

        BlockPos immutableLampPos = lampPos.immutable();
        return lampState.getShape(level, immutableLampPos, CollisionContext.of(cameraEntity))
                .clip(from, to, immutableLampPos);
    }
}
