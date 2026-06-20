package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class SamonBlock extends Block {
    public static final MapCodec<SamonBlock> CODEC = simpleCodec(SamonBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CORNER = BooleanProperty.create("corner");

    public SamonBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CORNER, false));
    }

    @Override
    protected MapCodec<SamonBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CORNER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getClockWise()); //Special clockWise
    }

    public BlockState getStateForConnections(BlockGetter level, BlockPos pos) {
        boolean north = isSamon(level, pos.north());
        boolean east = isSamon(level, pos.east());
        boolean south = isSamon(level, pos.south());
        boolean west = isSamon(level, pos.west());

        if (north && south) {
            return connectedState(Direction.EAST, false);
        }
        if (east && west) {
            return connectedState(Direction.SOUTH, false);
        }

        if (south && east) {
            return connectedState(Direction.SOUTH, true);
        }
        if (south && west) {
            return connectedState(Direction.WEST, true);
        }
        if (north && west) {
            return connectedState(Direction.NORTH, true);
        }
        if (north && east) {
            return connectedState(Direction.EAST, true);
        }

        if (north || south) {
            return connectedState(Direction.EAST, false);
        }
        if (east || west) {
            return connectedState(Direction.SOUTH, false);
        }

        return defaultBlockState();
    }

    private BlockState connectedState(Direction facing, boolean corner) {
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(CORNER, corner);
    }

    private static boolean isSamon(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof SamonBlock;
    }
}
