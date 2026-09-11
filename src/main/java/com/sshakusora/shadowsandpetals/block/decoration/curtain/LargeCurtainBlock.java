package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;

/**
 * Experimental four-block large curtain: each block renders its own
 * hand-authored quadrant model while static, and right-click toggles OPEN
 * through the resource-driven animation on all four blocks. The block the
 * player placed (the anchor) drives the whole rig through its block-entity
 * renderer during the animation window; the other three blocks go INVISIBLE
 * then, so the moving curtain is drawn exactly once.
 *
 * <p>Structure: {@link DoubleBlockHalf} picks the row and {@link Column}
 * picks the block of that row — {@code OUTER} is the anchor column (in a
 * window pair the two OUTER columns meet at the window center) and
 * {@code INNER} is the column the fabric bunches to when opening.
 * {@link Side} marks which side of the window the whole 2x2 curtain hangs
 * on, mirroring {@link CurtainBlock}: the LEFT curtain bunches to the
 * observer's left and pairs with the RIGHT curtain on its right, and vice
 * versa.</p>
 */
public class LargeCurtainBlock extends CurtainBlock {
    public static final MapCodec<LargeCurtainBlock> CODEC = simpleCodec(LargeCurtainBlock::new);
    public static final EnumProperty<Column> COLUMN = EnumProperty.create("column", Column.class);
    public static final BooleanProperty ANCHOR = BooleanProperty.create("anchor");
    private static final int STRUCTURE_REMOVAL_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final ThreadLocal<Deque<Set<BlockPos>>> ACTIVE_TEARDOWNS =
            ThreadLocal.withInitial(ArrayDeque::new);

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

    /**
     * Gameplay collision slices for FACING=north. The horizontal footprint is
     * kept inside this block's cell even though folded model elements overhang
     * by about one pixel; the wall-normal depth follows the visible folds.
     */
    private static final VoxelShape NORTH_CLOSED_LOWER = toShape(
            LargeCurtainGeometry.closedCollisionBox(false)
    );
    private static final VoxelShape NORTH_CLOSED_UPPER = toShape(
            LargeCurtainGeometry.closedCollisionBox(true)
    );
    private static final Map<Direction, VoxelShape> CLOSED_LOWER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_CLOSED_LOWER);
    private static final Map<Direction, VoxelShape> CLOSED_UPPER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_CLOSED_UPPER);
    /**
     * Open-pose slices for FACING=north, mirroring the open quadrant
     * models: the fabric piles into the inner column (right curtain at
     * local x 1..9, left curtain at x 7..15), the outer column keeps only
     * the rail band in its upper block, and the lower outer block is empty.
     */
    private static final VoxelShape NORTH_OPEN_RAIL = toShape(
            LargeCurtainGeometry.openCollisionBox(true, true, false)
    );
    private static final VoxelShape NORTH_OPEN_PILE_RIGHT_LOWER = toShape(
            LargeCurtainGeometry.openCollisionBox(false, false, false)
    );
    private static final VoxelShape NORTH_OPEN_PILE_RIGHT_UPPER = Shapes.or(
            toShape(LargeCurtainGeometry.openCollisionBox(false, true, false)),
            NORTH_OPEN_RAIL
    ).optimize();
    private static final VoxelShape NORTH_OPEN_PILE_LEFT_LOWER = toShape(
            LargeCurtainGeometry.openCollisionBox(false, false, true)
    );
    private static final VoxelShape NORTH_OPEN_PILE_LEFT_UPPER = Shapes.or(
            toShape(LargeCurtainGeometry.openCollisionBox(false, true, true)),
            NORTH_OPEN_RAIL
    ).optimize();
    private static final Map<Direction, VoxelShape> OPEN_PILE_RIGHT_LOWER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_PILE_RIGHT_LOWER);
    private static final Map<Direction, VoxelShape> OPEN_PILE_RIGHT_UPPER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_PILE_RIGHT_UPPER);
    private static final Map<Direction, VoxelShape> OPEN_PILE_LEFT_LOWER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_PILE_LEFT_LOWER);
    private static final Map<Direction, VoxelShape> OPEN_PILE_LEFT_UPPER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_PILE_LEFT_UPPER);
    private static final Map<Direction, VoxelShape> OPEN_RAIL_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_RAIL);

    private static VoxelShape toShape(LargeCurtainGeometry.CollisionBox box) {
        return Block.box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    public LargeCurtainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(COLUMN, Column.OUTER)
                .setValue(SIDE, Side.RIGHT)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(ANIMATING, false)
                .setValue(ANCHOR, false));
    }

    @Override
    protected MapCodec<LargeCurtainBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, COLUMN, SIDE, OPEN, POWERED, ANIMATING, ANCHOR);
    }

    /**
     * The in-world direction from the anchor (outer) column to the
     * bunching (inner) column: away from the window center, toward this
     * curtain's outer edge. The RIGHT curtain bunches to the observer's
     * right when facing it, so the step points to the observer's right.
     * The LEFT curtain mirrors that.
     */
    static Direction innerStep(BlockState state) {
        return LargeCurtainGeometry.innerStep(
                state.getValue(FACING),
                state.getValue(SIDE) == Side.RIGHT
        );
    }

    /**
     * Returns the four positions belonging to the curtain anchored at {@code anchor}.
     */
    static BlockPos[] structurePositions(BlockPos anchor, BlockState state) {
        return LargeCurtainGeometry.structurePositions(anchor, innerStep(state));
    }

    /**
     * Places the four blocks with the clicked position representing the top
     * outer block whenever the space below is available. If it is not, the
     * structure falls back to the lower outer block at the clicked position,
     * matching the small curtain's upward-extending fallback.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        boolean sneaking = context.getPlayer() != null
                && context.getPlayer().isSecondaryUseActive();
        // The horizontal side is independent of the vertical candidate, but
        // the side must be known before the inner-column direction can be
        // checked for all four cells.
        Side side = CurtainStructure.sideForPlacement(level, clickedPos, facing, sneaking);
        BlockState placementState = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(COLUMN, Column.OUTER)
                .setValue(SIDE, side);
        LargeCurtainGeometry.Placement placement = LargeCurtainGeometry.choosePlacement(
                clickedPos,
                innerStep(placementState),
                part -> level.getBlockState(part).canBeReplaced(context)
        );
        if (placement == null) {
            return null;
        }
        BlockState anchor = placementState
                .setValue(HALF, placement.clickedIsUpper()
                        ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER)
                .setValue(ANCHOR, !placement.clickedIsUpper());
        boolean powered = false;
        for (BlockPos position : LargeCurtainGeometry.structurePositions(
                placement.lowerOuter(), innerStep(anchor))) {
            if (level.hasNeighborSignal(position)) {
                powered = true;
                break;
            }
        }
        return anchor
                .setValue(POWERED, powered)
                .setValue(OPEN, powered);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean upper = state.getValue(HALF) == DoubleBlockHalf.UPPER;
        if (!state.getValue(OPEN)) {
            return (upper ? CLOSED_UPPER_SHAPES : CLOSED_LOWER_SHAPES).get(facing);
        }
        if (state.getValue(COLUMN) == Column.OUTER) {
            return upper
                    ? OPEN_RAIL_SHAPES.get(facing)
                    : Shapes.empty();
        }
        if (state.getValue(SIDE) == Side.LEFT) {
            return (upper ? OPEN_PILE_LEFT_UPPER_SHAPES : OPEN_PILE_LEFT_LOWER_SHAPES).get(facing);
        }
        return (upper ? OPEN_PILE_RIGHT_UPPER_SHAPES : OPEN_PILE_RIGHT_LOWER_SHAPES).get(facing);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.LARGE_CURTAIN.get().create(pos, state);
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        // Do not call super: CurtainBlock.setPlacedBy would place a second
        // vertical half meant for the one-column curtain.
        BlockPos lowerOuterPos = state.getValue(HALF) == DoubleBlockHalf.UPPER
                ? pos.below()
                : pos;
        BlockState anchor = state
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(COLUMN, Column.OUTER)
                .setValue(ANCHOR, true);
        Direction inner = innerStep(anchor);
        BlockPos lowerInnerPos = lowerOuterPos.relative(inner);
        BlockPos upperOuterPos = lowerOuterPos.above();
        BlockPos upperInnerPos = lowerInnerPos.above();

        // When the clicked position was the preferred upper row, create the
        // canonical lower anchor first and then rewrite the clicked cell as
        // the upper outer quadrant. This keeps ANCHOR meaningful regardless
        // of which vertical placement candidate was selected.
        level.setBlock(lowerOuterPos, anchor, Block.UPDATE_ALL);
        level.setBlock(lowerInnerPos,
                anchor.setValue(COLUMN, Column.INNER).setValue(ANCHOR, false),
                Block.UPDATE_ALL);
        level.setBlock(upperOuterPos,
                anchor.setValue(HALF, DoubleBlockHalf.UPPER).setValue(ANCHOR, false),
                Block.UPDATE_ALL);
        level.setBlock(upperInnerPos,
                anchor.setValue(HALF, DoubleBlockHalf.UPPER)
                        .setValue(COLUMN, Column.INNER)
                        .setValue(ANCHOR, false),
                Block.UPDATE_ALL);
    }

    /**
     * Keeps the four blocks anchored to each other: losing a structural
     * neighbour breaks this block, mirroring vanilla door updateShape.
     */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        for (Direction expected : structuralDirections(state)) {
            if (direction != expected) {
                continue;
            }
            if (isTeardownPosition(pos)) {
                // CurtainBlock's vertical-pair check would otherwise turn
                // this part into AIR while the hit block is being removed.
                // Keep every part alive until the explicit teardown below
                // removes it with UPDATE_SUPPRESS_DROPS.
                return state;
            }
            if (!isSameCurtain(level.getBlockState(neighborPos), state)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * The two directions from any block of the curtain to its partners.
     */
    private static Direction[] structuralDirections(BlockState state) {
        Direction vertical = state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? Direction.UP
                : Direction.DOWN;
        Direction columnStep = state.getValue(COLUMN) == Column.OUTER
                ? innerStep(state)
                : innerStep(state).getOpposite();
        return new Direction[]{vertical, columnStep};
    }

    private static boolean isSameCurtain(BlockState neighbour, BlockState state) {
        return neighbour.getBlock() == state.getBlock()
                && neighbour.getValue(FACING) == state.getValue(FACING)
                && neighbour.getValue(SIDE) == state.getValue(SIDE)
                && ((neighbour.getValue(HALF) != state.getValue(HALF)
                && neighbour.getValue(COLUMN) == state.getValue(COLUMN))
                || (neighbour.getValue(HALF) == state.getValue(HALF)
                && neighbour.getValue(COLUMN) != state.getValue(COLUMN)));
    }

    /**
     * The lower outer corner (anchor) of the curtain that owns this block.
     */
    static BlockPos anchorOf(BlockPos pos, BlockState state) {
        return LargeCurtainGeometry.anchorOf(
                pos,
                innerStep(state),
                state.getValue(HALF) == DoubleBlockHalf.UPPER,
                state.getValue(COLUMN) == Column.INNER
        );
    }

    protected boolean handlesVanillaPairedBreak() {
        return false;
    }

    /**
     * Removes the complete 2x2 structure as one player-destruction
     * transaction. The guard keeps the four parts alive while the normal
     * removal of the hit block sends its neighbor updates, then the remaining
     * parts are removed with normal notifications but suppressed drops.
     */
    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid
    ) {
        if (level.isClientSide()) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }

        BlockPos anchor = anchorOf(pos, state);
        Set<BlockPos> structure = Set.of(structurePositions(anchor, state));
        beginTeardown(structure);
        try {
            boolean removed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
            if (!removed) {
                return false;
            }

            // The hit block is removed through the normal game-mode path above.
            // A non-anchor hit therefore needs exactly one explicit anchor
            // drop, with the real player/tool context, before the anchor is
            // removed as a non-dropping companion.
            if (!pos.equals(anchor) && willHarvest) {
                dropAnchorForBreak(level, anchor, state, player, player.getMainHandItem());
            }
            removeStructure(level, structure, anchor, pos, state);
            return true;
        } finally {
            endTeardown();
        }
    }

    /**
     * Drops the one item represented by the lower/outer anchor when a non-anchor
     * part is the block being broken. The normal game-mode path still handles
     * the break statistic, tool damage, and anchor drops when the anchor itself
     * is clicked.
     */
    private static void dropAnchorForBreak(
            Level level,
            BlockPos anchor,
            BlockState brokenState,
            Player player,
            ItemStack toolStack
    ) {
        BlockState anchorState = level.getBlockState(anchor);
        if (!isAnchorState(anchorState, brokenState)
                || player.isCreative()) {
            return;
        }
        Block.dropResources(
                anchorState,
                level,
                anchor,
                level.getBlockEntity(anchor),
                player,
                toolStack.copy()
        );
    }

    private static boolean isAnchorState(BlockState candidate, BlockState reference) {
        return candidate.getBlock() == reference.getBlock()
                && candidate.getValue(FACING) == reference.getValue(FACING)
                && candidate.getValue(SIDE) == reference.getValue(SIDE)
                && candidate.getValue(HALF) == DoubleBlockHalf.LOWER
                && candidate.getValue(COLUMN) == Column.OUTER
                && candidate.getValue(ANCHOR);
    }

    /**
     * Removes the parts other than the block already handled by the game mode.
     */
    private static void removeStructure(
            Level level, Set<BlockPos> structure, BlockPos anchor, BlockPos hit, BlockState state
    ) {
        for (BlockPos part : structure) {
            if (part.equals(hit)) {
                continue;
            }
            BlockState partState = level.getBlockState(part);
            if (isPartOfStructure(part, partState, anchor, state)) {
                // UPDATE_ALL keeps surrounding blocks consistent. The
                // teardown guard prevents these notifications from recursively
                // destroying another curtain part; UPDATE_SUPPRESS_DROPS is a
                // final safety net for any future shape/update path.
                level.setBlock(part, Blocks.AIR.defaultBlockState(), STRUCTURE_REMOVAL_FLAGS);
            }
        }
    }

    private static boolean isPartOfStructure(
            BlockPos pos, BlockState candidate, BlockPos anchor, BlockState reference
    ) {
        return candidate.getBlock() == reference.getBlock()
                && candidate.getValue(FACING) == reference.getValue(FACING)
                && candidate.getValue(SIDE) == reference.getValue(SIDE)
                && anchorOf(pos, candidate).equals(anchor);
    }

    private static void beginTeardown(Set<BlockPos> structure) {
        ACTIVE_TEARDOWNS.get().push(structure);
    }

    private static void endTeardown() {
        Deque<Set<BlockPos>> stack = ACTIVE_TEARDOWNS.get();
        stack.pop();
        if (stack.isEmpty()) {
            ACTIVE_TEARDOWNS.remove();
        }
    }

    private static boolean isTeardownPosition(BlockPos pos) {
        for (Set<BlockPos> structure : ACTIVE_TEARDOWNS.get()) {
            if (structure.contains(pos)) {
                return true;
            }
        }
        return false;
    }

}