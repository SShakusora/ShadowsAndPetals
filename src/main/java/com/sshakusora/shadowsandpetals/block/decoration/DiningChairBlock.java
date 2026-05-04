package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class DiningChairBlock extends AbstractSeatBlock {
    public static final MapCodec<DiningChairBlock> CODEC = simpleCodec(DiningChairBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final double SEAT_HEIGHT = 0.5D;
    private static final Map<Direction, VoxelShape> SHAPES = VoxelShapeUtils.rotateHorizontal(Shapes.or(
            Block.box(2.0D, 0.0D, 2.0D, 4.0D, 16.0D, 4.0D),
            Block.box(12.0D, 0.0D, 2.0D, 14.0D, 16.0D, 4.0D),
            Block.box(2.0D, 0.0D, 12.0D, 4.0D, 16.0D, 14.0D),
            Block.box(12.0D, 0.0D, 12.0D, 14.0D, 16.0D, 14.0D),
            Block.box(2.0D, 16.0D, 12.0D, 4.0D, 32.0D, 14.0D),
            Block.box(12.0D, 16.0D, 12.0D, 14.0D, 32.0D, 14.0D),
            Block.box(2.0D, 6.0D, 2.0D, 14.0D, 8.0D, 14.0D),
            Block.box(3.0D, 8.0D, 11.0D, 13.0D, 20.0D, 13.0D),
            Block.box(3.0D, 20.0D, 12.0D, 13.0D, 32.0D, 14.0D)
    ));

    public DiningChairBlock(BlockBehaviour.Properties properties) {
        super(properties, SEAT_HEIGHT);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<DiningChairBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
