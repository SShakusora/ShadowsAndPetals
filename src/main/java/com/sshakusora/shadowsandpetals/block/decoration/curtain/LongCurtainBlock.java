package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** The original two-block curtain, now exposed as the long curtain. */
public class LongCurtainBlock extends AbstractCurtainBlock {
    public static final MapCodec<LongCurtainBlock> CODEC = simpleCodec(LongCurtainBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape NORTH_CLOSED_LOWER = toShape(LongCurtainGeometry.closed(false));
    private static final VoxelShape NORTH_CLOSED_UPPER = toShape(LongCurtainGeometry.closed(true));
    private static final VoxelShape NORTH_OPEN_LOWER_RIGHT = toShape(LongCurtainGeometry.open(false, false));
    private static final VoxelShape NORTH_OPEN_LOWER_LEFT = toShape(LongCurtainGeometry.open(false, true));
    private static final VoxelShape NORTH_OPEN_UPPER_RIGHT = Shapes.or(
            toShape(LongCurtainGeometry.open(true, false)),
            box(0, 14, 14, 16, 16, 16)
    ).optimize();
    private static final VoxelShape NORTH_OPEN_UPPER_LEFT = Shapes.or(
            toShape(LongCurtainGeometry.open(true, true)),
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

    private static VoxelShape toShape(LongCurtainGeometry.CollisionBox box) {
        return Block.box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    public LongCurtainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends LongCurtainBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

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
        CurtainSide side = CurtainStructure.sideForPlacement(
                level,
                upperPos,
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
        if (state.getValue(SIDE) == CurtainSide.LEFT) {
            return (upper ? OPEN_UPPER_LEFT_SHAPES : OPEN_LOWER_LEFT_SHAPES).get(facing);
        }
        return (upper ? OPEN_UPPER_RIGHT_SHAPES : OPEN_LOWER_RIGHT_SHAPES).get(facing);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.LONG_CURTAIN.get().create(pos, state);
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack
    ) {
        if (placer == null) {
            return;
        }
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockPos otherPos = pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? Direction.UP : Direction.DOWN);
        level.setBlock(otherPos, state.setValue(HALF, otherHalf(state)), Block.UPDATE_ALL);
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
        if (!level.isClientSide()
                && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            DoublePlantBlock.preventDropFromBottomPart(level, pos, state, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static DoubleBlockHalf otherHalf(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? DoubleBlockHalf.UPPER
                : DoubleBlockHalf.LOWER;
    }
}
