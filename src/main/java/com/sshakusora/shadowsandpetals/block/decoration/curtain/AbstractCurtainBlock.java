package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import org.jetbrains.annotations.Nullable;

/** Common interaction, redstone and animation behavior for every curtain size. */
public abstract class AbstractCurtainBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<CurtainSide> SIDE = EnumProperty.create("side", CurtainSide.class);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ANIMATING = BooleanProperty.create("animating");
    public static final int ANIMATION_TICKS = 6;

    protected AbstractCurtainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(SIDE, CurtainSide.LEFT)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(ANIMATING, false));
    }

    @Override
    protected abstract MapCodec<? extends AbstractCurtainBlock> codec();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, OPEN, POWERED, ANIMATING);
    }

    public static boolean isAnimating(BlockState state) {
        return state.getBlock() instanceof AbstractCurtainBlock
                && state.hasProperty(ANIMATING)
                && state.getValue(ANIMATING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(ANIMATING)
                ? RenderShape.ENTITYBLOCK_ANIMATED
                : RenderShape.MODEL;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
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
        if (isAnimating(state)) {
            return InteractionResult.CONSUME;
        }
        if (isPoweredPair(level, pos, state)) {
            return InteractionResult.PASS;
        }
        togglePair(level, pos, !state.getValue(OPEN));
        return InteractionResult.SUCCESS;
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
        if (isAnimating(state)) {
            return ItemInteractionResult.CONSUME;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block block,
            BlockPos fromPos, boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }
        boolean powered = hasRedstoneSignal(level, pos, state);
        if (powered != state.getValue(POWERED)) {
            if (powered != state.getValue(OPEN)) {
                togglePair(level, pos, powered);
            } else {
                setPairPowered(level, pos, powered);
            }
        }
    }

    protected void togglePair(Level level, BlockPos pos, boolean open) {
        CurtainPairController.togglePair(level, pos, open);
    }

    protected boolean hasRedstoneSignal(Level level, BlockPos pos, BlockState state) {
        return CurtainStructure.resolve(level, pos)
                .map(structure -> structure.hasLiveRedstoneSignal(level))
                .orElseGet(() -> level.hasNeighborSignal(pos));
    }

    protected boolean isPoweredPair(Level level, BlockPos pos, BlockState state) {
        return CurtainPairController.isPoweredPair(level, pos, state);
    }

    protected void setPairPowered(Level level, BlockPos pos, boolean powered) {
        CurtainStructure.resolve(level, pos).ifPresentOrElse(
                structure -> structure.setPowered(level, powered),
                () -> level.setBlock(pos, level.getBlockState(pos).setValue(POWERED, powered), Block.UPDATE_ALL)
        );
    }

    @Override
    protected abstract VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context
    );

    @Override
    public abstract @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state);
}
