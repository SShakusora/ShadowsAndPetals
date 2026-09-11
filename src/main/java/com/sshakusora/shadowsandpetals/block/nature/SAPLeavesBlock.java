package com.sshakusora.shadowsandpetals.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class SAPLeavesBlock extends LeavesBlock {
    public static final MapCodec<SAPLeavesBlock> CODEC = simpleCodec(SAPLeavesBlock::new);

    public SAPLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public SAPLeavesBlock(float ignoredLeafParticleChance, BlockBehaviour.Properties properties) {
        this(properties);
    }

    public SAPLeavesBlock(float ignoredLeafParticleChance, BlockBehaviour.Properties properties,
                          Supplier<? extends ParticleOptions> ignoredParticleSupplier) {
        this(properties);
    }

    @Override
    public MapCodec<SAPLeavesBlock> codec() {
        return CODEC;
    }
}