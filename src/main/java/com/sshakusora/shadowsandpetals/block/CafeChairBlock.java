package com.sshakusora.shadowsandpetals.block;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CafeChairBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CafeChairBlock> CODEC = simpleCodec(CafeChairBlock::new);
    private static final double SEAT_HEIGHT = 0.4375D;
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D),
            Block.box(4.0D, 1.0D, 4.0D, 12.0D, 8.0D, 12.0D),
            Block.box(4.0D, 8.0D, 10.0D, 12.0D, 16.0D, 12.0D),
            Block.box(4.0D, 0.0D, 4.0D, 6.0D, 8.0D, 6.0D),
            Block.box(10.0D, 0.0D, 4.0D, 12.0D, 8.0D, 6.0D),
            Block.box(4.0D, 0.0D, 10.0D, 6.0D, 8.0D, 12.0D),
            Block.box(10.0D, 0.0D, 10.0D, 12.0D, 8.0D, 12.0D)
    );
    private static final VoxelShape SHAPE_EAST = rotateShape(SHAPE_NORTH, Direction.EAST);
    private static final VoxelShape SHAPE_SOUTH = rotateShape(SHAPE_NORTH, Direction.SOUTH);
    private static final VoxelShape SHAPE_WEST = rotateShape(SHAPE_NORTH, Direction.WEST);

    public CafeChairBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<CafeChairBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return trySit(level, pos, player);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = trySit(level, pos, player);
        if (result.consumesAction()) {
            return result == InteractionResult.SUCCESS
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.CONSUME;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    private InteractionResult trySit(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        SeatEntity seat = SeatEntity.getOrCreate(level, pos, SEAT_HEIGHT);
        if (seat == null || !seat.canBeSatOn()) {
            return InteractionResult.CONSUME;
        }

        player.startRiding(seat, false);
        return InteractionResult.CONSUME;
    }

    private static VoxelShape rotateShape(VoxelShape shape, Direction direction) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
        int rotations = switch (direction) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };

        for (int i = 0; i < rotations; i++) {
            buffer[1] = Shapes.empty();
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    buffer[1] = Shapes.or(buffer[1], Block.box(
                            16.0D - maxZ * 16.0D,
                            minY * 16.0D,
                            minX * 16.0D,
                            16.0D - minZ * 16.0D,
                            maxY * 16.0D,
                            maxX * 16.0D
                    ))
            );
            buffer[0] = buffer[1];
        }
        return buffer[0];
    }
}
