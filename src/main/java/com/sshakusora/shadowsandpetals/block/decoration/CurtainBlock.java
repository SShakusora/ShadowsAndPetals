package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.CurtainBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.StringRepresentable;
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

    /** Which side of a window the curtain panel hangs on. */
    public enum Side implements StringRepresentable {
        LEFT("left"),
        RIGHT("right");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public Side mirror() {
            return this == LEFT ? RIGHT : LEFT;
        }
    }

    public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);
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
                .setValue(SIDE, Side.RIGHT)
                .setValue(OPEN, false)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<CurtainBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SIDE, OPEN, POWERED);
    }

    /**
     * Places the pair with the upper half at the clicked position, extending
     * downward past the clicked spot when the block below can be replaced.
     * When the spot below cannot be replaced, the clicked position becomes
     * the lower half and the pair extends upward instead.
     *
     * <p>The side follows the neighbouring curtain of the same facing: by
     * default the new curtain takes the opposite side so a window pair links
     * open/close, while sneaking takes the same side for placing two
     * curtains side by side on one wall.</p>
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        boolean belowReplaceable = level.getBlockState(clickedPos.below()).canBeReplaced(context);
        DoubleBlockHalf halfAtClick = belowReplaceable
                ? DoubleBlockHalf.UPPER
                : DoubleBlockHalf.LOWER;
        BlockPos lowerPos = belowReplaceable ? clickedPos.below() : clickedPos;
        BlockPos upperPos = belowReplaceable ? clickedPos : clickedPos.above();
        if (!level.getBlockState(lowerPos).canBeReplaced(context)
                || !level.getBlockState(upperPos).canBeReplaced(context)) {
            return null;
        }
        boolean powered = level.hasNeighborSignal(lowerPos) || level.hasNeighborSignal(upperPos);
        Direction facing = context.getHorizontalDirection().getOpposite();
        Side side = sideForNeighbour(level, lowerPos, facing, context.getPlayer() != null && context.getPlayer().isSecondaryUseActive());
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, halfAtClick)
                .setValue(SIDE, side)
                .setValue(POWERED, powered)
                .setValue(OPEN, powered);
    }

    /**
     * Chooses the side from the neighbouring curtain of the same facing,
     * using wall geometry: the neighbour on the observer's left makes this
     * curtain RIGHT, the neighbour on the observer's right makes it LEFT.
     * Sneaking keeps the neighbour's side instead (same-side pairing).
     * Without a linkable neighbour the curtain is a plain RIGHT.
     */
    private static Side sideForNeighbour(Level level, BlockPos lowerPos, Direction facing, boolean sneaking) {
        Direction leftDir = facing.getClockWise();
        Direction[] both = {leftDir, leftDir.getOpposite()};
        for (Direction direction : both) {
            BlockPos neighbourPos = lowerPos.relative(direction);
            BlockState neighbour = level.getBlockState(neighbourPos);
            if (neighbour.getBlock() instanceof CurtainBlock
                    && neighbour.getValue(FACING) == facing) {
                if (sneaking) {
                    return neighbour.getValue(SIDE);
                }
                // The new curtain sits on the opposite side of the window
                // from the neighbour: neighbour on the left => RIGHT here.
                return direction == leftDir ? Side.RIGHT : Side.LEFT;
            }
        }
        return Side.RIGHT;
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
        // Remove the other half in the same interaction so the pair never
        // lingers. The loot table only drops for the lower half, so exactly
        // one item falls per curtain in every mode.
        if (!level.isClientSide()) {
            BlockPos otherPos = pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER
                    ? Direction.UP : Direction.DOWN);
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.getBlock() instanceof CurtainBlock) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
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

    /**
     * Toggles this curtain's halves plus its linked neighbour curtain,
     * recording the shared animation clock on each block entity.
     *
     * <p>Linking is geometric: a LEFT curtain only links with a RIGHT
     * curtain on its left side (as seen from the room), and a RIGHT curtain
     * only links with a LEFT curtain on its right side. Two same-side
     * curtains never link.</p>
     */
    private static void togglePair(Level level, BlockPos pos, BlockState state, boolean open) {
        long gameTime = level.getGameTime();
        toggleColumn(level, pos, state, open, gameTime);

        Direction facing = state.getValue(FACING);
        Side side = state.getValue(SIDE);
        Direction outward = side == Side.LEFT
                ? facing.getClockWise()          // LEFT curtains link toward the observer's left
                : facing.getClockWise().getOpposite(); // RIGHT curtains link toward the observer's right
        BlockPos neighbourPos = pos.relative(outward);
        BlockState neighbour = level.getBlockState(neighbourPos);
        if (neighbour.getBlock() instanceof CurtainBlock
                && neighbour.getValue(FACING) == facing
                && neighbour.getValue(SIDE) != side) {
            toggleColumn(level, neighbourPos, neighbour, open, gameTime);
        }
    }

    /** Toggles both vertical halves of one curtain column. */
    private static void toggleColumn(Level level, BlockPos pos, BlockState state, boolean open, long gameTime) {
        DoubleBlockHalf half = state.getValue(HALF);
        BlockPos otherPos = pos.relative(half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        BlockState otherState = level.getBlockState(otherPos);
        boolean hasPair = otherState.getBlock() instanceof CurtainBlock
                && otherState.getValue(SIDE) == state.getValue(SIDE);
        boolean powered = state.hasProperty(POWERED) && state.getValue(POWERED);

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