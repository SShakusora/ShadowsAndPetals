package com.sshakusora.shadowsandpetals.block.decoration.window;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CasementWindowBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CasementWindowBlock> CODEC =
            simpleCodec(CasementWindowBlock::new);
    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<CasementWindowSide> SIDE =
            EnumProperty.create("side", CasementWindowSide.class);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty ANIMATING = BooleanProperty.create("animating");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int ANIMATION_TICKS = 12;

    public CasementWindowBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(SIDE, CasementWindowSide.LEFT)
                .setValue(OPEN, false)
                .setValue(ANIMATING, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends CasementWindowBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, OPEN, ANIMATING, WATERLOGGED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(ANIMATING) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(SIDE, context.isSecondaryUseActive()
                        ? CasementWindowSide.RIGHT : CasementWindowSide.LEFT)
                .setValue(WATERLOGGED, context.getLevel().getFluidState(pos).is(Fluids.WATER));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (state.getValue(ANIMATING)) {
            return InteractionResult.CONSUME;
        }

        if (!state.getValue(OPEN)
                && hitResult.getDirection() != state.getValue(FACING)) {
            return InteractionResult.CONSUME;
        }

        boolean changed = CasementWindowPairController.togglePair(
                level, pos, !state.getValue(OPEN));
        return changed
                ? InteractionResult.sidedSuccess(level.isClientSide())
                : InteractionResult.CONSUME;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        return state.getValue(ANIMATING)
                ? ItemInteractionResult.CONSUME
                : super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ANIMATING)) {
            level.setBlock(pos, state.setValue(ANIMATING, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return null;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return CasementWindowGeometry.shape(
                state.getValue(FACING),
                state.getValue(SIDE),
                state.getValue(OPEN)
        );
    }

    @Override
    public BlockState rotate(
            BlockState state,
            Rotation rotation
    ) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(
            BlockState state,
            Mirror mirror
    ) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
                .setValue(SIDE, state.getValue(SIDE).mirror());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.CASEMENT_WINDOW.get().create(pos, state);
    }
}
