package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.block.decoration.ShishiOdoshiBlock;
import com.sshakusora.shadowsandpetals.block.decoration.ShishiOdoshiPipeBlock;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class ShishiOdoshiBlockEntity extends BlockEntity {
    private static final String WATER_AMOUNT_KEY = "WaterAmount";
    private static final String ANIMATION_PHASE_KEY = "AnimationPhase";
    private static final String ANIMATION_TICK_KEY = "AnimationTick";

    public static final int WATER_CAPACITY = 100;
    public static final int TIPPING_DURATION = 24;
    public static final int RETURNING_DURATION = 8;
    public static final int BOUNCING_DURATION = 12;
    public static final int POUR_START_TICK = 20;
    public static final int POUR_DURATION = 7;
    public static final int POUR_IMPACT_TICK = 5;
    public static final float MAX_TIP_ANGLE = -34.5F;
    public static final float MAX_BOUNCE_ANGLE = -6.0F;

    private int waterAmount;
    private AnimationPhase animationPhase = AnimationPhase.FILLING;
    private int animationTick;
    private boolean clientSplashSpawned;

    public ShishiOdoshiBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.SHISHI_ODOSHI.get(), pos, blockState);
    }

    /** Exposes the current water amount (0–{@link #WATER_CAPACITY}) for capability providers. */
    public int getWaterAmount() {
        return waterAmount;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShishiOdoshiBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.tickAnimation(false);
            blockEntity.tickClientEffects(level, pos, state);
            return;
        }

        if (blockEntity.animationPhase == AnimationPhase.FILLING) {
            if (hasFlowingPipe(level, pos.above())) {
                blockEntity.waterAmount++;
                if (blockEntity.waterAmount > 0 && blockEntity.waterAmount % 30 == 0) {
                    level.playSound(null, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.12F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                }
                if (blockEntity.waterAmount >= WATER_CAPACITY) {
                    blockEntity.waterAmount = WATER_CAPACITY;
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

        animationTick++;
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
            waterAmount = 0;
        }
        setChanged();
        if (syncTransitions) {
            syncToClient();
        }
    }

    private static boolean hasFlowingPipe(Level level, BlockPos pipePos) {
        BlockState pipeState = level.getBlockState(pipePos);
        if (!(pipeState.getBlock() instanceof ShishiOdoshiPipeBlock)) {
            return false;
        }

        BlockPos sourcePos = pipePos.relative(pipeState.getValue(ShishiOdoshiPipeBlock.FACING).getOpposite());
        BlockState sourceState = level.getBlockState(sourcePos);
        return sourceState.hasProperty(BlockStateProperties.WATERLOGGED)
                && sourceState.getValue(BlockStateProperties.WATERLOGGED);
    }

    public float getTipAngle(float partialTick) {
        if (animationPhase == AnimationPhase.FILLING) {
            return 0.0F;
        }

        int duration = getPhaseDuration(animationPhase);
        float progress = Mth.clamp((animationTick + partialTick) / duration, 0.0F, 1.0F);
        if (animationPhase == AnimationPhase.TIPPING) {
            return MAX_TIP_ANGLE * progress * progress;
        }
        if (animationPhase == AnimationPhase.RETURNING) {
            return MAX_TIP_ANGLE * (1.0F - progress);
        }
        return MAX_BOUNCE_ANGLE * Mth.sin((float) Math.PI * progress);
    }

    public float getPourProgress(float partialTick) {
        float elapsed = switch (animationPhase) {
            case FILLING -> -1.0F;
            case TIPPING -> animationTick + partialTick - POUR_START_TICK;
            case RETURNING -> TIPPING_DURATION - POUR_START_TICK + animationTick + partialTick;
            case BOUNCING -> TIPPING_DURATION - POUR_START_TICK + RETURNING_DURATION + animationTick + partialTick;
        };
        return elapsed < 0.0F ? -1.0F : Mth.clamp(elapsed / POUR_DURATION, 0.0F, 1.0F);
    }

    private void tickClientEffects(Level level, BlockPos pos, BlockState state) {
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
        if (waterAmount > 0) {
            output.putInt(WATER_AMOUNT_KEY, waterAmount);
        }
        if (animationPhase != AnimationPhase.FILLING) {
            output.putInt(ANIMATION_PHASE_KEY, animationPhase.ordinal());
            output.putInt(ANIMATION_TICK_KEY, animationTick);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        waterAmount = Mth.clamp(input.getInt(WATER_AMOUNT_KEY).orElse(0), 0, WATER_CAPACITY);
        int phaseOrdinal = input.getInt(ANIMATION_PHASE_KEY).orElse(AnimationPhase.FILLING.ordinal());
        animationPhase = phaseOrdinal >= 0 && phaseOrdinal < AnimationPhase.values().length
                ? AnimationPhase.values()[phaseOrdinal]
                : AnimationPhase.FILLING;
        int duration = getPhaseDuration(animationPhase);
        animationTick = Mth.clamp(input.getInt(ANIMATION_TICK_KEY).orElse(0), 0, duration - 1);
    }

    private static int getPhaseDuration(AnimationPhase phase) {
        return switch (phase) {
            case FILLING -> 1;
            case TIPPING -> TIPPING_DURATION;
            case RETURNING -> RETURNING_DURATION;
            case BOUNCING -> BOUNCING_DURATION;
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
        private static final int SCALE = 10;
        private static final int CAPACITY_MB = WATER_CAPACITY * SCALE;

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
            return FluidResource.of(Fluids.WATER);
        }

        @Override
        public long getAmountAsLong(int index) {
            if (index != 0) return 0;
            return (long) blockEntity.getWaterAmount() * SCALE;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            if (index != 0) return 0;
            return CAPACITY_MB;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return index == 0 && resource.is(Fluids.WATER);
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
