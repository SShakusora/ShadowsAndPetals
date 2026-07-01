package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.api.ShishiOdoshiFluidRegistry;
import com.sshakusora.shadowsandpetals.block.decoration.ShishiOdoshiBlock;
import com.sshakusora.shadowsandpetals.block.decoration.ShishiOdoshiPipeBlock;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class ShishiOdoshiBlockEntity extends BlockEntity {
    private static final String FLUID_AMOUNT_KEY = "WaterAmount";
    private static final String FLUID_KEY = "Fluid";
    private static final String ANIMATION_PHASE_KEY = "AnimationPhase";
    private static final String ANIMATION_TICK_KEY = "AnimationTickMilli";
    private static final String LEGACY_ANIMATION_TICK_KEY = "AnimationTick";
    private static final String POUR_TICK_KEY = "PourTickMilli";

    public static final int WATER_CAPACITY = 100;
    public static final int TIPPING_DURATION = 24;
    public static final int RETURNING_DURATION = 8;
    public static final int BOUNCING_DURATION = 12;
    public static final int POUR_START_TICK = 20;
    public static final int POUR_DURATION = 7;
    public static final int POUR_IMPACT_TICK = 5;
    public static final float MAX_TIP_ANGLE = -34.5F;
    public static final float MAX_BOUNCE_ANGLE = -6.0F;

    private int fluidAmount;
    private Fluid fluid = Fluids.WATER;
    private AnimationPhase animationPhase = AnimationPhase.FILLING;
    private float animationTick;
    private float pourTick = -1.0F;
    private boolean clientSplashSpawned;
    private @Nullable BlockPos cachedPipePos;
    private long nextPipeCheckTick = Long.MIN_VALUE;

    public ShishiOdoshiBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.SHISHI_ODOSHI.get(), pos, blockState);
    }

    /** Exposes the current water amount (0–{@link #WATER_CAPACITY}) for capability providers. */
    public int getWaterAmount() {
        return fluidAmount;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShishiOdoshiBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.tickAnimation(false);
            blockEntity.tickClientEffects(level, pos, state);
            return;
        }

        if (blockEntity.animationPhase == AnimationPhase.FILLING) {
            Fluid flowingFluid = blockEntity.getFlowingPipeFluid(level, pos, state);
            if (flowingFluid != null) {
                if (blockEntity.fluidAmount > 0 && blockEntity.fluid != flowingFluid) {
                    blockEntity.fluidAmount = 0;
                }
                blockEntity.fluid = flowingFluid;
                blockEntity.fluidAmount++;
                if (blockEntity.fluid == Fluids.WATER
                        && blockEntity.fluidAmount > 0
                        && blockEntity.fluidAmount % 30 == 0) {
                    level.playSound(null, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.12F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                }
                if (blockEntity.fluidAmount >= WATER_CAPACITY) {
                    blockEntity.fluidAmount = WATER_CAPACITY;
                    blockEntity.animationPhase = AnimationPhase.TIPPING;
                    blockEntity.animationTick = 0;
                    blockEntity.setChanged();
                    blockEntity.syncToClient();
                } else {
                    blockEntity.setChanged();
                }
            }
            return;
        }

        blockEntity.tickAnimation(true);

        if (blockEntity.animationPhase == AnimationPhase.BOUNCING && blockEntity.animationTick == 0) {
            var random = level.getRandom();
            float volume = 0.9F + random.nextFloat() * 0.1F;
            float pitch = 0.9F + random.nextFloat() * 0.2F;
            level.playSound(null, pos, SoundRegistry.SHISHI_ODOSHI.get(), SoundSource.BLOCKS, volume, pitch);
        }
    }

    private void tickAnimation(boolean syncTransitions) {
        if (animationPhase == AnimationPhase.FILLING) {
            return;
        }

        float previousAnimationTick = animationTick;
        animationTick += getAnimationSpeed();
        tickPouringStream(previousAnimationTick);
        int duration = getPhaseDuration(animationPhase);
        if (animationTick < duration) {
            return;
        }

        animationTick = 0;
        if (animationPhase == AnimationPhase.TIPPING) {
            animationPhase = AnimationPhase.RETURNING;
        } else if (animationPhase == AnimationPhase.RETURNING) {
            animationPhase = AnimationPhase.BOUNCING;
        } else {
            animationPhase = AnimationPhase.FILLING;
            fluidAmount = 0;
            pourTick = -1.0F;
        }
        setChanged();
        if (syncTransitions) {
            syncToClient();
        }
    }

    private @Nullable Fluid getFlowingPipeFluid(Level level, BlockPos pos, BlockState state) {
        long gameTime = level.getGameTime();
        if (gameTime >= nextPipeCheckTick) {
            cachedPipePos = findPipeAbove(level, pos);
            nextPipeCheckTick = gameTime + ShishiOdoshiPipeBlock.CONNECTION_RECHECK_INTERVAL_TICKS;
        }
        if (cachedPipePos == null) {
            return null;
        }

        BlockPos pipePos = cachedPipePos;
        BlockState pipeState = level.getBlockState(pipePos);
        if (!(pipeState.getBlock() instanceof ShishiOdoshiPipeBlock)) {
            cachedPipePos = null;
            nextPipeCheckTick = gameTime;
            return null;
        }

        ShishiOdoshiPipeBlock.PipeLength desiredLength = ShishiOdoshiPipeBlock.computePipeLength(
                pipeState.getValue(ShishiOdoshiPipeBlock.FACING),
                state.getValue(ShishiOdoshiBlock.FACING)
        );
        if (pipeState.getValue(ShishiOdoshiPipeBlock.LENGTH) != desiredLength) {
            pipeState = pipeState.setValue(ShishiOdoshiPipeBlock.LENGTH, desiredLength);
            level.setBlock(pipePos, pipeState, Block.UPDATE_CLIENTS);
        }

        BlockPos sourcePos = pipePos.relative(pipeState.getValue(ShishiOdoshiPipeBlock.FACING).getOpposite());
        return ShishiOdoshiFluidRegistry.findSourceFluid(level, sourcePos);
    }

    private static @Nullable BlockPos findPipeAbove(Level level, BlockPos shishiOdoshiPos) {
        for (int distance = 1; distance <= ShishiOdoshiPipeBlock.MAX_VERTICAL_CONNECTION_DISTANCE; distance++) {
            BlockPos candidatePos = shishiOdoshiPos.above(distance);
            if (level.isOutsideBuildHeight(candidatePos)) {
                return null;
            }

            BlockState candidateState = level.getBlockState(candidatePos);
            if (candidateState.getBlock() instanceof ShishiOdoshiPipeBlock) {
                return candidatePos;
            }
            if (!candidateState.getCollisionShape(level, candidatePos).isEmpty()) {
                return null;
            }
        }
        return null;
    }

    public float getTipAngle(float partialTick) {
        if (animationPhase == AnimationPhase.FILLING) {
            return 0.0F;
        }

        int duration = getPhaseDuration(animationPhase);
        float progress = Mth.clamp(
                (animationTick + partialTick * getAnimationSpeed()) / duration,
                0.0F,
                1.0F
        );
        if (animationPhase == AnimationPhase.TIPPING) {
            return MAX_TIP_ANGLE * progress * progress;
        }
        if (animationPhase == AnimationPhase.RETURNING) {
            return MAX_TIP_ANGLE * (1.0F - progress);
        }
        return MAX_BOUNCE_ANGLE * Mth.sin((float) Math.PI * progress);
    }

    public float getPourProgress(float partialTick) {
        if (pourTick < 0.0F) {
            return -1.0F;
        }
        float elapsed = pourTick;
        if (elapsed < POUR_DURATION) {
            elapsed += partialTick * ShishiOdoshiFluidRegistry.getAnimationSpeed(fluid);
        }
        return Mth.clamp(elapsed / POUR_DURATION, 0.0F, 1.0F);
    }

    private void tickPouringStream(float previousAnimationTick) {
        float flowSpeed = ShishiOdoshiFluidRegistry.getAnimationSpeed(fluid);
        if (animationPhase == AnimationPhase.TIPPING
                && previousAnimationTick < POUR_START_TICK
                && animationTick >= POUR_START_TICK) {
            pourTick = animationTick - POUR_START_TICK;
        } else if (pourTick >= 0.0F && pourTick < POUR_DURATION) {
            pourTick += flowSpeed;
        }
    }

    private void tickClientEffects(Level level, BlockPos pos, BlockState state) {
        if (fluid != Fluids.WATER) {
            clientSplashSpawned = false;
            return;
        }
        float pourProgress = getPourProgress(0.0F);
        if (pourProgress < 0.0F) {
            clientSplashSpawned = false;
            return;
        }
        float impactProgress = POUR_IMPACT_TICK / (float) POUR_DURATION;
        if (pourProgress < impactProgress || clientSplashSpawned) {
            return;
        }

        clientSplashSpawned = true;
        var facing = state.getValue(ShishiOdoshiBlock.FACING);
        double impactX = pos.getX() + 0.5D + facing.getStepX() * 5.0D / 16.0D;
        double impactY = pos.getY() + 3.1D / 16.0D;
        double impactZ = pos.getZ() + 0.5D + facing.getStepZ() * 5.0D / 16.0D;
        var random = level.getRandom();
        for (int i = 0; i < 7; i++) {
            level.addParticle(
                    ParticleTypes.SPLASH,
                    impactX + (random.nextDouble() - 0.5D) * 2.0D / 16.0D,
                    impactY,
                    impactZ + (random.nextDouble() - 0.5D) * 2.0D / 16.0D,
                    (random.nextDouble() - 0.5D) * 0.08D,
                    0.06D + random.nextDouble() * 0.06D,
                    (random.nextDouble() - 0.5D) * 0.08D
            );
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (fluidAmount > 0) {
            output.putInt(FLUID_AMOUNT_KEY, fluidAmount);
            output.putString(FLUID_KEY, BuiltInRegistries.FLUID.getKey(fluid).toString());
        }
        if (animationPhase != AnimationPhase.FILLING) {
            output.putInt(ANIMATION_PHASE_KEY, animationPhase.ordinal());
            output.putInt(ANIMATION_TICK_KEY, Math.round(animationTick * 1000.0F));
        }
        if (pourTick >= 0.0F) {
            output.putInt(POUR_TICK_KEY, Math.round(pourTick * 1000.0F));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluidAmount = Mth.clamp(input.getInt(FLUID_AMOUNT_KEY).orElse(0), 0, WATER_CAPACITY);
        fluid = input.getString(FLUID_KEY)
                .map(Identifier::tryParse)
                .map(BuiltInRegistries.FLUID::getValue)
                .filter(value -> value != Fluids.EMPTY)
                .orElse(Fluids.WATER);
        int phaseOrdinal = input.getInt(ANIMATION_PHASE_KEY).orElse(AnimationPhase.FILLING.ordinal());
        animationPhase = phaseOrdinal >= 0 && phaseOrdinal < AnimationPhase.values().length
                ? AnimationPhase.values()[phaseOrdinal]
                : AnimationPhase.FILLING;
        int duration = getPhaseDuration(animationPhase);
        float savedAnimationTick = input.getInt(ANIMATION_TICK_KEY)
                .map(value -> value / 1000.0F)
                .orElseGet(() -> input.getInt(LEGACY_ANIMATION_TICK_KEY).orElse(0).floatValue());
        animationTick = Mth.clamp(
                savedAnimationTick,
                0.0F,
                duration - 0.001F
        );
        pourTick = input.getInt(POUR_TICK_KEY)
                .map(value -> value / 1000.0F)
                .orElseGet(this::getLegacyPourTick);
    }

    private static int getPhaseDuration(AnimationPhase phase) {
        return switch (phase) {
            case FILLING -> 1;
            case TIPPING -> TIPPING_DURATION;
            case RETURNING -> RETURNING_DURATION;
            case BOUNCING -> BOUNCING_DURATION;
        };
    }

    private float getAnimationSpeed() {
        return animationPhase == AnimationPhase.TIPPING
                ? ShishiOdoshiFluidRegistry.getAnimationSpeed(fluid)
                : 1.0F;
    }

    private float getLegacyPourTick() {
        return switch (animationPhase) {
            case FILLING -> -1.0F;
            case TIPPING -> animationTick < POUR_START_TICK ? -1.0F : animationTick - POUR_START_TICK;
            case RETURNING -> TIPPING_DURATION - POUR_START_TICK + animationTick;
            case BOUNCING -> TIPPING_DURATION - POUR_START_TICK + RETURNING_DURATION + animationTick;
        };
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    private enum AnimationPhase {
        FILLING,
        TIPPING,
        RETURNING,
        BOUNCING
    }

    public static class FluidHandler implements ResourceHandler<FluidResource> {
        private static final int CAPACITY_MB = WATER_CAPACITY;

        private final ShishiOdoshiBlockEntity blockEntity;

        public FluidHandler(ShishiOdoshiBlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            if (index != 0 || blockEntity.getWaterAmount() <= 0) {
                return FluidResource.EMPTY;
            }
            return FluidResource.of(blockEntity.getFluid());
        }

        @Override
        public long getAmountAsLong(int index) {
            if (index != 0) return 0;
            return blockEntity.getWaterAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            if (index != 0) return 0;
            return CAPACITY_MB;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return index == 0 && resource.is(blockEntity.getFluid());
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }
    }
}
