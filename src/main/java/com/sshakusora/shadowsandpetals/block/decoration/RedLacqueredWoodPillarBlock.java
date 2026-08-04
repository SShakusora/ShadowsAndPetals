package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineProvider;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedLacqueredWoodPillarBlock extends Block implements BlockOutlineProvider, SimpleWaterloggedBlock {
    public static final MapCodec<RedLacqueredWoodPillarBlock> CODEC = simpleCodec(RedLacqueredWoodPillarBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double CENTER = 8.0D;
    private static final double CORNER_CUT = 4.68629D;
    private static final double OCTAGON_LIMIT = 16.0D - CORNER_CUT;
    private static final VoxelShape SHAPE = createShape();
    private static final OutlineGeometry OUTLINE = OutlineGeometry.octagonalPrism(
            0.0D,
            0.0D,
            16.0D,
            16.0D,
            CORNER_CUT,
            0.0D,
            16.0D
    );

    public RedLacqueredWoodPillarBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<RedLacqueredWoodPillarBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER
        );
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        return OUTLINE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static VoxelShape createShape() {
        VoxelShape shape = Shapes.empty();

        for (int z = 0; z < 16; z++) {
            double zCenter = z + 0.5D;
            int minX = 16;
            int maxX = -1;

            for (int x = 0; x < 16; x++) {
                double xCenter = x + 0.5D;
                if (Math.abs(xCenter - CENTER) + Math.abs(zCenter - CENTER) <= OCTAGON_LIMIT) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }

            if (maxX >= minX) {
                shape = Shapes.or(shape, Block.box(minX, 0.0D, z, maxX + 1.0D, 16.0D, z + 1.0D));
            }
        }

        return shape.optimize();
    }
}
