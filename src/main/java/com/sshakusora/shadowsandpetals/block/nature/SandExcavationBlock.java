package com.sshakusora.shadowsandpetals.block.nature;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.SandExcavationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class SandExcavationBlock extends BaseEntityBlock {
    public static final IntegerProperty DUSTED = BlockStateProperties.DUSTED;
    public static final MapCodec<SandExcavationBlock> CODEC = simpleCodec(SandExcavationBlock::new);
    private static final int TICK_DELAY = 2;

    public SandExcavationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DUSTED, 0));
    }

    @Override
    protected MapCodec<? extends SandExcavationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DUSTED);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, TICK_DELAY);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        ticks.scheduleTick(pos, this, TICK_DELAY);
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (FallingBlock.isFree(level.getBlockState(pos.below()))) {
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
            return;
        }

        if (level.getBlockEntity(pos) instanceof SandExcavationBlockEntity excavation) {
            excavation.checkReset(level);
        } else {
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SandExcavationBlockEntity(pos, state);
    }
}
