package com.sshakusora.shadowsandpetals.block.nature;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.block.decoration.VerticalSlabBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class LeavesVerticalSlabBlock extends VerticalSlabBlock {
    public static final MapCodec<LeavesVerticalSlabBlock> CODEC = simpleCodec(LeavesVerticalSlabBlock::new);

    public LeavesVerticalSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<LeavesVerticalSlabBlock> codec() {
        return CODEC;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }
}
