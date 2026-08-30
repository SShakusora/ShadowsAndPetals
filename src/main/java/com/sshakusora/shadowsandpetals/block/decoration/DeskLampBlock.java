package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class DeskLampBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DeskLampBlock> CODEC = simpleCodec(DeskLampBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE = Shapes.or(
            box(5, 0, 5, 11, 1, 11),
            box(7, 1, 8, 9, 2, 10),
            box(7.5, 2, 8.5, 8.5, 8, 9.5),
            box(7.5, 7.29289622, 3.85025193, 8.5, 12.94975047, 9.50710618),
            box(7, 8.46446609, 0.96447137, 9, 12.70710678, 5.20711206),
            box(5.63423345, 7.31779993, -0.18219493, 7.47628841, 12.35354827, 4.85355348),
            box(8.54740215, 7.31963053, -0.18036434, 10.38945643, 12.35537889, 4.85538403),
            box(6.5, 9.38093385, -0.65410802, 9.5, 12.37352373, 3.25675737),
            box(6.5, 6.85866458, 1.86095666, 9.5, 10.76952997, 4.85354654)
    ).optimize();

    private static final Map<Direction, VoxelShape> SHAPES = VoxelShapeUtils.rotateHorizontal(SHAPE);

    public DeskLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<DeskLampBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
