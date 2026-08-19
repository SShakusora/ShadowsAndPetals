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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WoodPillarBlock extends RotatedPillarBlock implements BlockOutlineProvider, SimpleWaterloggedBlock {
    public static final MapCodec<WoodPillarBlock> CODEC = simpleCodec(WoodPillarBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double CENTER = 8.0D;
    private static final double CORNER_CUT = 4.68629D;
    private static final double OCTAGON_LIMIT = 16.0D - CORNER_CUT;
    private static final VoxelShape X_SHAPE = createShape(Direction.Axis.X);
    private static final VoxelShape Y_SHAPE = createShape(Direction.Axis.Y);
    private static final VoxelShape Z_SHAPE = createShape(Direction.Axis.Z);
    private static final OutlineGeometry Y_OUTLINE = OutlineGeometry.octagonalPrism(
            0.0D,
            0.0D,
            16.0D,
            16.0D,
            CORNER_CUT,
            0.0D,
            16.0D
    );
    private static final OutlineGeometry X_OUTLINE = rotateOutline(Direction.Axis.X);
    private static final OutlineGeometry Z_OUTLINE = rotateOutline(Direction.Axis.Z);

    public WoodPillarBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(WATERLOGGED, false)
                .setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return shapeFor(state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                AXIS,
                context.getClickedFace().getAxis()
        ).setValue(
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
        builder.add(WATERLOGGED, AXIS);
    }

    @Override
    public OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X_OUTLINE;
            case Y -> Y_OUTLINE;
            case Z -> Z_OUTLINE;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(AXIS)) {
            case X -> X_SHAPE;
            case Y -> Y_SHAPE;
            case Z -> Z_SHAPE;
        };
    }

    private static VoxelShape createShape(Direction.Axis axis) {
        VoxelShape shape = Shapes.empty();

        for (int longitudinal = 0; longitudinal < 16; longitudinal++) {
            for (int crossSecond = 0; crossSecond < 16; crossSecond++) {
                double secondCenter = crossSecond + 0.5D;
                int minCrossFirst = 16;
                int maxCrossFirst = -1;

                for (int crossFirst = 0; crossFirst < 16; crossFirst++) {
                    double firstCenter = crossFirst + 0.5D;
                    if (Math.abs(firstCenter - CENTER) + Math.abs(secondCenter - CENTER) <= OCTAGON_LIMIT) {
                        minCrossFirst = Math.min(minCrossFirst, crossFirst);
                        maxCrossFirst = Math.max(maxCrossFirst, crossFirst);
                    }
                }

                if (maxCrossFirst < minCrossFirst) {
                    continue;
                }

                switch (axis) {
                    case X -> shape = Shapes.or(shape, Block.box(
                            longitudinal,
                            minCrossFirst,
                            crossSecond,
                            longitudinal + 1.0D,
                            maxCrossFirst + 1.0D,
                            crossSecond + 1.0D
                    ));
                    case Y -> shape = Shapes.or(shape, Block.box(
                            minCrossFirst,
                            longitudinal,
                            crossSecond,
                            maxCrossFirst + 1.0D,
                            longitudinal + 1.0D,
                            crossSecond + 1.0D
                    ));
                    case Z -> shape = Shapes.or(shape, Block.box(
                            minCrossFirst,
                            crossSecond,
                            longitudinal,
                            maxCrossFirst + 1.0D,
                            crossSecond + 1.0D,
                            longitudinal + 1.0D
                    ));
                }
            }
        }

        return shape.optimize();
    }

    private static OutlineGeometry rotateOutline(Direction.Axis axis) {
        return OutlineGeometry.of(Y_OUTLINE.lines().stream()
                .map(line -> new OutlineGeometry.Line(
                        transformOutlinePoint(line.from(), axis),
                        transformOutlinePoint(line.to(), axis)
                ))
                .toList());
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private static Vec3 transformOutlinePoint(Vec3 point, Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(point.y, 16.0D - point.x, point.z);
            case Y -> point;
            case Z -> new Vec3(point.x, 16.0D - point.z, point.y);
        };
    }
}
