package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.CurtainBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Experimental four-block curtain (two wide, two tall): right-click toggles
 * the OPEN state and plays the resource-driven open/close animation across
 * all four blocks of this curtain and its linked partner curtain.
 *
 * <p>Structure: {@link DoubleBlockHalf} picks the row and {@link Column}
 * picks the block of that row — {@code OUTER} is the column the closed
 * fabric bunches to when opening (this curtain's own window side), and
 * {@code INNER} faces the partner curtain. Placement anchors the lower
 * outer block and extends upward and toward the inner side; breaking any
 * block removes the whole curtain and drops one item.</p>
 */
public class LargeCurtainBlock extends BaseEntityBlock {
    public static final MapCodec<LargeCurtainBlock> CODEC = simpleCodec(LargeCurtainBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Column> COLUMN = EnumProperty.create("column", Column.class);
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

    /** Which column of the two-wide curtain this block is. */
    public enum Column implements StringRepresentable {
        OUTER("outer"),
        INNER("inner");

        private final String name;

        Column(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public Column mirror() {
            return this == OUTER ? INNER : OUTER;
        }
    }

    /** Which side of a window the curtain hangs on. */
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

    /**
     * Collision slices for FACING=north, RIGHT curtain (outer column at
     * west). Closed curtains cover the full two-block width; open curtains
     * only cover the bunched fabric in the outer column.
     */
    private static final VoxelShape NORTH_CLOSED = box(0, 0, 14, 16, 16, 15);
    private static final VoxelShape NORTH_OPEN_OUTER = box(0, 0, 14, 4, 16, 15);
    private static final VoxelShape NORTH_OPEN_INNER = box(4, 0, 14, 8, 16, 15);
    private static final Map<Direction, VoxelShape> CLOSED_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_CLOSED);
    private static final Map<Direction, VoxelShape> OPEN_OUTER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_OUTER);
    private static final Map<Direction, VoxelShape> OPEN_INNER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_INNER);

    public LargeCurtainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(COLUMN, Column.OUTER)
                .setValue(SIDE, Side.RIGHT)
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
        builder.add(FACING, HALF, COLUMN, SIDE, OPEN, POWERED, ANIMATING);
    }

    /**
     * The in-world direction from the outer to the inner column: toward the
     * partner curtain. For a RIGHT curtain (FACING=north) the inner column
     * lies to the observer's left of the outer one.
     */
    private static Direction innerStep(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(SIDE) == Side.RIGHT
                ? facing.getClockWise().getOpposite()
                : facing.getClockWise();
    }

    /**
     * Places the four blocks anchored at the clicked position: the click
     * lands on the lower outer block and the structure extends upward and
     * toward the inner side. If the inner or upper cells are blocked the
     * placement fails, like a multi-block door.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        Side side = sideForNeighbour(level, clickedPos, facing,
                context.getPlayer() != null && context.getPlayer().isSecondaryUseActive());
        BlockState anchor = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(COLUMN, Column.OUTER)
                .setValue(SIDE, side);
        BlockPos inner = clickedPos.relative(innerStep(anchor));
        if (!level.getBlockState(inner).canBeReplaced(context)
                || !level.getBlockState(clickedPos.above()).canBeReplaced(context)
                || !level.getBlockState(inner.above()).canBeReplaced(context)) {
            return null;
        }
        boolean powered = hasAnyRedstoneSignal(level, clickedPos, inner);
        return anchor
                .setValue(POWERED, powered)
                .setValue(OPEN, powered);
    }

    /**
     * Chooses the side from the neighbouring large curtain of the same
     * facing: the neighbour on the observer's left marks this curtain's
     * window position as the observer's right, so the curtain is RIGHT, and
     * vice versa. Sneaking keeps the neighbour's side (same-side pairing).
     * Without a neighbouring curtain this defaults to RIGHT, whose outer
     * column is on the observer's left — matching the authored model.
     */
    private static Side sideForNeighbour(Level level, BlockPos lowerOuterPos, Direction facing, boolean sneaking) {
        Direction leftDir = facing.getClockWise();
        Direction[] both = {leftDir, leftDir.getOpposite()};
        for (Direction direction : both) {
            BlockPos neighbourPos = lowerOuterPos.relative(direction);
            BlockState neighbour = level.getBlockState(neighbourPos);
            if (neighbour.getBlock() instanceof LargeCurtainBlock
                    && neighbour.getValue(FACING) == facing) {
                if (sneaking) {
                    return neighbour.getValue(SIDE);
                }
                return direction == leftDir ? Side.RIGHT : Side.LEFT;
            }
        }
        return Side.RIGHT;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (!state.getValue(OPEN)) {
            return CLOSED_SHAPES.get(facing);
        }
        // The bunch always sits in the outer column; in the inner column a
        // thin edge still marks where the fabric gathers.
        return (state.getValue(COLUMN) == Column.OUTER
                ? OPEN_OUTER_SHAPES
                : OPEN_INNER_SHAPES).get(facing);
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
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.@Nullable LivingEntity placer,
            net.minecraft.world.item.ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!state.getValue(HALF).equals(DoubleBlockHalf.LOWER)
                || state.getValue(COLUMN) != Column.OUTER) {
            return;
        }
        BlockPos innerPos = pos.relative(innerStep(state));
        level.setBlock(innerPos, state.setValue(COLUMN, Column.INNER), Block.UPDATE_ALL);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        level.setBlock(innerPos.above(),
                state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(COLUMN, Column.INNER),
                Block.UPDATE_ALL);
    }

    private static boolean isSameCurtain(BlockState state, BlockState neighbour) {
        if (!(neighbour.getBlock() instanceof LargeCurtainBlock)) {
            return false;
        }
        boolean differentHalf = neighbour.getValue(HALF) != state.getValue(HALF);
        boolean differentColumn = neighbour.getValue(COLUMN) != state.getValue(COLUMN);
        return neighbour.getValue(FACING) == state.getValue(FACING)
                && neighbour.getValue(SIDE) == state.getValue(SIDE)
                && (differentHalf || differentColumn);
    }

    @Override
    protected BlockState updateShape(
            BlockState state, net.minecraft.world.level.LevelReader level,
            net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState,
            net.minecraft.util.RandomSource random
    ) {
        for (Direction expected : structuralDirections(state)) {
            if (direction != expected) {
                continue;
            }
            BlockState neighbour = level.getBlockState(neighborPos);
            if (!isSameCurtain(state, neighbour)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** The three directions from any block of the curtain to its partners. */
    private static Direction[] structuralDirections(BlockState state) {
        Direction vertical = state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? Direction.UP
                : Direction.DOWN;
        Direction columnStep = state.getValue(COLUMN) == Column.OUTER
                ? innerStep(state)
                : innerStep(state).getOpposite();
        return new Direction[]{vertical, columnStep};
    }


    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            // Only the lower outer block drops an item.
            removeStructure(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Removes the remaining blocks of this curtain without extra drops. */
    private static void removeStructure(Level level, BlockPos pos, BlockState state) {
        BlockPos anchor = anchorOf(pos, state);
        Direction inner = innerStep(state);
        for (BlockPos part : new BlockPos[]{
                anchor, anchor.relative(inner), anchor.above(), anchor.above().relative(inner)
        }) {
            BlockState partState = level.getBlockState(part);
            if (partState.getBlock() instanceof LargeCurtainBlock
                    && partState.getValue(SIDE) == state.getValue(SIDE)
                    && partState.getValue(FACING) == state.getValue(FACING)) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** The lower outer corner of the curtain that owns this block. */
    private static BlockPos anchorOf(BlockPos pos, BlockState state) {
        BlockPos anchor = pos;
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            anchor = anchor.below();
        }
        if (state.getValue(COLUMN) == Column.INNER) {
            anchor = anchor.relative(innerStep(state).getOpposite());
        }
        return anchor;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        if (isPoweredPair(level, pos, state)) {
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
        BlockPos anchor = anchorOf(pos, state);
        Direction inner = innerStep(state);
        boolean powered = hasAnyRedstoneSignal(level,
                anchor, anchor.relative(inner));
        boolean upperPowered = hasAnyRedstoneSignal(level,
                anchor.above(), anchor.above().relative(inner));
        powered = powered || upperPowered;
        if (powered != state.getValue(POWERED)) {
            if (powered != state.getValue(OPEN)) {
                togglePair(level, pos, state, powered);
            } else {
                setCurtainFlag(level, pos, state, LargeCurtainBlock.POWERED, powered);
            }
        }
    }

    /** Sets a flag on every block of this curtain. */
    private static void setCurtainFlag(
            Level level, BlockPos pos, BlockState state, BooleanProperty flag, boolean value
    ) {
        BlockPos anchor = anchorOf(pos, state);
        Direction inner = innerStep(state);
        for (BlockPos part : new BlockPos[]{
                anchor, anchor.relative(inner), anchor.above(), anchor.above().relative(inner)
        }) {
            BlockState partState = level.getBlockState(part);
            if (partState.getBlock() instanceof LargeCurtainBlock
                    && partState.getValue(SIDE) == state.getValue(SIDE)
                    && partState.getValue(FACING) == state.getValue(FACING)) {
                level.setBlock(part, partState.setValue(flag, value), Block.UPDATE_ALL);
            }
        }
    }

    /** True if any of the four blocks of this curtain sees redstone. */
    private static boolean hasAnyRedstoneSignal(Level level, BlockPos lowerOuter, BlockPos lowerInner) {
        for (BlockPos pos : new BlockPos[]{
                lowerOuter, lowerInner, lowerOuter.above(), lowerInner.above()
        }) {
            if (level.hasNeighborSignal(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggles this curtain's four blocks plus its linked partner curtain,
     * recording the shared animation clock on each block entity.
     *
     * <p>Linking is geometric: in a window pair the LEFT curtain stands on
     * the observer's left, so its RIGHT partner is toward the observer's
     * right, and vice versa. Two same-side curtains never link.</p>
     */
    private static void togglePair(Level level, BlockPos pos, BlockState state, boolean open) {
        long gameTime = level.getGameTime();
        BlockPos neighbourPos = linkedNeighbourPos(pos, state);
        BlockState neighbour = level.getBlockState(neighbourPos);
        boolean hasLinkedNeighbour = isLinkedNeighbour(state, neighbour);
        BlockPos anchor = anchorOf(pos, state);
        boolean powered = hasAnyRedstoneSignal(level, anchor, anchor.relative(innerStep(state)));

        BlockPos neighbourAnchor = hasLinkedNeighbour
                ? anchorOf(neighbourPos, neighbour)
                : null;
        boolean neighbourPowered = hasLinkedNeighbour && neighbourAnchor != null
                && hasAnyRedstoneSignal(level, neighbourAnchor, neighbourAnchor.relative(innerStep(neighbour)));
        boolean targetOpen = open || powered || neighbourPowered;

        toggleCurtain(level, anchor, state, targetOpen, gameTime, powered);
        if (hasLinkedNeighbour) {
            toggleCurtain(level, neighbourAnchor, neighbour, targetOpen, gameTime, neighbourPowered);
        }
    }

    /** Toggles all four blocks of the curtain anchored at {@code anchor}. */
    private static void toggleCurtain(
            Level level, BlockPos anchor, BlockState state, boolean open, long gameTime, boolean powered
    ) {
        Direction inner = innerStep(state);
        for (BlockPos pos : new BlockPos[]{
                anchor, anchor.relative(inner), anchor.above(), anchor.above().relative(inner)
        }) {
            BlockState partState = level.getBlockState(pos);
            if (!(partState.getBlock() instanceof LargeCurtainBlock)) {
                continue;
            }
            // Record the clock before setBlock so the block-entity data
            // packet carries OPEN and the animation timestamp together.
            recordClock(level, pos, gameTime, open);
            level.setBlock(pos, partState.setValue(OPEN, open).setValue(POWERED, powered)
                    .setValue(ANIMATING, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, partState.getBlock(), ANIMATION_TICKS);
        }
    }

    private static boolean isPoweredPair(Level level, BlockPos pos, BlockState state) {
        BlockPos anchor = anchorOf(pos, state);
        if (state.getValue(POWERED)
                || hasAnyRedstoneSignal(level, anchor, anchor.relative(innerStep(state)))) {
            return true;
        }

        BlockPos neighbourPos = linkedNeighbourPos(pos, state);
        BlockState neighbour = level.getBlockState(neighbourPos);
        if (!isLinkedNeighbour(state, neighbour)) {
            return false;
        }
        BlockPos neighbourAnchor = anchorOf(neighbourPos, neighbour);
        return neighbour.getValue(POWERED)
                || hasAnyRedstoneSignal(level, neighbourAnchor,
                        neighbourAnchor.relative(innerStep(neighbour)));
    }

    /** The partner curtain's inner-adjacent block position, if linked. */
    private static BlockPos linkedNeighbourPos(BlockPos pos, BlockState state) {
        BlockPos anchor = anchorOf(pos, state);
        Direction towardPartner = state.getValue(SIDE) == Side.LEFT
                ? innerStep(state)          // LEFT looks right for its RIGHT partner
                : innerStep(state).getOpposite();  // RIGHT looks left for its LEFT partner
        return anchor.relative(towardPartner);
    }

    private static boolean isLinkedNeighbour(BlockState state, BlockState neighbour) {
        return neighbour.getBlock() instanceof LargeCurtainBlock
                && neighbour.getValue(FACING) == state.getValue(FACING)
                && neighbour.getValue(SIDE) != state.getValue(SIDE);
    }

    private static void recordClock(Level level, BlockPos pos, long gameTime, boolean open) {
        if (level.getBlockEntity(pos) instanceof CurtainBlockEntity curtain) {
            curtain.recordTransition(gameTime, open);
            curtain.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
    }
}