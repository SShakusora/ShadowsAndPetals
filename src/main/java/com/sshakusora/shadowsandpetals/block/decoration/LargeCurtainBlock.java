package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.LargeCurtainBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Experimental single-block large curtain: one block carries the whole
 * eight-panel model (the geometry overhangs the block cell, like a fence)
 * and right-click toggles OPEN through the resource-driven animation.
 *
 * <p>Probe build for the model/animation pipeline: no multi-block
 * structure, no side pairing — just placement, animation and the static
 * baked poses.</p>
 */
public class LargeCurtainBlock extends BaseEntityBlock {
    public static final MapCodec<LargeCurtainBlock> CODEC = simpleCodec(LargeCurtainBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /**
     * True only during the open/close animation window: the block-entity
     * renderer owns the pose then. Once false, the plain block-state model
     * (baked to the current OPEN pose) renders the curtain for free.
     */
    public static final BooleanProperty ANIMATING = BooleanProperty.create("animating");
    /** Server ticks to hold ANIMATING: ceil of the 0.29167 s clip length. */
    public static final int ANIMATION_TICKS = 6;

    /**
     * Collision slice for FACING=north: a thin strip along the wall face.
     * The closed curtain visually spans the neighbouring cells, but the
     * block's own collision stays inside its cell (like a fence's post).
     */
    private static final VoxelShape NORTH_SHAPE = box(0, 0, 14, 16, 16, 15);
    private static final Map<Direction, VoxelShape> SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_SHAPE);

    public LargeCurtainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(ANIMATING, false));
    }

    @Override
    protected MapCodec<LargeCurtainBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, ANIMATING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, powered)
                .setValue(OPEN, powered);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Static chunk-mesh rendering outside the animation window; the
        // block-entity renderer takes over only while ANIMATING.
        return state.getValue(ANIMATING) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.LARGE_CURTAIN.get().create(pos, state);
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level,
                        BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(ANIMATING)) {
            level.setBlock(pos, state.setValue(ANIMATING, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        if (state.getValue(POWERED)) {
            return InteractionResult.PASS;
        }
        boolean open = !state.getValue(OPEN);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        toggle(level, pos, state, open);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block block,
            net.minecraft.world.level.redstone.@Nullable Orientation orientation,
            boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            if (powered != state.getValue(OPEN)) {
                toggle(level, pos, state, powered);
            } else {
                level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
            }
        }
    }

    /** Toggles this curtain, recording the animation clock on its block entity. */
    private static void toggle(Level level, BlockPos pos, BlockState state, boolean open) {
        long gameTime = level.getGameTime();
        // Record the clock before setBlock so the block-entity data packet
        // carries OPEN and the animation timestamp together.
        if (level.getBlockEntity(pos) instanceof LargeCurtainBlockEntity curtain) {
            curtain.recordTransition(gameTime, open);
            curtain.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
        level.setBlock(pos, state.setValue(OPEN, open).setValue(POWERED, open)
                        .setValue(ANIMATING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), ANIMATION_TICKS);
    }
}