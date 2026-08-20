package com.sshakusora.shadowsandpetals.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Raw concrete keeps the dense/hole appearance as a normal block state so the
 * choice is saved with the chunk and synchronised to clients.
 */
public class RawConcreteBlock extends Block {
    public static final MapCodec<RawConcreteBlock> CODEC = simpleCodec(RawConcreteBlock::new);
    public static final BooleanProperty DENSE = BooleanProperty.create("dense");

    public RawConcreteBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DENSE, false));
    }

    @Override
    protected MapCodec<RawConcreteBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DENSE);
    }

    /**
     * Returns whether the specified face is one of the existing periodic hole
     * positions used by the connected-texture layout.
     */
    public static boolean isHolePosition(BlockPos pos, Direction face) {
        int first;
        int second;
        switch (face.getAxis()) {
            case X -> {
                first = pos.getY();
                second = pos.getZ();
            }
            case Y -> {
                first = pos.getX();
                second = pos.getZ();
            }
            case Z -> {
                first = pos.getX();
                second = pos.getY();
            }
            default -> throw new IllegalStateException("Unexpected direction axis: " + face.getAxis());
        }
        return Math.floorMod(first, 2) == 0 && Math.floorMod(second, 2) == 0;
    }

    /**
     * Maps a raw-concrete block state and face to the registered texture-sheet
     * index: normal, hole, or dense hole.
     */
    public static int selectTextureIndex(BlockState state, BlockPos pos, Direction face) {
        return selectTextureIndex(state.getValue(DENSE), pos, face);
    }

    public static int selectTextureIndex(boolean dense, BlockPos pos, Direction face) {
        if (!isHolePosition(pos, face)) {
            return 0;
        }
        return dense ? 2 : 1;
    }
}
