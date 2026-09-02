package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.CurtainBlockEntity;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Experimental two-block curtain: right-click toggles the OPEN state and
 * plays the resource-driven open/close animation on both halves.
 *
 * <p>Like a vanilla door the block stores a {@link DoubleBlockHalf} so the
 * two halves stay paired: breaking one half drops only the lower item, and
 * updating one half re-anchors the other. Unlike a door, placement prefers
 * extending <em>downward</em>: when the clicked spot has a replaceable block
 * below it, the lower half is placed below and the upper half takes the
 * clicked position, matching how curtains hang.</p>
 */
public class CurtainBlock extends BaseEntityBlock {
    public static final MapCodec<CurtainBlock> CODEC = simpleCodec(CurtainBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(0, 0, 12.5, 16, 16, 14.5),
            // The unfolded curtain panels sweep across the full X width.
            box(0, 0, 12, 16, 15.2, 14.5)
    ).optimize();
    private static final Map<Direction, VoxelShape> SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_SHAPE);

    public CurtainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, false)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<CurtainBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN, POWERED);
    }

    /**
     * Places the pair anchored at the clicked position: the upper half takes
     * the clicked spot and the lower half extends below it when that block
     * can be replaced. When the spot below cannot be replaced, the clicked
     * position becomes the lower half and the pair extends upward instead.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        BlockPos lowerPos = level.getBlockState(clickedPos.below()).canBeReplaced(context)
                ? clickedPos.below()
                : clickedPos;
        BlockPos upperPos = lowerPos.above();
        if (lowerPos.getY() < level.getMinY()
                || !level.getBlockState(lowerPos).canBeReplaced(context)
                || !level.getBlockState(upperPos).canBeReplaced(context)) {
            return null;
        }
        boolean powered = level.hasNeighborSignal(lowerPos) || level.hasNeighborSignal(upperPos);
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(POWERED, powered)
                .setValue(OPEN, powered);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.CURTAIN.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return null;
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.@Nullable LivingEntity placer,
            net.minecraft.world.item.ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockPos otherPos = pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? Direction.UP : Direction.DOWN);
        level.setBlock(otherPos, state.setValue(HALF, otherHalf(state)), Block.UPDATE_ALL);
    }

    /**
     * Keeps the halves anchored to each other, mirroring vanilla door
     * updateShape: losing the counterpart half breaks this half.
     */
    @Override
    protected BlockState updateShape(
            BlockState state, net.minecraft.world.level.LevelReader level,
            net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState,
            net.minecraft.util.RandomSource random
    ) {
        DoubleBlockHalf half = state.getValue(HALF);
        Direction expected = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (direction == expected
                && !(neighborState.getBlock() instanceof CurtainBlock
                && neighborState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Mirror the vanilla door/double-plant pairing so exactly one item
        // drops per curtain: creative silently removes the other half; in
        // survival the lower half drops once and the upper half is cleared
        // without loot (playerDestroy suppresses the default drop path).
        if (!level.isClientSide()) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos otherPos = pos.relative(half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
            BlockState otherState = level.getBlockState(otherPos);
            boolean hasPair = otherState.getBlock() instanceof CurtainBlock;
            if (player.preventsBlockDrops()) {
                if (hasPair) {
                    level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            } else {
                if (hasPair) {
                    level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                if (half == DoubleBlockHalf.UPPER && hasPair) {
                    // Breaking the upper half by hand: drop once for the pair
                    // from this position, since playerDestroy is suppressed.
                    dropResources(state, level, pos, null, player, player.getMainHandItem());
                } else if (half == DoubleBlockHalf.LOWER) {
                    dropResources(state, level, pos, null, player, player.getMainHandItem());
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(
            Level level, Player player, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.entity.@Nullable BlockEntity blockEntity,
            net.minecraft.world.item.ItemStack destroyedWith
    ) {
        // Suppress the default drop: playerWillDestroy already dropped for
        // the pair exactly once.
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        // Redstone-driven curtains stay closed to manual use, like vanilla doors.
        if (state.getValue(POWERED)) {
            return InteractionResult.PASS;
        }
        boolean open = !state.getValue(OPEN);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        togglePair(level, pos, state, open);
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
        boolean powered = level.hasNeighborSignal(pos)
                || level.hasNeighborSignal(pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? Direction.UP : Direction.DOWN));
        if (powered != state.getValue(POWERED)) {
            if (powered != state.getValue(OPEN)) {
                togglePair(level, pos, state, powered);
            } else {
                // Only the POWERED flag changes; keep the current pose.
                level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
                BlockPos otherPos = pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER
                        ? Direction.UP : Direction.DOWN);
                BlockState otherState = level.getBlockState(otherPos);
                if (otherState.getBlock() instanceof CurtainBlock) {
                    level.setBlock(otherPos, otherState.setValue(POWERED, powered), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** Toggles both halves and records the shared animation clock on each. */
    private static void togglePair(Level level, BlockPos pos, BlockState state, boolean open) {
        DoubleBlockHalf half = state.getValue(HALF);
        BlockPos otherPos = pos.relative(half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        BlockState otherState = level.getBlockState(otherPos);
        boolean hasPair = otherState.getBlock() instanceof CurtainBlock;
        boolean powered = state.hasProperty(POWERED) && state.getValue(POWERED);

        long gameTime = level.getGameTime();
        // Record the clock before setBlock so the block-entity data packet
        // carries OPEN and the animation timestamp together.
        recordClock(level, pos, gameTime, open);
        level.setBlock(pos, state.setValue(OPEN, open).setValue(POWERED, powered), Block.UPDATE_ALL);
        if (hasPair) {
            recordClock(level, otherPos, gameTime, open);
            level.setBlock(otherPos, otherState.setValue(OPEN, open).setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    private static void recordClock(Level level, BlockPos pos, long gameTime, boolean open) {
        if (level.getBlockEntity(pos) instanceof CurtainBlockEntity curtain) {
            curtain.recordTransition(gameTime, open);
            curtain.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
    }

    private static DoubleBlockHalf otherHalf(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? DoubleBlockHalf.UPPER
                : DoubleBlockHalf.LOWER;
    }
}