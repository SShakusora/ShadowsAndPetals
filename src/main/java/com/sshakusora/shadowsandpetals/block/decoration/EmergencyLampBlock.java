package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class EmergencyLampBlock extends DirectionalBlock {
    public static final MapCodec<EmergencyLampBlock> CODEC = simpleCodec(EmergencyLampBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final Map<Direction, VoxelShape> SHAPES;

    static {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.UP, box(4, 0, 4, 12, 12, 12));
        shapes.put(Direction.DOWN, box(4, 4, 4, 12, 16, 12));
        shapes.put(Direction.NORTH, box(4, 4, 4, 12, 12, 16));
        shapes.put(Direction.SOUTH, box(4, 4, 0, 12, 12, 12));
        shapes.put(Direction.EAST, box(0, 4, 4, 12, 12, 12));
        shapes.put(Direction.WEST, box(4, 4, 4, 16, 12, 12));
        SHAPES = Collections.unmodifiableMap(shapes);
    }

    public EmergencyLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<EmergencyLampBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return LampToggle.toggle(state, level, pos, player, LIT);
    }
}
