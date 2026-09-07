package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.LargeCurtainBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

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
    /** Marks the block that drives the rig (placed block, lower outer). */
    public static final BooleanProperty ANCHOR = BooleanProperty.create("anchor");

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
     * Collision slice for FACING=north: a thin strip along the wall face,
     * inside the block's own cell (the model overhangs like a fence).
     */
    private static final VoxelShape NORTH_SHAPE = box(0, 0, 14, 16, 16, 15);
    private static final Map<Direction, VoxelShape> SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_SHAPE);
    /**
     * Open-pose slices for FACING=north, mirroring the open quadrant
     * models: the fabric piles into the inner column (right curtain at
     * local x 1..9, left curtain at x 7..15), the outer column keeps only
     * the rail band in its upper block, and the lower outer block is empty.
     */
    private static final VoxelShape NORTH_OPEN_RAIL = box(0, 14, 14, 16, 15, 15);
    private static final VoxelShape NORTH_OPEN_PILE_RIGHT = box(1, 0, 14, 9, 16, 15);
    private static final VoxelShape NORTH_OPEN_PILE_LEFT = box(7, 0, 14, 15, 16, 15);
    private static final Map<Direction, VoxelShape> OPEN_RAIL_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_RAIL);
    private static final Map<Direction, VoxelShape> OPEN_PILE_RIGHT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_PILE_RIGHT);
    private static final Map<Direction, VoxelShape> OPEN_PILE_LEFT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_PILE_LEFT);

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
    private static Direction innerStep(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(SIDE) == Side.RIGHT
                ? facing.getClockWise().getOpposite()
                : facing.getClockWise();
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
        BlockPos inner = clickedPos.relative(innerStep(anchor));
        if (!level.getBlockState(inner).canBeReplaced(context)
                || !level.getBlockState(clickedPos.above()).canBeReplaced(context)
                || !level.getBlockState(inner.above()).canBeReplaced(context)) {
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
                    return direction == leftDir ? Side.RIGHT : Side.LEFT;
                }
            }
        }
        return Side.LEFT;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (!state.getValue(OPEN)) {
            return SHAPES.get(facing);
        }
        if (state.getValue(COLUMN) == Column.OUTER) {
            return state.getValue(HALF) == DoubleBlockHalf.UPPER
                    ? OPEN_RAIL_SHAPES.get(facing)
                    : Shapes.empty();
        }
        return (state.getValue(SIDE) == Side.LEFT
                ? OPEN_PILE_LEFT_SHAPES
                : OPEN_PILE_RIGHT_SHAPES).get(facing);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.LARGE_CURTAIN.get().create(pos, state);
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.@Nullable LivingEntity placer,
            net.minecraft.world.item.ItemStack stack
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
            BlockState state, net.minecraft.world.level.LevelReader level,
            net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState,
            net.minecraft.util.RandomSource random
    ) {
        for (Direction expected : structuralDirections(state)) {
            if (direction != expected) {
                continue;
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
        return neighbour.getBlock() instanceof LargeCurtainBlock
                && neighbour.getValue(FACING) == state.getValue(FACING)
                && (neighbour.getValue(HALF) != state.getValue(HALF)
                || neighbour.getValue(COLUMN) != state.getValue(COLUMN));
    }

    /** The lower outer corner (anchor) of the curtain that owns this block. */
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
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            // The anchor's loot drop covers the whole curtain.
            removeStructure(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Removes the remaining blocks of this curtain; the anchor drops the item. */
    private static void removeStructure(Level level, BlockPos pos, BlockState state) {
        BlockPos anchor = anchorOf(pos, state);
        Direction inner = innerStep(state);
        for (BlockPos part : new BlockPos[]{
                anchor, anchor.relative(inner), anchor.above(), anchor.above().relative(inner)
        }) {
            if (part.equals(anchor)) {
                continue;
            }
            BlockState partState = level.getBlockState(part);
            if (isSameCurtain(partState, state) || partState.getBlock() instanceof LargeCurtainBlock) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * The anchor of the partner curtain in a window pair, or null. Linking
     * is geometric like {@link CurtainBlock}'s: the two curtains' anchors
     * (their lower outer columns) sit side by side at the window center, so
     * the partner's anchor is the next cell opposite this curtain's
     * bunching direction. Two same-side curtains never link.
     */
    private static @Nullable BlockPos partnerAnchor(Level level, BlockPos anchor, BlockState state) {
        BlockPos partnerAnchor = anchor.relative(innerStep(state).getOpposite());
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
        setCurtainFlag(level, anchorOf(pos, state), innerStep(state), POWERED, powered);
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
        boolean targetOpen = open || powered || partnerPowered;

        toggleCurtain(level, anchor, state, targetOpen);
        if (partner != null) {
            toggleCurtain(level, partner, level.getBlockState(partner), targetOpen);
        }
    }

    /** Sets a flag on every block of the curtain anchored at {@code anchor}. */
    private static void setCurtainFlag(
            Level level, BlockPos anchor, Direction inner, BooleanProperty flag, boolean value
    ) {
        for (BlockPos part : new BlockPos[]{
                anchor, anchor.relative(inner), anchor.above(), anchor.above().relative(inner)
        }) {
            BlockState partState = level.getBlockState(part);
            if (partState.getBlock() instanceof LargeCurtainBlock) {
                level.setBlock(part, partState.setValue(flag, value), Block.UPDATE_ALL);
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
        for (BlockPos part : new BlockPos[]{
                anchor, anchor.relative(inner), anchor.above(), anchor.above().relative(inner)
        }) {
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