package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class ShishiOdoshiPipeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ShishiOdoshiPipeBlock> CODEC = simpleCodec(ShishiOdoshiPipeBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<PipeLength> LENGTH = EnumProperty.create("length", PipeLength.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final Map<PipeLength, Map<Direction, VoxelShape>> SHAPES = buildShapes();

    public ShishiOdoshiPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LENGTH, PipeLength.NORMAL)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<ShishiOdoshiPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        return level.getBlockState(sourcePos).isFaceSturdy(level, sourcePos, facing, SupportType.FULL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LENGTH, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction pipeFacing = context.getHorizontalDirection().getOpposite();
        PipeLength length = computePipeLength(context.getLevel(), context.getClickedPos(), pipeFacing);

        return defaultBlockState()
                .setValue(FACING, pipeFacing)
                .setValue(LENGTH, length)
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    private static PipeLength computePipeLength(LevelReader level, BlockPos pos, Direction pipeFacing) {
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() instanceof ShishiOdoshiBlock) {
            Direction shishiFacing = below.getValue(ShishiOdoshiBlock.FACING).getOpposite();
            if (pipeFacing == shishiFacing) {
                return PipeLength.SHORT;
            }
            if (pipeFacing == shishiFacing.getOpposite()) {
                return PipeLength.LONG;
            }
            if (pipeFacing == shishiFacing.getClockWise()) {
                return PipeLength.NORMAL_RIGHT;
            }
            if (pipeFacing == shishiFacing.getCounterClockWise()) {
                return PipeLength.NORMAL_LEFT;
            }
            return PipeLength.NORMAL;
        }
        return PipeLength.NORMAL;
    }

    @Override
    protected BlockState updateShape(
            BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random
    ) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (direction == Direction.DOWN) {
            PipeLength updatedLength = computePipeLength(level, pos, state.getValue(FACING));
            state = state.setValue(LENGTH, updatedLength);
        }
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(LENGTH)).get(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.SHISHI_ODOSHI_PIPE.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    private static Map<PipeLength, Map<Direction, VoxelShape>> buildShapes() {
        Map<PipeLength, Map<Direction, VoxelShape>> shapes = new EnumMap<>(PipeLength.class);
        shapes.put(PipeLength.SHORT, VoxelShapeUtils.rotateHorizontal(Shapes.or(
                Block.box(6.5D, 0.0D, 11.0D, 9.5D, 1.0D, 16.0D),
                Block.box(6.5D, 2.0D, 13.0D, 9.5D, 3.0D, 16.0D),
                Block.box(6.5D, 1.0D, 12.0D, 7.5D, 2.0D, 16.0D),
                Block.box(8.5D, 1.0D, 12.0D, 9.5D, 2.0D, 16.0D)
        )));
        shapes.put(PipeLength.NORMAL, VoxelShapeUtils.rotateHorizontal(Shapes.or(
                Block.box(6.5D, 0.0D, 8.0D, 9.5D, 1.0D, 16.0D),
                Block.box(6.5D, 2.0D, 10.0D, 9.5D, 3.0D, 16.0D),
                Block.box(6.5D, 1.0D, 9.0D, 7.5D, 2.0D, 16.0D),
                Block.box(8.5D, 1.0D, 9.0D, 9.5D, 2.0D, 16.0D)
        )));
        shapes.put(PipeLength.NORMAL_LEFT, VoxelShapeUtils.rotateHorizontal(Shapes.or(
                Block.box(3.0D, 0.0D, 8.0D, 6.0D, 1.0D, 16.0D),
                Block.box(3.0D, 2.0D, 10.0D, 6.0D, 3.0D, 16.0D),
                Block.box(3.0D, 1.0D, 9.0D, 4.0D, 2.0D, 16.0D),
                Block.box(5.0D, 1.0D, 9.0D, 6.0D, 2.0D, 16.0D)
        )));
        shapes.put(PipeLength.NORMAL_RIGHT, VoxelShapeUtils.rotateHorizontal(Shapes.or(
                Block.box(10.0D, 0.0D, 8.0D, 13.0D, 1.0D, 16.0D),
                Block.box(10.0D, 2.0D, 10.0D, 13.0D, 3.0D, 16.0D),
                Block.box(10.0D, 1.0D, 9.0D, 11.0D, 2.0D, 16.0D),
                Block.box(12.0D, 1.0D, 9.0D, 13.0D, 2.0D, 16.0D)
        )));
        shapes.put(PipeLength.LONG, VoxelShapeUtils.rotateHorizontal(Shapes.or(
                Block.box(6.5D, 0.0D, 4.0D, 9.5D, 1.0D, 16.0D),
                Block.box(6.5D, 2.0D, 6.0D, 9.5D, 3.0D, 16.0D),
                Block.box(6.5D, 1.0D, 5.0D, 7.5D, 2.0D, 16.0D),
                Block.box(8.5D, 1.0D, 5.0D, 9.5D, 2.0D, 16.0D)
        )));
        return shapes;
    }

    public enum PipeLength implements StringRepresentable {
        SHORT("short"),
        NORMAL("normal"),
        NORMAL_LEFT("normal_left"),
        NORMAL_RIGHT("normal_right"),
        LONG("long");

        private final String name;

        PipeLength(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
