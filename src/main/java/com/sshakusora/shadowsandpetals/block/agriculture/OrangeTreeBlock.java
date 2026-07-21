package com.sshakusora.shadowsandpetals.block.agriculture;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;

public class OrangeTreeBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<OrangeTreeBlock> CODEC = simpleCodec(OrangeTreeBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int DOUBLE_HEIGHT_AGE = 3;
    public static final int MATURE_AGE = 4;
    public static final int MAX_AGE = 7;

    private static final VoxelShape[] UPPER_SHAPES = {
            Block.column(14.0D, 0.0D, 6.0D),
            Block.column(14.0D, 0.0D, 10.0D),
            Block.column(14.0D, 0.0D, 14.0D),
            Block.column(14.0D, 0.0D, 14.0D),
            Block.column(14.0D, 0.0D, 14.0D),
            Block.column(14.0D, 0.0D, 14.0D),
            Block.column(14.0D, 0.0D, 14.0D),
            Block.column(14.0D, 0.0D, 14.0D),
    };
    private static final VoxelShape[] LOWER_SHAPES = {
            UPPER_SHAPES[0],
            UPPER_SHAPES[1],
            UPPER_SHAPES[2],
            Block.column(14.0D, 0.0D, 16.0D),
            Block.column(14.0D, 0.0D, 16.0D),
            Block.column(14.0D, 0.0D, 16.0D),
            Block.column(14.0D, 0.0D, 16.0D),
            Block.column(14.0D, 0.0D, 16.0D),
    };

    public OrangeTreeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(AGE, 0)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<? extends OrangeTreeBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return (state.getValue(HALF) != DoubleBlockHalf.LOWER || !level.getBlockState(pos.below()).is(Blocks.FARMLAND)) && super.canSurvive(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int age = state.getValue(AGE);
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPES[age] : UPPER_SHAPES[age];
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        if (state.getValue(AGE) >= DOUBLE_HEIGHT_AGE) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || level.getRawBrightness(pos, 0) < 9) {
            return;
        }

        boolean shouldGrow = random.nextInt(7) == 0;
        if (CommonHooks.canCropGrow(level, pos, state, shouldGrow)) {
            grow(level, pos, state);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    private void grow(ServerLevel level, BlockPos lowerPos, BlockState lowerState) {
        int newAge = Math.min(lowerState.getValue(AGE) + 1, MAX_AGE);
        if (!canGrow(level, lowerPos, lowerState, newAge)) {
            return;
        }

        BlockState newLowerState = lowerState.setValue(AGE, newAge).setValue(HALF, DoubleBlockHalf.LOWER);
        level.setBlock(lowerPos, newLowerState, Block.UPDATE_CLIENTS);
        if (newAge >= DOUBLE_HEIGHT_AGE) {
            level.setBlock(
                    lowerPos.above(),
                    newLowerState.setValue(HALF, DoubleBlockHalf.UPPER),
                    Block.UPDATE_ALL
            );
        }
    }

    private boolean canGrow(LevelReader level, BlockPos lowerPos, BlockState lowerState, int newAge) {
        if (lowerState.getValue(AGE) >= MAX_AGE || !CropBlock.hasSufficientLight(level, lowerPos)) {
            return false;
        }

        BlockPos upperPos = lowerPos.above();
        if (!level.isInsideBuildHeight(upperPos)) {
            return false;
        }
        if (newAge < DOUBLE_HEIGHT_AGE) {
            return true;
        }

        BlockState upperState = level.getBlockState(upperPos);
        return upperState.isAir() || upperState.is(this) && upperState.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER || state.getValue(AGE) != MAX_AGE || !player.getMainHandItem().isEmpty()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (level instanceof ServerLevel serverLevel) {
            BlockPos lowerPos = pos.below();
            BlockState lowerState = serverLevel.getBlockState(lowerPos);
            if (!lowerState.is(this) || lowerState.getValue(HALF) != DoubleBlockHalf.LOWER || lowerState.getValue(AGE) != MAX_AGE) {
                return InteractionResult.PASS;
            }

            BlockState harvestedLower = lowerState.setValue(AGE, MATURE_AGE);
            serverLevel.setBlock(lowerPos, harvestedLower, Block.UPDATE_CLIENTS);
            serverLevel.setBlock(
                    pos,
                    harvestedLower.setValue(HALF, DoubleBlockHalf.UPPER),
                    Block.UPDATE_ALL
            );
            Block.popResource(serverLevel, pos, new ItemStack(ItemRegistry.ORANGE.get(), 4));
            serverLevel.playSound(
                    null,
                    pos,
                    SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.8F + serverLevel.getRandom().nextFloat() * 0.4F
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            boolean includeData,
            @Nullable Player player
    ) {
        return new ItemStack(ItemRegistry.ORANGE_SEED.get());
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        PosAndState lowerHalf = getLowerHalf(level, pos, state);
        return lowerHalf != null && canGrow(level, lowerHalf.pos(), lowerHalf.state(), lowerHalf.state().getValue(AGE) + 1);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        PosAndState lowerHalf = getLowerHalf(level, pos, state);
        if (lowerHalf != null) {
            grow(level, lowerHalf.pos(), lowerHalf.state());
        }
    }

    private @Nullable PosAndState getLowerHalf(LevelReader level, BlockPos pos, BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new PosAndState(pos, state);
        }

        BlockPos lowerPos = pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        return lowerState.is(this) && lowerState.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new PosAndState(lowerPos, lowerState)
                : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FACING);
        super.createBlockStateDefinition(builder);
    }

    private record PosAndState(BlockPos pos, BlockState state) {
    }
}
