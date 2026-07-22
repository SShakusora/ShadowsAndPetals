package com.sshakusora.shadowsandpetals.client.effect;

import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriComponentTopology;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState.FirewoodModel;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState.FirewoodModelStage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public final class IroriClientEffects {
    private static final float FIREWOOD_APPEAR_ANIMATION_SPEED = 0.4F;
    private static final double FIREWOOD_EFFECT_Y = 12.0D / 16.0D;
    private static final double FIREWOOD_FLAME_SPREAD = 3.0D / 16.0D;
    private static final double FIREWOOD_SMOKE_SPREAD = 4.0D / 16.0D;

    private static final Map<IroriBlockEntity, State> STATES = new WeakHashMap<>();

    private IroriClientEffects() {
    }

    public static void tick(IroriBlockEntity blockEntity, Level level, BlockState blockState) {
        if (!level.isClientSide() || !blockEntity.isValidMaster()) {
            return;
        }

        State state = stateFor(blockEntity);
        state.tickFirewoodAppearAnimation(blockEntity.shouldRenderFirewood());
        state.tickFirewoodModelChangeEffects(blockEntity, level, blockState);
        tickBurningEffects(blockEntity, level, blockState);
    }

    public static float getFirewoodAppearProgress(IroriBlockEntity blockEntity, float partialTick) {
        return stateFor(blockEntity).getFirewoodAppearProgress(partialTick);
    }

    private static State stateFor(IroriBlockEntity blockEntity) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(blockEntity, ignored -> new State());
        }
    }

    private static void tickBurningEffects(IroriBlockEntity blockEntity, Level level, BlockState blockState) {
        if (blockEntity.getBurnTime() <= 0
                || blockEntity.getFirewoodModel() == null
                || blockState.getValue(IroriBlock.WATERLOGGED)) {
            return;
        }

        RandomSource random = level.getRandom();
        FirewoodEffectOrigin origin = getFirewoodEffectOrigin(blockEntity);

        if (random.nextInt(16) == 0) {
            addFireParticle(level, random, origin.x(), origin.y(), origin.z(), origin.flameSpreadX(), origin.flameSpreadZ());
        }
        if (random.nextInt(90) == 0) {
            addFireParticle(level, random, origin.x(), origin.y() + 0.03D, origin.z(), origin.flameSpreadX(), origin.flameSpreadZ());
        }
        if (random.nextInt(12) == 0) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    offset(random, origin.x(), origin.smokeSpreadX()),
                    origin.y() + 0.18D + random.nextDouble() * 0.08D,
                    offset(random, origin.z(), origin.smokeSpreadZ()),
                    random.nextGaussian() * 0.004D,
                    0.025D + random.nextDouble() * 0.015D,
                    random.nextGaussian() * 0.004D
            );
        }
        if (random.nextInt(45) == 0) {
            level.addParticle(
                    ParticleTypes.ASH,
                    offset(random, origin.x(), origin.smokeSpreadX()),
                    origin.y() + 0.12D,
                    offset(random, origin.z(), origin.smokeSpreadZ()),
                    random.nextGaussian() * 0.01D,
                    0.01D + random.nextDouble() * 0.015D,
                    random.nextGaussian() * 0.01D
            );
        }
        if (random.nextInt(150) == 0) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    offset(random, origin.x(), origin.flameSpreadX() * 0.65D),
                    origin.y() + 0.08D,
                    offset(random, origin.z(), origin.flameSpreadZ() * 0.65D),
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
        if (random.nextInt(90) == 0) {
            level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.18F, 0.75F + random.nextFloat() * 0.25F, false);
        } else if (random.nextInt(180) == 0) {
            level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.08F, 1.8F + random.nextFloat() * 0.25F, false);
        }
    }

    private static FirewoodEffectOrigin getFirewoodEffectOrigin(IroriBlockEntity blockEntity) {
        IroriBlockEntity.FirewoodRenderOffset renderOffset = blockEntity.getFirewoodRenderOffset();
        IroriComponentTopology.Layout layout = blockEntity.getComponentLayout();
        double centerX = blockEntity.getBlockPos().getX() + 0.5D + renderOffset.x();
        double centerY = blockEntity.getBlockPos().getY() + FIREWOOD_EFFECT_Y;
        double centerZ = blockEntity.getBlockPos().getZ() + 0.5D + renderOffset.z();
        double flameSpreadX = Math.min(FIREWOOD_FLAME_SPREAD * layout.width(), 0.5D);
        double flameSpreadZ = Math.min(FIREWOOD_FLAME_SPREAD * layout.depth(), 0.5D);
        double smokeSpreadX = Math.min(FIREWOOD_SMOKE_SPREAD * layout.width(), 0.65D);
        double smokeSpreadZ = Math.min(FIREWOOD_SMOKE_SPREAD * layout.depth(), 0.65D);
        return new FirewoodEffectOrigin(centerX, centerY, centerZ, flameSpreadX, flameSpreadZ, smokeSpreadX, smokeSpreadZ);
    }

    private static void spawnFirewoodShrinkEffects(Level level, RandomSource random, FirewoodEffectOrigin origin) {
        for (int i = 0; i < 3; i++) {
            level.addParticle(
                    ParticleTypes.ASH,
                    offset(random, origin.x(), origin.smokeSpreadX() * 0.75D),
                    origin.y() + 0.08D + random.nextDouble() * 0.12D,
                    offset(random, origin.z(), origin.smokeSpreadZ() * 0.75D),
                    random.nextGaussian() * 0.018D,
                    0.018D + random.nextDouble() * 0.025D,
                    random.nextGaussian() * 0.018D
            );
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    offset(random, origin.x(), origin.smokeSpreadX()),
                    origin.y() + 0.18D + random.nextDouble() * 0.12D,
                    offset(random, origin.z(), origin.smokeSpreadZ()),
                    random.nextGaussian() * 0.01D,
                    0.035D + random.nextDouble() * 0.02D,
                    random.nextGaussian() * 0.01D
            );
        }
        if (random.nextBoolean()) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    offset(random, origin.x(), origin.flameSpreadX() * 0.55D),
                    origin.y() + 0.08D,
                    offset(random, origin.z(), origin.flameSpreadZ() * 0.55D),
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
        level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.32F, 0.6F + random.nextFloat() * 0.16F, false);
    }

    private static void spawnFirewoodAshEffects(Level level, RandomSource random, FirewoodEffectOrigin origin) {
        for (int i = 0; i < 5; i++) {
            level.addParticle(
                    random.nextBoolean() ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH,
                    offset(random, origin.x(), origin.smokeSpreadX()),
                    origin.y() + 0.04D + random.nextDouble() * 0.16D,
                    offset(random, origin.z(), origin.smokeSpreadZ()),
                    random.nextGaussian() * 0.02D,
                    0.012D + random.nextDouble() * 0.025D,
                    random.nextGaussian() * 0.02D
            );
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    offset(random, origin.x(), origin.smokeSpreadX() * 0.8D),
                    origin.y() + 0.1D + random.nextDouble() * 0.12D,
                    offset(random, origin.z(), origin.smokeSpreadZ() * 0.8D),
                    random.nextGaussian() * 0.012D,
                    0.025D + random.nextDouble() * 0.02D,
                    random.nextGaussian() * 0.012D
            );
        }
        level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.16F, 1.8F + random.nextFloat() * 0.2F, false);
        level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.SAND_FALL, SoundSource.BLOCKS, 0.22F, 0.7F + random.nextFloat() * 0.16F, false);
    }

    private static void addFireParticle(
            Level level,
            RandomSource random,
            double centerX,
            double centerY,
            double centerZ,
            double spreadX,
            double spreadZ
    ) {
        level.addParticle(
                random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                offset(random, centerX, spreadX),
                centerY + random.nextDouble() * 0.1D,
                offset(random, centerZ, spreadZ),
                random.nextGaussian() * 0.003D,
                0.01D + random.nextDouble() * 0.01D,
                random.nextGaussian() * 0.003D
        );
    }

    private static double offset(RandomSource random, double center, double spread) {
        return center + (random.nextDouble() * 2.0D - 1.0D) * spread;
    }

    private static final class State {
        private boolean firewoodAppearAnimationInitialized;
        private boolean hadVisibleFirewood;
        private boolean firewoodEffectModelInitialized;
        private @Nullable FirewoodModel previousFirewoodEffectModel;
        private float firewoodAppearProgress = 1.0F;
        private float firewoodAppearProgressOld = 1.0F;

        private float getFirewoodAppearProgress(float partialTick) {
            return firewoodAppearProgressOld
                    + (firewoodAppearProgress - firewoodAppearProgressOld) * partialTick;
        }

        private void tickFirewoodAppearAnimation(boolean visibleFirewood) {
            firewoodAppearProgressOld = firewoodAppearProgress;

            if (!firewoodAppearAnimationInitialized) {
                firewoodAppearAnimationInitialized = true;
                hadVisibleFirewood = visibleFirewood;
                firewoodAppearProgress = visibleFirewood ? 1.0F : 0.0F;
                firewoodAppearProgressOld = firewoodAppearProgress;
                return;
            }

            if (visibleFirewood && !hadVisibleFirewood) {
                firewoodAppearProgress = 0.0F;
                firewoodAppearProgressOld = 0.0F;
            }
            hadVisibleFirewood = visibleFirewood;

            if (visibleFirewood) {
                firewoodAppearProgress = Math.min(
                        1.0F,
                        firewoodAppearProgress + FIREWOOD_APPEAR_ANIMATION_SPEED
                );
            } else {
                firewoodAppearProgress = 0.0F;
                firewoodAppearProgressOld = 0.0F;
            }
        }

        private void tickFirewoodModelChangeEffects(
                IroriBlockEntity blockEntity,
                Level level,
                BlockState blockState
        ) {
            FirewoodModel firewoodModel = blockEntity.getFirewoodModel();
            if (firewoodModel == null || blockState.getValue(IroriBlock.WATERLOGGED)) {
                firewoodEffectModelInitialized = false;
                previousFirewoodEffectModel = firewoodModel;
                return;
            }

            if (!firewoodEffectModelInitialized) {
                firewoodEffectModelInitialized = true;
                previousFirewoodEffectModel = firewoodModel;
                return;
            }
            if (previousFirewoodEffectModel == firewoodModel) {
                return;
            }

            FirewoodModelStage previousStage = previousFirewoodEffectModel == null
                    ? null
                    : previousFirewoodEffectModel.stage();
            FirewoodModelStage currentStage = firewoodModel.stage();
            RandomSource random = level.getRandom();
            FirewoodEffectOrigin origin = getFirewoodEffectOrigin(blockEntity);

            if (previousStage == FirewoodModelStage.LIT
                    && currentStage == FirewoodModelStage.SMALL_LIT) {
                spawnFirewoodShrinkEffects(level, random, origin);
            } else if (previousStage == FirewoodModelStage.SMALL_LIT
                    && currentStage == FirewoodModelStage.ASH) {
                spawnFirewoodAshEffects(level, random, origin);
            }

            previousFirewoodEffectModel = firewoodModel;
        }
    }

    private record FirewoodEffectOrigin(
            double x,
            double y,
            double z,
            double flameSpreadX,
            double flameSpreadZ,
            double smokeSpreadX,
            double smokeSpreadZ
    ) {
    }
}
