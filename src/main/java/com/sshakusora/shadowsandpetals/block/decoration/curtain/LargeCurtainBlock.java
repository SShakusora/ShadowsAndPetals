package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.LargeCurtainBlockEntity;
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
import net.minecraft.world.level.ScheduledTickAccess;
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
import org.jspecify.annotations.Nullable;

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

    /** Returns the four positions belonging to the curtain anchored at {@code anchor}. */
    static BlockPos[] structurePositions(BlockPos anchor, BlockState state) {
        return LargeCurtainGeometry.structurePositions(anchor, innerStep(state));
    }

    /**
     * Places the four blocks anchored at the clicked position: the click
     * lands on the lower outer block and the structure extends upward and
     * toward the inner column.
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
                .setValue(SIDE, side)
                .setValue(ANCHOR, true);
        if (!LargeCurtainGeometry.allReplaceable(
                clickedPos,
                innerStep(anchor),
                part -> level.getBlockState(part).canBeReplaced(context))) {
            return null;
        }
        boolean powered = level.hasNeighborSignal(clickedPos);
        return anchor
                .setValue(POWERED, powered)
                .setValue(OPEN, powered);
    }

    /**
     * Chooses the side from the neighbouring large curtain of the same
     * facing, mirroring {@link CurtainBlock}'s wall geometry: the neighbour
     * on the observer's left marks this curtain's window position as the
     * observer's right, so the curtain is RIGHT, and vice versa. Sneaking
     * keeps the neighbour's side instead (same-side pairing). Without a
     * neighbouring curtain the curtain defaults to LEFT.
     *
     * <p>Because each curtain is two blocks wide, the partner of a window
     * pair sits two cells away from this anchor (its inner column borders
     * this curtain's inner column), while a directly adjacent curtain is
     * one cell away; both distances are probed.</p>
     */
    private static Side sideForNeighbour(Level level, BlockPos lowerPos, Direction facing, boolean sneaking) {
        Direction leftDir = facing.getClockWise();
        Direction[] both = {leftDir, leftDir.getOpposite()};
        for (Direction direction : both) {
            for (int distance = 1; distance <= 2; distance++) {
                BlockState neighbour = level.getBlockState(lowerPos.relative(direction, distance));
                if (neighbour.getBlock() instanceof LargeCurtainBlock
                        && neighbour.getValue(FACING) == facing) {
                    if (sneaking) {
                        return neighbour.getValue(SIDE);
                    }
                    // This curtain sits on the opposite window side from the
                    // neighbour: neighbour at observer-left => this is RIGHT.
                    return LargeCurtainGeometry.sideFromNeighbour(
                            neighbour.getValue(SIDE) == Side.RIGHT,
                            direction == leftDir,
                            false
                    ) ? Side.RIGHT : Side.LEFT;
                }
            }
        }
        return Side.LEFT;
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
        if (!state.getValue(ANCHOR)) {
            return;
        }
        BlockPos innerPos = pos.relative(innerStep(state));
        level.setBlock(innerPos, state.setValue(COLUMN, Column.INNER).setValue(ANCHOR, false), Block.UPDATE_ALL);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(ANCHOR, false), Block.UPDATE_ALL);
        level.setBlock(innerPos.above(),
                state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(COLUMN, Column.INNER).setValue(ANCHOR, false),
                Block.UPDATE_ALL);
    }

    /**
     * Keeps the four blocks anchored to each other: losing a structural
     * neighbour breaks this block, mirroring vanilla door updateShape.
     */
    @Override
    protected BlockState updateShape(
            BlockState state, LevelReader level,
            ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState,
            RandomSource random
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
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** The two directions from any block of the curtain to its partners. */
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

    /** The lower outer corner (anchor) of the curtain that owns this block. */
    static BlockPos anchorOf(BlockPos pos, BlockState state) {
        return LargeCurtainGeometry.anchorOf(
                pos,
                innerStep(state),
                state.getValue(HALF) == DoubleBlockHalf.UPPER,
                state.getValue(COLUMN) == Column.INNER
        );
    }

    @Override
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
            ItemStack toolStack,
            boolean willHarvest,
            FluidState fluid
    ) {
        if (level.isClientSide()) {
            return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
        }

        BlockPos anchor = anchorOf(pos, state);
        Set<BlockPos> structure = Set.of(structurePositions(anchor, state));
        beginTeardown(structure);
        try {
            boolean removed = super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
            if (!removed) {
                return false;
            }

            // The hit block is removed through the normal game-mode path above.
            // A non-anchor hit therefore needs exactly one explicit anchor
            // drop, with the real player/tool context, before the anchor is
            // removed as a non-dropping companion.
            if (!pos.equals(anchor) && willHarvest) {
                dropAnchorForBreak(level, anchor, state, player, toolStack);
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
                || player.preventsBlockDrops()) {
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

    /** Removes the parts other than the block already handled by the game mode. */
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

    /**
     * The anchor of the partner curtain in a window pair, or null. Linking
     * is geometric like {@link CurtainBlock}'s: the two curtains' anchors
     * (their lower outer columns) sit side by side at the window center, so
     * the partner's anchor is the next cell opposite this curtain's
     * bunching direction. Two same-side curtains never link.
     */
    private static @Nullable BlockPos partnerAnchor(Level level, BlockPos anchor, BlockState state) {
        BlockPos partnerAnchor = LargeCurtainGeometry.partnerAnchor(anchor, innerStep(state));
        BlockState partner = level.getBlockState(partnerAnchor);
        if (!isLinkedPartner(partner, state)
                || partner.getValue(COLUMN) != Column.OUTER
                || partner.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return partnerAnchor;
    }

    private static boolean isLinkedPartner(BlockState neighbour, BlockState state) {
        return neighbour.getBlock() instanceof LargeCurtainBlock
                && neighbour.getValue(FACING) == state.getValue(FACING)
                && neighbour.getValue(SIDE) != state.getValue(SIDE);
    }

    @Override
    protected boolean hasRedstoneSignal(Level level, BlockPos pos, BlockState state) {
        return hasAnyRedstoneSignal(level, anchorOf(pos, state), innerStep(state));
    }

    @Override
    protected void setPairPowered(Level level, BlockPos pos, BlockState state, boolean powered) {
        setCurtainPowered(level, anchorOf(pos, state), innerStep(state), powered);
    }

    /**
     * True when this curtain or its linked partner is powered; a powered
     * pair ignores manual use, mirroring {@link CurtainBlock}.
     */
    @Override
    protected boolean isPoweredPair(Level level, BlockPos pos, BlockState state) {
        BlockPos anchor = anchorOf(pos, state);
        if (state.getValue(POWERED) || hasAnyRedstoneSignal(level, anchor, innerStep(state))) {
            return true;
        }
        BlockPos partner = partnerAnchor(level, anchor, state);
        if (partner == null) {
            return false;
        }
        BlockState partnerState = level.getBlockState(partner);
        return partnerState.getValue(POWERED)
                || hasAnyRedstoneSignal(level, partner, innerStep(partnerState));
    }

    /**
     * Toggles this curtain plus its linked partner curtain, mirroring
     * {@link CurtainBlock#togglePair}: a redstone signal on either curtain
     * forces both open. {@code pos} is any block of this curtain.
     */
    @Override
    protected void togglePair(Level level, BlockPos pos, BlockState state, boolean open) {
        BlockPos anchor = anchorOf(pos, state);
        BlockPos partner = partnerAnchor(level, anchor, state);
        boolean powered = hasAnyRedstoneSignal(level, anchor, innerStep(state));
        boolean partnerPowered = partner != null
                && hasAnyRedstoneSignal(level, partner, innerStep(level.getBlockState(partner)));
        boolean targetOpen = LargeCurtainGeometry.targetOpen(open, powered, partnerPowered);

        toggleCurtain(level, anchor, state, targetOpen);
        if (partner != null) {
            toggleCurtain(level, partner, level.getBlockState(partner), targetOpen);
        }
    }

    /** Sets a flag on every block of the curtain anchored at {@code anchor}. */
    private static void setCurtainPowered(Level level, BlockPos anchor, Direction inner, boolean powered) {
        for (BlockPos part : new BlockPos[]{
                anchor, anchor.relative(inner), anchor.above(), anchor.above().relative(inner)
        }) {
            BlockState partState = level.getBlockState(part);
            if (partState.getBlock() instanceof LargeCurtainBlock) {
                level.setBlock(part, partState.setValue(POWERED, powered), Block.UPDATE_ALL);
            }
        }
    }

    /** True if any of the four blocks of this curtain sees redstone. */
    private static boolean hasAnyRedstoneSignal(Level level, BlockPos anchor, Direction inner) {
        for (BlockPos pos : new BlockPos[]{
                anchor, anchor.relative(inner), anchor.above(), anchor.above().relative(inner)
        }) {
            if (level.hasNeighborSignal(pos)) {
                return true;
            }
        }
        return false;
    }


    /** Toggles all four blocks of the curtain anchored at {@code anchor}. */
    private static void toggleCurtain(Level level, BlockPos anchor, BlockState state, boolean open) {
        long gameTime = level.getGameTime();
        Direction inner = innerStep(state);
        // POWERED tracks the live redstone signal, never the open target: a
        // wrongly-stuck POWERED would lock the curtain against manual use.
        boolean powered = hasAnyRedstoneSignal(level, anchor, inner);
        for (BlockPos part : structurePositions(anchor, state)) {
            BlockState partState = level.getBlockState(part);
            if (!(partState.getBlock() instanceof LargeCurtainBlock)) {
                continue;
            }
            // Record the clock before setBlock so the block-entity data
            // packet carries OPEN and the animation timestamp together.
            if (level.getBlockEntity(part) instanceof LargeCurtainBlockEntity curtain) {
                curtain.recordTransition(gameTime, open);
                curtain.setChanged();
                level.sendBlockUpdated(part, partState, partState, Block.UPDATE_CLIENTS);
            }
            level.setBlock(part, partState.setValue(OPEN, open).setValue(POWERED, powered)
                            .setValue(ANIMATING, true), Block.UPDATE_ALL);
            level.scheduleTick(part, partState.getBlock(), ANIMATION_TICKS);
        }
    }
}
