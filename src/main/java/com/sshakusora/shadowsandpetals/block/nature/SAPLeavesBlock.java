package com.sshakusora.shadowsandpetals.block.nature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class SAPLeavesBlock extends LeavesBlock {
    private final boolean usesCustomFallingLeafParticle;
    private final Supplier<ParticleOptions> fallingLeafParticleSupplier;

    public static final MapCodec<SAPLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("leaf_particle_chance").forGetter(block -> block.leafParticleChance),
                    propertiesCodec()
            ).apply(instance, SAPLeavesBlock::new)
    );

    public SAPLeavesBlock(float leafParticleChance, BlockBehaviour.Properties properties) {
        super(leafParticleChance, properties);
        this.usesCustomFallingLeafParticle = false;
        this.fallingLeafParticleSupplier = () -> ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0);
    }

    public SAPLeavesBlock(float leafParticleChance, BlockBehaviour.Properties properties, Supplier<? extends ParticleOptions> fallingLeafParticleSupplier) {
        super(leafParticleChance, properties);
        this.usesCustomFallingLeafParticle = true;
        this.fallingLeafParticleSupplier = fallingLeafParticleSupplier::get;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleOptions particle = this.usesCustomFallingLeafParticle
                ? this.fallingLeafParticleSupplier.get()
                : ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, level.getClientLeafTintColor(pos));
        ParticleUtils.spawnParticleBelow(level, pos, random, particle);
    }

    @Override
    public MapCodec<SAPLeavesBlock> codec() {
        return CODEC;
    }
}
