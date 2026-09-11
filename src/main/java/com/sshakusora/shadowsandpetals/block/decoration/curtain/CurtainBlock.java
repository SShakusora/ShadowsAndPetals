package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

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
    /**
     * True only during the open/close animation window: the block-entity
     * renderer owns the pose then. Once false, the plain block-state model
     * (baked to the current OPEN pose) renders the curtain for free. The
     * server also uses this flag as the short interaction lock.
     */
    public static final BooleanProperty ANIMATING = BooleanProperty.create("animating");
    /**
     * Server level to hold ANIMATING: ceil of the 0.29167 s clip length.
     */
    public static final int ANIMATION_TICKS = 6;

    /**
     * Which side of a window the curtain panel hangs on.
     */
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
     * Gameplay collision slices for FACING=north. The model is a thin curtain
     * rather than a full block: closed fabric occupies roughly pixels 13..16
     * along the wall normal, while the lower half starts at pixel 3. Open
     * fabric gathers to its own side and reaches pixels 12..17 at the deepest
     * folds. The upper open pose also retains a rail spanning the full cell.
     */
    private static final VoxelShape NORTH_CLOSED_LOWER = toShape(CurtainGeometry.closed(false));
    private static final VoxelShape NORTH_CLOSED_UPPER = toShape(CurtainGeometry.closed(true));
    private static final VoxelShape NORTH_OPEN_LOWER_RIGHT = toShape(CurtainGeometry.open(false, false));
    private static final VoxelShape NORTH_OPEN_LOWER_LEFT = toShape(CurtainGeometry.open(false, true));
    private static final VoxelShape NORTH_OPEN_UPPER_RIGHT = Shapes.or(
            toShape(CurtainGeometry.open(true, false)),
            box(0, 14, 14, 16, 16, 16)
    ).optimize();
    private static final VoxelShape NORTH_OPEN_UPPER_LEFT = Shapes.or(
            toShape(CurtainGeometry.open(true, true)),
            box(0, 14, 14, 16, 16, 16)
    ).optimize();
    private static final Map<Direction, VoxelShape> CLOSED_LOWER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_CLOSED_LOWER);
    private static final Map<Direction, VoxelShape> CLOSED_UPPER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_CLOSED_UPPER);
    private static final Map<Direction, VoxelShape> OPEN_LOWER_RIGHT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_LOWER_RIGHT);
    private static final Map<Direction, VoxelShape> OPEN_LOWER_LEFT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_LOWER_LEFT);
    private static final Map<Direction, VoxelShape> OPEN_UPPER_RIGHT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_UPPER_RIGHT);
    private static final Map<Direction, VoxelShape> OPEN_UPPER_LEFT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_UPPER_LEFT);

    private static VoxelShape toShape(CurtainGeometry.CollisionBox box) {
        return Block.box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    public CurtainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(SIDE, Side.LEFT)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(ANIMATING, false));
    }

    @Override
    protected MapCodec<? extends CurtainBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SIDE, OPEN, POWERED, ANIMATING);
    }

    /**
     * Whether this state belongs to either curtain family while its
     * open/close transition is being rendered by the block entity renderer.
     */
    public static boolean isAnimating(BlockState state) {
        return state.getBlock() instanceof CurtainBlock && state.getValue(ANIMATING);
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
        Side side = CurtainStructure.sideForPlacement(
                level,
                lowerPos,
                facing,
                context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()
        );
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, halfAtClick)
                .setValue(SIDE, side)
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
        if (state.getValue(SIDE) == Side.LEFT) {
            return (upper ? OPEN_UPPER_LEFT_SHAPES : OPEN_LOWER_LEFT_SHAPES).get(facing);
        }
        return (upper ? OPEN_UPPER_RIGHT_SHAPES : OPEN_LOWER_RIGHT_SHAPES).get(facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Static chunk-mesh rendering outside the animation window; the
        // block-entity renderer takes over only while ANIMATING.
        return state.getValue(ANIMATING) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.CURTAIN.get().create(pos, state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level,
                        BlockPos pos, RandomSource random) {
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
            @Nullable LivingEntity placer,
            ItemStack stack
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
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        DoubleBlockHalf half = state.getValue(HALF);
        Direction expected = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (direction == expected
                && !(neighborState.getBlock() == state.getBlock()
                && neighborState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (handlesVanillaPairedBreak()
                && !level.isClientSide()
                && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            DoublePlantBlock.preventDropFromBottomPart(level, pos, state, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Whether the vanilla two-block companion cleanup should run before the
     * normal player-destruction pipeline. Wider curtains perform their own
     * atomic teardown and must opt out so this helper cannot remove a part
     * before that transaction starts.
     */
    protected boolean handlesVanillaPairedBreak() {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        if (isAnimating(state)) {
            return InteractionResult.CONSUME;
        }
        if (isPoweredPair(level, pos, state)) {
            return InteractionResult.PASS;
        }
        boolean open = !state.getValue(OPEN);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        togglePair(level, pos, state, open);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (isAnimating(state)) {
            return ItemInteractionResult.CONSUME;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block block,
            BlockPos fromPos,
            boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }
        boolean powered = hasRedstoneSignal(level, pos, state);
        if (powered != state.getValue(POWERED)) {
            if (powered != state.getValue(OPEN)) {
                togglePair(level, pos, state, powered);
            } else {
                // Only the POWERED flag changes; keep the current pose.
                setPairPowered(level, pos, state, powered);
            }
        }
    }

    /**
     * Sets the POWERED flag on every member of this logical curtain.
     */
    protected void setPairPowered(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (CurtainStructure.resolve(level, pos).isPresent()) {
            CurtainPairController.setPowered(level, pos, powered);
        } else {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    /**
     * Toggles this curtain's halves plus its linked neighbour curtain,
     * recording the shared animation clock on each block entity.
     *
     * <p>Linking is geometric: in a window pair the LEFT curtain stands on
     * the observer's left, so its RIGHT partner is toward the observer's
     * right, and vice versa. Two same-side curtains never link.</p>
     *
     * <p>The shared controller resolves the complete logical structure from
     * {@code pos}, so this method works for both curtain widths.</p>
     */
    protected void togglePair(Level level, BlockPos pos, BlockState state, boolean open) {
        if (!(state.getBlock() instanceof CurtainBlock)) {
            return;
        }
        CurtainPairController.togglePair(level, pos, open);
    }

    protected boolean hasRedstoneSignal(Level level, BlockPos pos, BlockState state) {
        return CurtainStructure.resolve(level, pos)
                .map(structure -> structure.hasLiveRedstoneSignal(level))
                .orElseGet(() -> state.getBlock() instanceof CurtainBlock && level.hasNeighborSignal(pos));
    }

    protected boolean isPoweredPair(Level level, BlockPos pos, BlockState state) {
        return CurtainPairController.isPoweredPair(level, pos, state);
    }

    private static DoubleBlockHalf otherHalf(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? DoubleBlockHalf.UPPER
                : DoubleBlockHalf.LOWER;
    }
}
