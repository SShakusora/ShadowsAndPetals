package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.CopperTeapotBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class CopperTeapotBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CopperTeapotBlock> CODEC = simpleCodec(CopperTeapotBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty ON_IRORI = BooleanProperty.create("on_irori");
    public static final double IRORI_RENDER_OFFSET = 5.0D / 16.0D;

    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(5.0D, 0.0D, 5.0D, 6.0D, 1.0D, 11.0D),
            Block.box(10.0D, 0.0D, 5.0D, 11.0D, 1.0D, 11.0D),
            Block.box(6.0D, 0.0D, 5.0D, 10.0D, 1.0D, 6.0D),
            Block.box(6.0D, 0.0D, 10.0D, 10.0D, 1.0D, 11.0D),
            Block.box(
                    7.0D, 1.9888237732D, 0.9085126678D,
                    9.0D, 5.3673165676D, 5.3693976626D
            ),
            Block.box(4.5D, 1.0D, 4.5D, 5.5D, 5.0D, 11.5D),
            Block.box(10.5D, 1.0D, 4.5D, 11.5D, 5.0D, 11.5D),
            Block.box(5.5D, 1.0D, 10.5D, 10.5D, 5.0D, 11.5D),
            Block.box(5.5D, 1.0D, 4.5D, 10.5D, 5.0D, 5.5D),
            Block.box(5.5D, 1.0D, 5.5D, 10.5D, 2.0D, 10.5D)
    );
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> ON_IRORI_SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.putAll(VoxelShapeUtils.rotateHorizontal(NORTH_SHAPE));
        ON_IRORI_SHAPES.putAll(VoxelShapeUtils.rotateHorizontal(
                NORTH_SHAPE.move(0.0D, IRORI_RENDER_OFFSET, 0.0D)));
    }

    public CopperTeapotBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(ON_IRORI, false));
    }

    @Override
    protected MapCodec<CopperTeapotBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperTeapotBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return createTickerHelper(type, BlockEntityRegistry.COPPER_TEAPOT.get(), CopperTeapotBlockEntity::tick);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos placementPos = context.getClickedPos();
        if (context.getLevel().getBlockEntity(placementPos.below()) instanceof IroriBlockEntity irori
                && irori.hasCookingItem(placementPos.below())) {
            return null;
        }

        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(placementPos).getType() == Fluids.WATER)
                .setValue(ON_IRORI, context.getLevel().getBlockState(placementPos.below()).getBlock() instanceof IroriBlock);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CopperTeapotBlockEntity teapot) {
            player.openMenu(teapot);
            player.awardStat(Stats.OPEN_BARREL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean onIrori = level.getBlockState(pos.below()).getBlock() instanceof IroriBlock;
        if (state.getValue(ON_IRORI) != onIrori) {
            state = state.setValue(ON_IRORI, onIrori);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(pos) instanceof CopperTeapotBlockEntity teapot) {
            teapot.recheckOpen();
        }
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
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
        if (direction == Direction.DOWN) {
            state = state.setValue(ON_IRORI, neighborState.getBlock() instanceof IroriBlock);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, ON_IRORI);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Map<Direction, VoxelShape> shapes = state.getValue(ON_IRORI) ? ON_IRORI_SHAPES : SHAPES;
        return shapes.get(state.getValue(FACING));
    }
}
