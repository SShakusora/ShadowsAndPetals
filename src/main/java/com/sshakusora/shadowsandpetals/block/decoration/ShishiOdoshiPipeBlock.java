package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.api.shishiOdoshi.ShishiOdoshiFluidRegistry;
import com.sshakusora.shadowsandpetals.blockentity.ShishiOdoshiPipeBlockEntity;
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
    public static final int MAX_VERTICAL_CONNECTION_DISTANCE = 32;
    public static final int CONNECTION_RECHECK_INTERVAL_TICKS = 10;
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
        BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isFaceSturdy(level, sourcePos, facing, SupportType.FULL)) {
            return true;
        }
        return sourceState.getBlock() instanceof AbstractCauldronBlock;
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
        BlockPos shishiOdoshiPos = findShishiOdoshiBelow(level, pos);
        if (shishiOdoshiPos != null) {
            return computePipeLength(
                    pipeFacing,
                    level.getBlockState(shishiOdoshiPos).getValue(ShishiOdoshiBlock.FACING)
            );
        }
        return PipeLength.NORMAL;
    }

    public static BlockState updatePipeLength(LevelReader level, BlockPos pos, BlockState state) {
        PipeLength updatedLength = computePipeLength(level, pos, state.getValue(FACING));
        return state.setValue(LENGTH, updatedLength);
    }

    public static @Nullable BlockPos findShishiOdoshiBelow(LevelReader level, BlockPos pipePos) {
        for (int distance = 1; distance <= MAX_VERTICAL_CONNECTION_DISTANCE; distance++) {
            BlockPos candidatePos = pipePos.below(distance);
            if (level.isOutsideBuildHeight(candidatePos)) {
                return null;
            }

            BlockState candidateState = level.getBlockState(candidatePos);
            if (candidateState.getBlock() instanceof ShishiOdoshiBlock) {
                return candidatePos;
            }
            if (!candidateState.getCollisionShape(level, candidatePos).isEmpty()) {
                return null;
            }
        }
        return null;
    }

    public static PipeLength computePipeLength(Direction pipeFacing, Direction shishiOdoshiFacing) {
        Direction inletFacing = shishiOdoshiFacing.getOpposite();
        if (pipeFacing == inletFacing) {
            return PipeLength.SHORT;
        }
        if (pipeFacing == inletFacing.getOpposite()) {
            return PipeLength.LONG;
        }
        if (pipeFacing == inletFacing.getClockWise()) {
            return PipeLength.NORMAL_RIGHT;
        }
        if (pipeFacing == inletFacing.getCounterClockWise()) {
            return PipeLength.NORMAL_LEFT;
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
        if (direction == state.getValue(FACING).getOpposite() && level instanceof Level blockLevel) {
            blockLevel.getLightEngine().checkBlock(pos);
        }
        if (direction == Direction.DOWN) {
            state = updatePipeLength(level, pos, state);
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
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return getSuppliedFluidLightEmission(state, level, pos);
    }

    public static int getSuppliedFluidLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        var fluid = ShishiOdoshiFluidRegistry.findSourceFluid(level, sourcePos);
        return fluid == null
                ? 0
                : fluid.defaultFluidState().createLegacyBlock().getLightEmission(level, sourcePos);
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
        if (level.isClientSide()) {
            return createTickerHelper(type, BlockEntityRegistry.SHISHI_ODOSHI_PIPE.get(), ShishiOdoshiPipeBlockEntity::clientTick);
        }
        return createTickerHelper(type, BlockEntityRegistry.SHISHI_ODOSHI_PIPE.get(), ShishiOdoshiPipeBlockEntity::serverTick);
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
        SHORT("short", 8.0F / 16.0F, 11.0F / 16.0F),
        NORMAL("normal", 8.0F / 16.0F, 8.0F / 16.0F),
        NORMAL_LEFT("normal_left", 4.5F / 16.0F, 8.0F / 16.0F),
        NORMAL_RIGHT("normal_right", 11.5F / 16.0F, 8.0F / 16.0F),
        LONG("long", 8.0F / 16.0F, 4.0F / 16.0F);

        private final String name;
        private final float outletX;
        private final float outletZ;

        PipeLength(String name, float outletX, float outletZ) {
            this.name = name;
            this.outletX = outletX;
            this.outletZ = outletZ;
        }

        public float outletX() {
            return outletX;
        }

        public float outletZ() {
            return outletZ;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
