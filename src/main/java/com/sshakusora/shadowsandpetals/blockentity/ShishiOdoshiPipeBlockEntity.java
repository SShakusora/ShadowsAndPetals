package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.api.shishiOdoshi.ShishiOdoshiFluidRegistry;
import com.sshakusora.shadowsandpetals.block.decoration.ShishiOdoshiPipeBlock;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class ShishiOdoshiPipeBlockEntity extends BlockEntity {
    private static final long SPLASH_INTERVAL_TICKS = 5L;
    private static final double CLIP_START_EPSILON = 1.0E-4D;

    private @Nullable BlockPos cachedShishiOdoshiPos;
    private long nextConnectionCheckTick = Long.MIN_VALUE;
    private @Nullable Vec3 cachedFallbackImpactPosition;
    private long nextImpactCheckTick = Long.MIN_VALUE;
    private long nextLengthCheckTick = Long.MIN_VALUE;
    private long lastSplashTick = Long.MIN_VALUE;

    public ShishiOdoshiPipeBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.SHISHI_ODOSHI_PIPE.get(), pos, blockState);
    }

    public @Nullable ShishiOdoshiBlockEntity getConnectedShishiOdoshi() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }

        long gameTime = level.getGameTime();
        if (gameTime >= nextConnectionCheckTick) {
            cachedShishiOdoshiPos = ShishiOdoshiPipeBlock.findShishiOdoshiBelow(level, getBlockPos());
            nextConnectionCheckTick = gameTime + ShishiOdoshiPipeBlock.CONNECTION_RECHECK_INTERVAL_TICKS;
        }

        if (cachedShishiOdoshiPos == null) {
            return null;
        }
        if (level.getBlockEntity(cachedShishiOdoshiPos) instanceof ShishiOdoshiBlockEntity shishiOdoshi) {
            return shishiOdoshi;
        }

        cachedShishiOdoshiPos = null;
        nextConnectionCheckTick = gameTime;
        return null;
    }

    public @Nullable Vec3 getFallbackImpactPosition() {
        Level level = getLevel();
        if (level == null || getConnectedShishiOdoshi() != null) {
            return null;
        }

        long gameTime = level.getGameTime();
        if (gameTime >= nextImpactCheckTick) {
            cachedFallbackImpactPosition = findFallbackImpactPosition(level);
            nextImpactCheckTick = gameTime + ShishiOdoshiPipeBlock.CONNECTION_RECHECK_INTERVAL_TICKS;
        }
        return cachedFallbackImpactPosition;
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, ShishiOdoshiPipeBlockEntity blockEntity
    ) {
        long gameTime = level.getGameTime();
        if (gameTime < blockEntity.nextLengthCheckTick) {
            return;
        }

        blockEntity.nextLengthCheckTick = gameTime + ShishiOdoshiPipeBlock.CONNECTION_RECHECK_INTERVAL_TICKS;
        BlockState updatedState = ShishiOdoshiPipeBlock.updatePipeLength(level, pos, state);
        if (updatedState != state) {
            level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(
            Level level, BlockPos pos, BlockState state, ShishiOdoshiPipeBlockEntity blockEntity
    ) {
        Vec3 impactPosition = blockEntity.getFallbackImpactPosition();
        long gameTime = level.getGameTime();
        if (impactPosition == null
                || (blockEntity.lastSplashTick != Long.MIN_VALUE
                && gameTime - blockEntity.lastSplashTick < SPLASH_INTERVAL_TICKS)) {
            return;
        }

        Direction facing = state.getValue(ShishiOdoshiPipeBlock.FACING);
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        if (ShishiOdoshiFluidRegistry.findSourceFluid(level, sourcePos) != Fluids.WATER) {
            return;
        }

        blockEntity.lastSplashTick = gameTime;
        spawnWaterSplash(level, impactPosition);
    }

    private @Nullable Vec3 findFallbackImpactPosition(Level level) {
        BlockState state = getBlockState();
        ShishiOdoshiPipeBlock.PipeLength length = state.getValue(ShishiOdoshiPipeBlock.LENGTH);
        Direction facing = state.getValue(ShishiOdoshiPipeBlock.FACING);
        float angle = (-facing.toYRot() + 180.0F) * Mth.DEG_TO_RAD;
        double relativeX = length.outletX() - 0.5D;
        double relativeZ = length.outletZ() - 0.5D;
        double cos = Mth.cos(angle);
        double sin = Mth.sin(angle);
        double outletX = getBlockPos().getX() + 0.5D + cos * relativeX + sin * relativeZ;
        double outletZ = getBlockPos().getZ() + 0.5D - sin * relativeX + cos * relativeZ;
        double startY = getBlockPos().getY() - CLIP_START_EPSILON;
        double endY = Math.max(
                level.getMinY(),
                getBlockPos().getY() - ShishiOdoshiPipeBlock.MAX_VERTICAL_CONNECTION_DISTANCE
        );

        BlockHitResult hit = level.clip(new ClipContext(
                new Vec3(outletX, startY, outletZ),
                new Vec3(outletX, endY, outletZ),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    private static void spawnWaterSplash(Level level, Vec3 impactPosition) {
        var random = level.getRandom();
        for (int i = 0; i < 7; i++) {
            level.addParticle(
                    ParticleTypes.SPLASH,
                    impactPosition.x + (random.nextDouble() - 0.5D) * 2.0D / 16.0D,
                    impactPosition.y + 0.01D,
                    impactPosition.z + (random.nextDouble() - 0.5D) * 2.0D / 16.0D,
                    (random.nextDouble() - 0.5D) * 0.08D,
                    0.06D + random.nextDouble() * 0.06D,
                    (random.nextDouble() - 0.5D) * 0.08D
            );
        }
    }
}
